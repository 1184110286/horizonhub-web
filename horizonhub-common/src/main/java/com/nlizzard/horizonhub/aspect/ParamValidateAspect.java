package com.nlizzard.horizonhub.aspect;

import com.nlizzard.horizonhub.annotation.GlobalInterceptor;
import com.nlizzard.horizonhub.annotation.VerifyParam;
import com.nlizzard.horizonhub.entity.enums.ResponseCodeEnum;
import com.nlizzard.horizonhub.exception.BusinessException;
import com.nlizzard.horizonhub.utils.VerifyUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 公共参数校验切面（web 与 admin 共用）。
 * <p>
 * 拦截被 {@link GlobalInterceptor} 标注、且 {@code checkParams=true} 的方法，
 * 在进入业务前按 {@link VerifyParam} 校验基础类型参数与对象字段。
 * <p>
 * 仅负责参数校验；web 端的操作频率限制由 {@code horizonhub-web} 的
 * {@code FrequencyLimitAspect} 独立承担，并通过 {@code @Order} 控制为本切面的内层
 * （参数校验通过后再消耗频率配额）。
 */
@Component
@Aspect
@Order(10)
public class ParamValidateAspect {

    private static final Logger logger = LoggerFactory.getLogger(ParamValidateAspect.class);

    /**
     * 支持直接校验的基础类型
     */
    private static final String[] TYPE_BASE = {
            "java.lang.String", "java.lang.Integer", "java.lang.Long"
    };

    @Around("@annotation(com.nlizzard.horizonhub.annotation.GlobalInterceptor)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        Object[] arguments = point.getArgs();
        MethodSignature methodSignature = (MethodSignature) point.getSignature();
        Class<?>[] parameterTypes = methodSignature.getMethod().getParameterTypes();
        String methodName = point.getSignature().getName();
        Object target = point.getTarget();
        Method method = target.getClass().getMethod(methodName, parameterTypes);
        GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);

        if (interceptor.checkParams()) {
            validateParams(method, arguments);
        }
        return point.proceed();
    }

    /**
     * 参数校验：遍历参数，如果参数上有 @VerifyParam 注解，则进行校验（1.基础类型；2.对象类型）
     */
    private void validateParams(Method m, Object[] arguments) throws BusinessException {
        Parameter[] parameters = m.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object value = arguments[i];
            VerifyParam verifyParam = parameter.getAnnotation(VerifyParam.class);
            if (verifyParam == null) {
                continue;
            }
            // 基础数据类型
            if (ArrayUtils.contains(TYPE_BASE, parameter.getParameterizedType().getTypeName())) {
                checkBasicTypeValue(value, verifyParam);
            } else {
                // 对象类型
                checkObjValue(parameter, value);
            }
        }
    }

    /**
     * 校验对象参数：通过反射获取对象的字段，遍历字段，如果字段上有 @VerifyParam 注解，则进行校验
     */
    private void checkObjValue(Parameter parameter, Object value) {
        try {
            String typeName = parameter.getParameterizedType().getTypeName();
            Class<?> clazz = Class.forName(typeName);
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                VerifyParam fieldVerifyParam = field.getAnnotation(VerifyParam.class);
                if (fieldVerifyParam == null) {
                    continue;
                }
                field.setAccessible(true);
                Object resultValue = field.get(value);
                checkBasicTypeValue(resultValue, fieldVerifyParam);
            }
        } catch (BusinessException e) {
            logger.error("校验参数失败", e);
            throw e;
        } catch (Exception e) {
            logger.error("校验参数失败", e);
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
    }

    /**
     * 校验基础类型参数
     */
    private void checkBasicTypeValue(Object value, VerifyParam verifyParam) throws BusinessException {
        boolean isEmpty = value == null || StringUtils.isBlank(value.toString());
        int length = value == null ? 0 : value.toString().length();

        // 校验必填
        if (isEmpty && verifyParam.required()) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "缺少必填字段");
        }

        // 校验长度
        if (!isEmpty && (verifyParam.max() != -1 && verifyParam.max() < length || verifyParam.min() != -1 && verifyParam.min() > length)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), String.format("参数长度必须在 %s - %s 之间", verifyParam.min(), verifyParam.max()));
        }
        // 校验正则
        if (!isEmpty && !StringUtils.isBlank(verifyParam.regex().getRegex()) && !VerifyUtils.verify(verifyParam.regex(), String.valueOf(value))) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), verifyParam.regex().getDesc());
        }
    }
}

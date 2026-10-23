package com.nlizzard.horizonhub.utils;

import cn.hutool.json.JSONUtil;
import com.nlizzard.horizonhub.entity.enums.ResponseCodeEnum;
import com.nlizzard.horizonhub.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 对象序列化和反序列化工具类
 * 主要用于将对象转换为JSON字符串，或者将JSON字符串转换为对象
 */
public class JsonUtils {
    private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);

    /**
     * 将对象转换为JSON字符串
     *
     * @param obj
     * @return
     */
    public static String object2Json(Object obj) {
        try {
            return JSONUtil.toJsonStr(obj);
        } catch (Exception e) {
            logger.error("Bean -> Json 对象序列化失败", e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        }
    }

    /**
     * 将JSON字符串转换为对象
     *
     * @param jsonStr
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T json2Object(String jsonStr, Class<T> clazz) {
        try {
            return JSONUtil.toBean(jsonStr, clazz);
        } catch (Exception e) {
            logger.error("Json -> Bean 对象反序列化失败", e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        }
    }

    /**
     * 将JSON字符串转换为List对象
     *
     * @param jsonStr
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> List<T> jsonArray2List(String jsonStr, Class<T> clazz) {
        try {
            return JSONUtil.toList(jsonStr, clazz);
        } catch (Exception e) {
            logger.error("Json -> List 对象反序列化失败", e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        }
    }
}

package com.nlizzard.horizonhub.basecontroller;

import com.nlizzard.horizonhub.entity.enums.ResponseCodeEnum;
import com.nlizzard.horizonhub.entity.vo.ResponseVO;
import com.nlizzard.horizonhub.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.stream.Collectors;


//全局异常处理
@RestControllerAdvice
public class GlobalExceptionHandlerController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandlerController.class);

    /**
     * JSR-303：{@code @RequestBody} + {@code @Valid} 校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseVO<Void> handleArgNotValid(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError != null ? fieldError.getDefaultMessage() : ResponseCodeEnum.CODE_600.getMsg();
        logger.warn("参数校验失败：{}", msg);
        return errorVO(ResponseCodeEnum.CODE_600, msg);
    }

    /**
     * JSR-303：{@code @RequestParam}/{@code @PathVariable} 约束校验失败（控制器需标注 {@code @Validated}）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    ResponseVO<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(";"));
        if (msg.isEmpty()) {
            msg = ResponseCodeEnum.CODE_600.getMsg();
        }
        logger.warn("参数约束校验失败：{}", msg);
        return errorVO(ResponseCodeEnum.CODE_600, msg);
    }

    /**
     * 表单参数绑定失败
     */
    @ExceptionHandler(BindException.class)
    ResponseVO<Void> handleBind(BindException e) {
        return errorVO(ResponseCodeEnum.CODE_600, ResponseCodeEnum.CODE_600.getMsg());
    }

    /**
     * 上传文件超过大小限制
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseVO<Void> handleUploadSize(MaxUploadSizeExceededException e) {
        logger.warn("上传文件超过大小限制：{}", e.getMessage());
        return errorVO(ResponseCodeEnum.CODE_600, "上传文件超过大小限制");
    }

    /**
     * 参数类型不匹配（如 Integer 形参传成了字符串）
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseVO<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return errorVO(ResponseCodeEnum.CODE_600, ResponseCodeEnum.CODE_600.getMsg());
    }

    /**
     * 缺少必填请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseVO<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return errorVO(ResponseCodeEnum.CODE_600, ResponseCodeEnum.CODE_600.getMsg());
    }

    /**
     * 请求体不可读（JSON 损坏、Content-Type 不匹配等）
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseVO<Void> handleNotReadable(HttpMessageNotReadableException e) {
        return errorVO(ResponseCodeEnum.CODE_600, ResponseCodeEnum.CODE_600.getMsg());
    }

    /**
     * 请求方法不支持（例如对仅 POST 的接口发起 GET）
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseVO<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        logger.warn("请求方法不支持：{}", e.getMessage());
        return errorVO(ResponseCodeEnum.CODE_600, "请求方法不支持");
    }

    @ExceptionHandler(value = Exception.class)
    Object handleException(Exception e, HttpServletRequest request) {
        logger.error("请求错误,请求地址{},错误信息：", request.getRequestURI(), e);
        ResponseVO<Void> ajaxResponse = new ResponseVO<>();
        //404
        if (e instanceof NoHandlerFoundException) {
            ajaxResponse.setCode(ResponseCodeEnum.CODE_404.getCode());
            ajaxResponse.setInfo(ResponseCodeEnum.CODE_404.getMsg());
            ajaxResponse.setStatus(STATUS_ERROR);
        } else if (e instanceof BusinessException biz) {
            //业务错误
            ajaxResponse.setCode(biz.getCode() == null ? ResponseCodeEnum.CODE_600.getCode() : biz.getCode());
            ajaxResponse.setInfo(biz.getMessage());
            ajaxResponse.setStatus(STATUS_ERROR);
        } else if (e instanceof DuplicateKeyException) {
            //主键冲突
            ajaxResponse.setCode(ResponseCodeEnum.CODE_601.getCode());
            ajaxResponse.setInfo(ResponseCodeEnum.CODE_601.getMsg());
            ajaxResponse.setStatus(STATUS_ERROR);
        } else {
            //500兜底，其他错误
            ajaxResponse.setCode(ResponseCodeEnum.CODE_500.getCode());
            ajaxResponse.setInfo(ResponseCodeEnum.CODE_500.getMsg());
            ajaxResponse.setStatus(STATUS_ERROR);
        }
        return ajaxResponse;
    }

    private ResponseVO<Void> errorVO(ResponseCodeEnum code, String info) {
        ResponseVO<Void> vo = new ResponseVO<>();
        vo.setCode(code.getCode());
        vo.setInfo(info);
        vo.setStatus(STATUS_ERROR);
        return vo;
    }
}

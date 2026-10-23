package com.nlizzard.horizonhub.exception;

import com.nlizzard.horizonhub.entity.enums.ResponseCodeEnum;

//自定义业务异常类
public class BusinessException extends RuntimeException {

    // 异常的响应代码枚举
    private ResponseCodeEnum codeEnum;

    // 异常的自定义代码
    private Integer code;

    // 异常的错误消息
    private String message;

    // 构造函数，接受错误消息和引起异常的原因
    public BusinessException(String message, Throwable e) {
        super(message, e);
        this.message = message;
    }

    // 构造函数，接受错误消息
    public BusinessException(String message) {
        super(message);
        this.message = message;
    }

    // 构造函数，接受引起异常的原因
    public BusinessException(Throwable e) {
        super(e);
    }

    // 构造函数，接受响应代码枚举
    public BusinessException(ResponseCodeEnum codeEnum) {
        super(codeEnum.getMsg());
        this.codeEnum = codeEnum;
        this.code = codeEnum.getCode();
        this.message = codeEnum.getMsg();
    }

    // 构造函数，接受自定义代码和错误消息
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    // 获取异常的响应代码枚举
    public ResponseCodeEnum getCodeEnum() {
        return codeEnum;
    }

    // 获取异常的自定义代码
    public Integer getCode() {
        return code;
    }

    // 获取异常的错误消息
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * 重写fillInStackTrace 业务异常不需要堆栈信息，提交效率
     */
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }
}

package com.nlizzard.horizonhub.entity.vo;


// 统一返回类
public class ResponseVO<T> {
    private String status; // 响应状态
    private Integer code; // 响应状态码
    private String info; // 响应信息
    private T data; // 响应数据

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}

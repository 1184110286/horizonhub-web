package com.nlizzard.horizonhub.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nlizzard.horizonhub.entity.enums.DateTimePatternEnum;
import com.nlizzard.horizonhub.utils.DateUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description:邮箱验证码
 * @author:nlizzard
 * @date:2026/03/08
 */
public class EmailCode implements Serializable {
    /**
     * 邮箱
     */
    private String email;

    /**
     * 编号
     */
    private String code;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy_MM_dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy_MM_dd HH:mm:ss")
    private Date createTime;

    /**
     * 0:未使用  1:已使用
     */
    @JsonIgnore
    private Integer status;

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return this.email;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getCreateTime() {
        return this.createTime;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getStatus() {
        return this.status;
    }

    @Override
    public String toString() {
        return "邮箱:" + (email == null ? "空" : email) + " ;编号:" + (code == null ? "空" : code) + " ;创建时间:" + (createTime == null ? "空" : DateUtils.format(createTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern())) + " ;0:未使用  1:已使用:" + (status == null ? "空" : status);
    }
}
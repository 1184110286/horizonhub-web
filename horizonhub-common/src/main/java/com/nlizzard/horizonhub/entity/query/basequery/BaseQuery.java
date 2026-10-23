package com.nlizzard.horizonhub.entity.query.basequery;

import com.nlizzard.horizonhub.entity.enums.ResponseCodeEnum;
import com.nlizzard.horizonhub.exception.BusinessException;

import java.util.regex.Pattern;

//基础查询类
public class BaseQuery {
    //分页信息
    private SimplePage simplePage;
    //页码
    private Integer pageNo;
    //每页大小
    private Integer pageSize;
    //排序变量
    private String orderBy;

    /**
     * 排序白名单：仅允许「列名 + 可选 asc/desc」，支持逗号分隔的多列。
     * 列名限定为以字母/下划线开头、由字母数字下划线组成（不含点、引号、分号、括号等），
     * 用于在 setter 处拦截任何试图通过 {@code order by ${query.orderBy}} 注入的载荷。
     */
    private static final Pattern ORDER_BY_PATTERN = Pattern.compile(
            "^\\s*[a-zA-Z_][a-zA-Z0-9_]*(\\s+(?i:asc|desc))?(\\s*,\\s*[a-zA-Z_][a-zA-Z0-9_]*(\\s+(?i:asc|desc))?)*\\s*$");

    public SimplePage getSimplePage() {
        return simplePage;
    }

    public void setSimplePage(SimplePage simplePage) {
        this.simplePage = simplePage;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPagoNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        // null/空视为不排序
        if (orderBy == null || orderBy.isBlank()) {
            this.orderBy = null;
            return;
        }
        // 仅放行符合白名单的排序片段，阻断 ${query.orderBy} 的 SQL 注入面
        if (!ORDER_BY_PATTERN.matcher(orderBy).matches()) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "非法的排序参数");
        }
        this.orderBy = orderBy;
    }
}

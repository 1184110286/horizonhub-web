package com.nlizzard.horizonhub.entity.vo;

import java.util.ArrayList;
import java.util.List;

// 分页显示VO
public class PaginationResultVO<T> {
    private Integer totalCount; // 总记录数
    private Integer pageSize; // 每页显示的记录数
    private Integer pageNo; // 当前页码
    private Integer pageTotal; // 总页数
    private List<T> list = new ArrayList<T>(); // 分页数据列表

    public PaginationResultVO(Integer totalCount, Integer pageSize, Integer pageNo, List<T> list) {
        this.totalCount = totalCount;
        this.pageSize = pageSize;
        this.pageNo = pageNo;
        this.list = list;
    }

    /**
     * 分页显示 VO 构造函数
     *
     * @param totalCount 数据总条数
     * @param pageSize   每页显示的条数
     * @param pageNo     当前页码
     * @param pageTotal  总页数
     * @param list       分页数据列表
     */
    public PaginationResultVO(Integer totalCount, Integer pageSize, Integer pageNo, Integer pageTotal, List<T> list) {
        if (pageNo == 0) {
            pageNo = 1; // 如果当前页码为0，则设置为1
        }
        this.totalCount = totalCount;
        this.pageSize = pageSize;
        this.pageNo = pageNo;
        this.pageTotal = pageTotal;
        this.list = list;
    }

    public PaginationResultVO(List<T> list) {
        this.list = list;
    }

    public PaginationResultVO() {

    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageTotal() {
        return pageTotal;
    }

    public void setPageTotal(Integer pageTotal) {
        this.pageTotal = pageTotal;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }
}

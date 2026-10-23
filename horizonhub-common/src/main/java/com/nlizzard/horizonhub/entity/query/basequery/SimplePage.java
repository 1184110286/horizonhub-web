package com.nlizzard.horizonhub.entity.query.basequery;

import com.nlizzard.horizonhub.entity.enums.PageSize;

//分页信息类
public class SimplePage {
    private Integer pageNo; // 当前页码
    private Integer countTotal; // 总记录数
    private Integer pageSize; // 每页显示的记录数
    private Integer pageTotal; // 总页数
    private Integer start; // 查询起始位置
    private Integer end; // 查询结束位置

    public SimplePage() {
    }

    public SimplePage(Integer pageNo, Integer countTotal, Integer pageSize) {
        if (null == pageNo) {
            pageNo = 0;
        }
        this.pageNo = pageNo;
        this.countTotal = countTotal;
        this.pageSize = pageSize;
        action(); // 根据当前设置的参数执行相应操作，计算其他属性值
    }

    public SimplePage(Integer start, Integer end) {
        this.start = start;
        this.end = end;
    }

    public void action() {
        if (this.pageSize <= 0) {
            this.pageSize = PageSize.SIZE20.getSize(); // 如果每页显示的记录数小于等于0，则设置为默认值20
        }
        if (this.countTotal > 0) {
            this.pageTotal = this.countTotal % this.pageSize == 0 ? this.countTotal / this.pageSize : this.countTotal / this.pageSize + 1; // 根据总记录数和每页显示的记录数计算总页数
        } else {
            pageTotal = 1; // 如果总记录数为0，则总页数设置为1
        }

        if (pageNo <= 1) {
            pageNo = 1; // 如果当前页码小于等于1，则设置为1
        }
        if (pageNo > pageTotal) {
            pageNo = pageTotal; // 如果当前页码大于总页数，则设置为总页数
        }
        this.start = (pageNo - 1) * pageSize; // 根据当前页码和每页显示的记录数计算查询起始位置
        this.end = this.pageSize; // 查询结束位置即为每页显示的记录数
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getCountTotal() {
        return countTotal;
    }

    public void setCountTotal(Integer countTotal) {
        this.countTotal = countTotal;
        this.action(); // 当总记录数发生变化时，重新执行操作，计算其他属性值
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getPageTotal() {
        return pageTotal;
    }

    public void setPageTotal(Integer pageTotal) {
        this.pageTotal = pageTotal;
    }

    public Integer getStart() {
        return start;
    }

    public void setStart(Integer start) {
        this.start = start;
    }

    public Integer getEnd() {
        return end;
    }

    public void setEnd(Integer end) {
        this.end = end;
    }
}

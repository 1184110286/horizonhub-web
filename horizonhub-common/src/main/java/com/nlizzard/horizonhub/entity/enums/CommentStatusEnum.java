package com.nlizzard.horizonhub.entity.enums;

public enum CommentStatusEnum {

    DEL(-1, "已删除"),
    NO_AUDIT(0, "待审核"),
    AUDIT(1, "已审核");

    private final Integer status;

    private final String description;

    CommentStatusEnum(Integer status, String description) {
        this.status = status;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public Integer getStatus() {
        return status;
    }
}

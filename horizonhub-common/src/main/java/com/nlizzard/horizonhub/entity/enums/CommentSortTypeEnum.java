package com.nlizzard.horizonhub.entity.enums;

public enum CommentSortTypeEnum {

    HOT_SORT_TYPE(0, "good_count desc , comment_id asc", "最热排序"),
    NEW_SORT_TYPE(1, "comment_id desc", "最新排序"),
    TOP_SORT_TYPE(null, "top_type desc ,", "置顶排序，总是拼在另外两种类型前面"),
    SECOND_LEVEL_COMMENT_SORT_TYPE(null, "comment_id asc", "二级评论排序，按照(评论ID自增的)升序，保证先发布的评论排在前面");

    private final Integer type;

    private final String sortSQLField;

    private final String description;

    public Integer getType() {
        return type;
    }

    public String getSortSQLField() {
        return sortSQLField;
    }

    public String getDescription() {
        return description;
    }

    CommentSortTypeEnum(Integer type, String sortSQLField, String description) {
        this.type = type;
        this.sortSQLField = sortSQLField;
        this.description = description;
    }
}

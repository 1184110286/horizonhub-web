package com.nlizzard.horizonhub.entity.enums;

public enum UserOperFrequencyTypeEnum {
    NO_CHECK(0, "不校验"),
    POST_ARTICLE(1, "发布文章"),
    POST_COMMENT(2, "评价"),
    DO_LIKE(3, "点赞"),
    IMAGE_UPLOAD(4, "图片上传");


    private final Integer operType;

    private final String desc;

    UserOperFrequencyTypeEnum(Integer operType, String desc) {
        this.operType = operType;
        this.desc = desc;
    }

    public Integer getOperType() {
        return operType;
    }

    public String getDesc() {
        return desc;
    }
}

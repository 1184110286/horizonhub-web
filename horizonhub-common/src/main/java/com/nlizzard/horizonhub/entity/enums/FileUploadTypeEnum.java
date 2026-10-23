package com.nlizzard.horizonhub.entity.enums;

import com.nlizzard.horizonhub.constants.Constants;

public enum FileUploadTypeEnum {
    ARTICLE_COVER("文章封面", Constants.IMAGE_ALL_SUFFIX),
    ARTICLE_ATTACHMENT("文章附件", Constants.ATTACHMENT_ALL_SUFFIX),
    COMMENT_IMAGE("评论图片", Constants.IMAGE_ALL_SUFFIX),
    AVATAR("个人头像", Constants.IMAGE_ALL_SUFFIX);


    private final String desc;
    private final String[] suffixArray;

    FileUploadTypeEnum(String desc, String[] suffixArray) {
        this.desc = desc;
        this.suffixArray = suffixArray;
    }

    public String getDesc() {
        return desc;
    }

    public String[] getSuffixArray() {
        return suffixArray;
    }
}

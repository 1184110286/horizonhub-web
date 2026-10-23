package com.nlizzard.horizonhub.entity.enums;

// 文章是否有附件枚举
public enum HasAttachmentEnum {
    NO(0, "无附件"),
    YES(1, "有附件");

    private final Integer code;
    private final String description;

    HasAttachmentEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static HasAttachmentEnum getByCode(Integer code) {
        for (HasAttachmentEnum item : HasAttachmentEnum.values()) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}

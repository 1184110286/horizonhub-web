package com.nlizzard.horizonhub.entity.enums;

public enum AttachmentFileTypeEnum {
    ZIP(0, new String[]{".zip", ".rar"}, "压缩包");


    private final Integer type;
    private final String[] suffixs;
    private final String desc;

    AttachmentFileTypeEnum(Integer type, String[] suffixs, String desc) {
        this.type = type;
        this.suffixs = suffixs;
        this.desc = desc;
    }

    public static AttachmentFileTypeEnum getByType(Integer type) {
        for (AttachmentFileTypeEnum item : AttachmentFileTypeEnum.values()) {
            if (item.getType().equals(type)) {
                return item;
            }
        }
        return null;
    }

    public Integer getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }

    public String[] getSuffixs() {
        return suffixs;
    }
}


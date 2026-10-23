package com.nlizzard.horizonhub.entity.enums;

// 帖子类型枚举
public enum ForumBoardPostTypeEnum {
    USER_BOARD(1, "普通用户可发帖的板块"),
    ADMIN_BOARD(0, "仅管理员可发帖的板块"),
    ALL_BOARD(null, "所有的板块");

    private final Integer code;
    private final String description;

    public static ForumBoardPostTypeEnum getByCode(Integer code) {
        for (ForumBoardPostTypeEnum type : ForumBoardPostTypeEnum.values()) {
            if (type.code != null && type.code.equals(code)) {
                return type;
            } else {
                if (type.code == null && code == null) {
                    return type;
                }
            }
        }
        return null;
    }

    ForumBoardPostTypeEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}

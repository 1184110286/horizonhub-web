package com.nlizzard.horizonhub.entity.enums;

public enum UserStatusEnum {
    ENABLE(1, "启用"),
    DISABLE(0, "禁用");


    private final Integer status;
    private final String desc;

    UserStatusEnum(Integer status, String desc) {
        this.desc = desc;
        this.status = status;
    }

    public static UserStatusEnum getUserStatusEnumByStatus(Integer status) {
        for (UserStatusEnum userStatusEnum : UserStatusEnum.values()) {
            if (userStatusEnum.getStatus().equals(status)) {
                return userStatusEnum;
            }
        }
        return null;
    }

    public Integer getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }
}

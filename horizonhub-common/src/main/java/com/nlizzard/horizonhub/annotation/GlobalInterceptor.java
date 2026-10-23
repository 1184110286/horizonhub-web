package com.nlizzard.horizonhub.annotation;

import com.nlizzard.horizonhub.entity.enums.UserOperFrequencyTypeEnum;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface GlobalInterceptor {

    /**
     * 是否检查登录状态
     *
     * @return
     */
    boolean checkLogin() default false;

    /**
     * 是否检查参数
     *
     * @return
     */
    boolean checkParams() default false;

    /**
     * 检查的频率类型枚举
     *
     * @return
     */
    UserOperFrequencyTypeEnum frequencyType() default UserOperFrequencyTypeEnum.NO_CHECK;

}

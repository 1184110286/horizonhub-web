package com.nlizzard.horizonhub.mappers;


import com.nlizzard.horizonhub.mappers.basemapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Description:邮箱验证码Mapper
 * @author:nlizzard
 * @date:2026/03/08
 */
public interface EmailCodeMapper<T, P> extends BaseMapper<T, P> {
    /**
     * 根据EmailAndCode查询
     */
    T selectByEmailAndCode(@Param("email") String email, @Param("code") String code);

    /**
     * 根据EmailAndCode更新
     */
    Integer updateByEmailAndCode(@Param("bean") T t, @Param("email") String email, @Param("code") String code);

    /**
     * 根据EmailAndCode删除
     */
    Integer deleteByEmailAndCode(@Param("email") String email, @Param("code") String code);

    /**
     * 禁用旧的验证码
     *
     */
    void disableEmailCode(@Param("email") String email);
}
package com.nlizzard.horizonhub.mappers.basemapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @param <T> 实体类
 * @param <P> 查询类
 * @Description基础Mapper接口(增删查改)
 */
public interface BaseMapper<T, P> {
    /**
     * insert:(插入)
     * @param t 实体类
     */
    Integer insert(@Param("bean") T t);

    /**
     * insertOrUpdate:(插入或更新)
     * @param t 实体类
     */
    Integer insertOrUpdate(@Param("bean") T t);

    /**
     * insertBatch:(批量插入)
     * @param list (实体类)列表
     */
    Integer insertBatch(@Param("list") List<T> list);

    /**
     * insertOrUpdateBatch:(批量插入或更新)
     * @param list (实体类)列表
     */
    Integer insertOrUpdateBatch(@Param("list") List<T> list);

    /**
     * selectList:(根据参数查询集合)
     * @param p 查询类
     */
    List<T> selectList(@Param("query") P p);

    /**
     * selectCount:(根据集合查询数量)
     * @param p 查询类
     */
    Integer selectCount(@Param("query") P p);
}

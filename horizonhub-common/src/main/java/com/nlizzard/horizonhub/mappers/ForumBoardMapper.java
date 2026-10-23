package com.nlizzard.horizonhub.mappers;


import com.nlizzard.horizonhub.mappers.basemapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Description:文章板块信息Mapper
 * @author:nlizzard
 * @date:2026/03/08
 */
public interface ForumBoardMapper<T, P> extends BaseMapper<T, P> {
	/**
	 * 根据BoardId查询
	 */
	T selectByBoardId(@Param("boardId") Integer boardId);

	/**
	 * 根据BoardId更新
	 */
	Integer updateByBoardId(@Param("bean") T t, @Param("boardId") Integer boardId);

	/**
	 * 根据BoardId删除
	 */
	Integer deleteByBoardId(@Param("boardId") Integer boardId);


}
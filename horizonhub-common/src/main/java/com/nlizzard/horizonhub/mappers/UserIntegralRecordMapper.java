package com.nlizzard.horizonhub.mappers;


import com.nlizzard.horizonhub.mappers.basemapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Description:用户积分记录表Mapper
 * @author:nlizzard
 * @date:2026/03/08
 */
public interface UserIntegralRecordMapper<T, P> extends BaseMapper<T, P> {
	/**
	 * 根据RecordId查询
	 */
	T selectByRecordId(@Param("recordId") Integer recordId);

	/**
	 * 根据RecordId更新
	 */
	Integer updateByRecordId(@Param("bean") T t, @Param("recordId") Integer recordId);

	/**
	 * 根据RecordId删除
	 */
	Integer deleteByRecordId(@Param("recordId") Integer recordId);


}
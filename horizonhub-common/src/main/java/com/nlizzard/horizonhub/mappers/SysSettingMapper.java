package com.nlizzard.horizonhub.mappers;


import com.nlizzard.horizonhub.mappers.basemapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Description:系统设置信息Mapper
 * @author:nlizzard
 * @date:2026/03/08
 */
public interface SysSettingMapper<T, P> extends BaseMapper<T, P> {
	/**
	 * 根据Code查询
	 */
	T selectByCode(@Param("code") String code);

	/**
	 * 根据Code更新
	 */
	Integer updateByCode(@Param("bean") T t, @Param("code") String code);

	/**
	 * 根据Code删除
	 */
	Integer deleteByCode(@Param("code") String code);


}
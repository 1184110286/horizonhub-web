package com.nlizzard.horizonhub.service;

import com.nlizzard.horizonhub.entity.dto.SysSettingDto;
import com.nlizzard.horizonhub.entity.pojo.SysSetting;
import com.nlizzard.horizonhub.entity.query.SysSettingQuery;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;

import java.util.List;

/**
 * @Description:系统设置信息Service
 * @author:nlizzard
 * @date:2026/03/08
 */
public interface SysSettingService {

    /**
     * 根据条件查询列表
     */
    List<SysSetting> findListByParam(SysSettingQuery query);

    /**
     * 根据条件查询数量
     */
    Integer findCountByParam(SysSettingQuery query);

    /**
     * 分页查询
     */
    PaginationResultVO<SysSetting> findListByPage(SysSettingQuery query);

    /**
     * 新增
     */
    Integer add(SysSetting bean);

    /**
     * 批量新增
     */
    Integer addBatch(List<SysSetting> listBean);

    /**
     * 批量新增或修改
     */
    Integer addOrUpdateBatch(List<SysSetting> listBean);

    /**
     * 根据Code查询
     */
    SysSetting getSysSettingByCode(String code);

    /**
     * 根据Code更新
     */
    Integer updateSysSettingByCode(SysSetting bean, String code);

    /**
     * 根据Code删除
     */
    Integer deleteSysSettingByCode(String code);

    /**
     * 将系统设置初始化到redis中  在系统启动时调用  将系统设置加载到redis中  以便后续使用  避免每次查询数据库获取系统设置
     */
    SysSettingDto initSysSettingToCache();

    /**
     * 更新系统设置  将新的系统设置保存到数据库中  同时更新redis中的系统设置  以便后续使用
     *
     * @param sysSettingDto 系统设置信息
     */
    void updateSetting(SysSettingDto sysSettingDto);
}
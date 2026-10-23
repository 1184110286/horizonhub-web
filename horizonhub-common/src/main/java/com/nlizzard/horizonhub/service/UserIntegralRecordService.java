package com.nlizzard.horizonhub.service;

import com.nlizzard.horizonhub.entity.pojo.UserIntegralRecord;
import com.nlizzard.horizonhub.entity.query.UserIntegralRecordQuery;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;

import java.util.List;

/**
 * @Description:用户积分记录表Service
 * @author:nlizzard
 * @date:2026/03/08
 */
public interface UserIntegralRecordService {

    /**
     * 根据条件查询列表
     */
    List<UserIntegralRecord> findListByParam(UserIntegralRecordQuery query);

    /**
     * 根据条件查询数量
     */
    Integer findCountByParam(UserIntegralRecordQuery query);

    /**
     * 分页查询
     */
    PaginationResultVO<UserIntegralRecord> findListByPage(UserIntegralRecordQuery query);

    /**
     * 新增
     */
    Integer add(UserIntegralRecord bean);

    /**
     * 批量新增
     */
    Integer addBatch(List<UserIntegralRecord> listBean);

    /**
     * 批量新增或修改
     */
    Integer addOrUpdateBatch(List<UserIntegralRecord> listBean);

    /**
     * 根据RecordId查询
     */
    UserIntegralRecord getUserIntegralRecordByRecordId(Integer recordId);

    /**
     * 根据RecordId更新
     */
    Integer updateUserIntegralRecordByRecordId(UserIntegralRecord bean, Integer recordId);

    /**
     * 根据RecordId删除
     */
    Integer deleteUserIntegralRecordByRecordId(Integer recordId);

}
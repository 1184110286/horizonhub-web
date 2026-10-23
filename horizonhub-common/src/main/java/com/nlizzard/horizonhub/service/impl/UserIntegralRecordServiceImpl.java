package com.nlizzard.horizonhub.service.impl;

import com.nlizzard.horizonhub.entity.enums.PageSize;
import com.nlizzard.horizonhub.entity.pojo.UserIntegralRecord;
import com.nlizzard.horizonhub.entity.query.UserIntegralRecordQuery;
import com.nlizzard.horizonhub.entity.query.basequery.SimplePage;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;
import com.nlizzard.horizonhub.mappers.UserIntegralRecordMapper;
import com.nlizzard.horizonhub.service.UserIntegralRecordService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Description:用户积分记录表ServiceImpl
 * @author:nlizzard
 * @date:2026/03/08
 */
@Service("userIntegralRecordService")
public class UserIntegralRecordServiceImpl implements UserIntegralRecordService {

    @Resource
    private UserIntegralRecordMapper<UserIntegralRecord, UserIntegralRecordQuery> userIntegralRecordMapper;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<UserIntegralRecord> findListByParam(UserIntegralRecordQuery query) {
        return this.userIntegralRecordMapper.selectList(query);
    }

    /**
     * 根据条件查询数量
     */
    @Override
    public Integer findCountByParam(UserIntegralRecordQuery query) {
        return this.userIntegralRecordMapper.selectCount(query);
    }

    /**
     * 分页查询
     */
    @Override
    public PaginationResultVO<UserIntegralRecord> findListByPage(UserIntegralRecordQuery query) {
        Integer count = this.findCountByParam(query);
        Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
        SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
        query.setSimplePage(page);
        List<UserIntegralRecord> list = this.findListByParam(query);
        return new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
    }

    /**
     * 新增
     */
    @Override
    public Integer add(UserIntegralRecord bean) {
        return this.userIntegralRecordMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<UserIntegralRecord> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userIntegralRecordMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或修改
     */
    @Override
    public Integer addOrUpdateBatch(List<UserIntegralRecord> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userIntegralRecordMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据RecordId查询
     */
    @Override
    public UserIntegralRecord getUserIntegralRecordByRecordId(Integer recordId) {
        return this.userIntegralRecordMapper.selectByRecordId(recordId);
    }

    /**
     * 根据RecordId更新
     */
    @Override
    public Integer updateUserIntegralRecordByRecordId(UserIntegralRecord bean, Integer recordId) {
        return this.userIntegralRecordMapper.updateByRecordId(bean, recordId);
    }

    /**
     * 根据RecordId删除
     */
    @Override
    public Integer deleteUserIntegralRecordByRecordId(Integer recordId) {
        return this.userIntegralRecordMapper.deleteByRecordId(recordId);
    }

}
package com.nlizzard.horizonhub.service;

import com.nlizzard.horizonhub.entity.enums.OperRecordOpTypeEnum;
import com.nlizzard.horizonhub.entity.pojo.LikeRecord;
import com.nlizzard.horizonhub.entity.query.LikeRecordQuery;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;

import java.util.List;

/**
 * @Description:点赞记录Service
 * @author:nlizzard
 * @date:2026/03/08
 */
public interface LikeRecordService {

    /**
     * 根据条件查询列表
     */
    List<LikeRecord> findListByParam(LikeRecordQuery query);

    /**
     * 根据条件查询数量
     */
    Integer findCountByParam(LikeRecordQuery query);

    /**
     * 分页查询
     */
    PaginationResultVO<LikeRecord> findListByPage(LikeRecordQuery query);

    /**
     * 新增
     */
    Integer add(LikeRecord bean);

    /**
     * 批量新增
     */
    Integer addBatch(List<LikeRecord> listBean);

    /**
     * 批量新增或修改
     */
    Integer addOrUpdateBatch(List<LikeRecord> listBean);

    /**
     * 根据OpId查询
     */
    LikeRecord getLikeRecordByOpId(Integer opId);

    /**
     * 根据OpId更新
     */
    Integer updateLikeRecordByOpId(LikeRecord bean, Integer opId);

    /**
     * 根据OpId删除
     */
    Integer deleteLikeRecordByOpId(Integer opId);

    /**
     * 根据ObjectIdAndUserIdAndOpType查询
     */
    LikeRecord getLikeRecordByObjectIdAndUserIdAndOpType(String objectId, String userId, Integer opType);

    /**
     * 根据ObjectIdAndUserIdAndOpType更新
     */
    Integer updateLikeRecordByObjectIdAndUserIdAndOpType(LikeRecord bean, String objectId, String userId, Integer opType);

    /**
     * 根据ObjectIdAndUserIdAndOpType删除
     */
    Integer deleteLikeRecordByObjectIdAndUserIdAndOpType(String objectId, String userId, Integer opType);

    /**
     * 根据ObjectIdAndUserIdAndOpType查询对象
     *
     * @param objectId 主体 ID （文章/评论 ID）
     * @param userId   当前用户 ID
     * @param opType   操作类型 0:点赞 1:取消点赞
     */
    LikeRecord getUserOperRecordByObjectIdAndUserIdAndOpType(String objectId, String userId, Integer opType);

    /**
     * 点赞/取消点赞
     *
     * @param objectId             被点赞/取消点赞的主体 ID （文章/评论）
     * @param userId               当前用户 ID
     * @param nickName             当前用户昵称
     * @param operRecordOpTypeEnum 记录表操作枚举(文章点赞/评论点赞)
     */
    void doLike(String objectId, String userId, String nickName, OperRecordOpTypeEnum operRecordOpTypeEnum);
}
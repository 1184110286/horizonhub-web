package com.nlizzard.horizonhub.service;

import com.nlizzard.horizonhub.entity.dto.UserMessageCountDto;
import com.nlizzard.horizonhub.entity.pojo.UserMessage;
import com.nlizzard.horizonhub.entity.query.UserMessageQuery;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;

import java.util.List;

/**
 * @Description:用户消息Service
 * @author:nlizzard
 * @date:2026/03/08
 */
public interface UserMessageService {

    /**
     * 根据条件查询列表
     */
    List<UserMessage> findListByParam(UserMessageQuery query);

    /**
     * 根据条件查询数量
     */
    Integer findCountByParam(UserMessageQuery query);

    /**
     * 分页查询
     */
    PaginationResultVO<UserMessage> findListByPage(UserMessageQuery query);

    /**
     * 新增
     */
    Integer add(UserMessage bean);

    /**
     * 批量新增
     */
    Integer addBatch(List<UserMessage> listBean);

    /**
     * 批量新增或修改
     */
    Integer addOrUpdateBatch(List<UserMessage> listBean);

    /**
     * 根据MessageId查询
     */
    UserMessage getUserMessageByMessageId(Integer messageId);

    /**
     * 根据MessageId更新
     */
    Integer updateUserMessageByMessageId(UserMessage bean, Integer messageId);

    /**
     * 根据MessageId删除
     */
    Integer deleteUserMessageByMessageId(Integer messageId);

    /**
     * 根据ArticleIdAndCommentIdAndSendUserIdAndMessageType查询
     */
    UserMessage getUserMessageByArticleIdAndCommentIdAndSendUserIdAndMessageType(String articleId, Integer commentId, String sendUserId, Integer messageType);

    /**
     * 根据ArticleIdAndCommentIdAndSendUserIdAndMessageType更新
     */
    Integer updateUserMessageByArticleIdAndCommentIdAndSendUserIdAndMessageType(UserMessage bean, String articleId, Integer commentId, String sendUserId, Integer messageType);

    /**
     * 根据ArticleIdAndCommentIdAndSendUserIdAndMessageType删除
     */
    Integer deleteUserMessageByArticleIdAndCommentIdAndSendUserIdAndMessageType(String articleId, Integer commentId, String sendUserId, Integer messageType);

    /**
     * 获取用户未读消息数量
     *
     * @param userId 用户ID
     * @return 用户消息未读数量
     */
    UserMessageCountDto getUserMessageCount(String userId);

    /**
     * 根据消息类型批量将用户消息设置为已读
     *
     * @param receivedUserId 接收消息的用户ID
     * @param messageType    消息类型
     */
    void readMessageByType(String receivedUserId, Integer messageType);
}
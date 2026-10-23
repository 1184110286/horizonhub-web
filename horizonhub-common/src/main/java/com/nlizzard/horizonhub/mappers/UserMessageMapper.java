package com.nlizzard.horizonhub.mappers;


import com.nlizzard.horizonhub.mappers.basemapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * @Description:用户消息Mapper
 * @author:nlizzard
 * @date:2026/03/08
 */
public interface UserMessageMapper<T, P> extends BaseMapper<T, P> {
    /**
     * 根据MessageId查询
     */
    T selectByMessageId(@Param("messageId") Integer messageId);

    /**
     * 根据MessageId更新
     */
    Integer updateByMessageId(@Param("bean") T t, @Param("messageId") Integer messageId);

    /**
     * 根据MessageId删除
     */
    Integer deleteByMessageId(@Param("messageId") Integer messageId);

    /**
     * 根据ArticleIdAndCommentIdAndSendUserIdAndMessageType查询
     */
    T selectByArticleIdAndCommentIdAndSendUserIdAndMessageType(@Param("articleId") String articleId, @Param("commentId") Integer commentId, @Param("sendUserId") String sendUserId, @Param("messageType") Integer messageType);

    /**
     * 根据ArticleIdAndSendUserIdAndMessageType查询
     */
    T selectByArticleIdAndSendUserIdAndMessageType(@Param("articleId") String articleId, @Param("sendUserId") String sendUserId, @Param("messageType") Integer messageType);

    /**
     * 根据ArticleIdAndCommentIdAndSendUserIdAndMessageType更新
     */
    Integer updateByArticleIdAndCommentIdAndSendUserIdAndMessageType(@Param("bean") T t, @Param("articleId") String articleId, @Param("commentId") Integer commentId, @Param("sendUserId") String sendUserId, @Param("messageType") Integer messageType);

    /**
     * 根据ArticleIdAndCommentIdAndSendUserIdAndMessageType删除
     */
    Integer deleteByArticleIdAndCommentIdAndSendUserIdAndMessageType(@Param("articleId") String articleId, @Param("commentId") Integer commentId, @Param("sendUserId") String sendUserId, @Param("messageType") Integer messageType);

    /**
     * 获取用户未读消息数量
     */
    List<Map<Object, Object>> selectUserMessageCount(@Param("userId") String userId);

    /**
     * 批量更新消息状态（可根据消息id列表，被接受人id，消息类型）
     */
    void updateMessageStatusBatch(@Param("messageIds") List<String> messageIds, @Param("receivedUserId") String receivedUserId,
                                  @Param("messageType") Integer messageType, @Param("status") Integer status);
}
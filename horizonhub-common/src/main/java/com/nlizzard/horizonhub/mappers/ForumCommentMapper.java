package com.nlizzard.horizonhub.mappers;


import com.nlizzard.horizonhub.mappers.basemapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * @Description:评论Mapper
 * @author:nlizzard
 * @date:2026/03/08
 */
public interface ForumCommentMapper<T, P> extends BaseMapper<T, P> {
    /**
     * 根据CommentId查询
     */
    T selectByCommentId(@Param("commentId") Integer commentId);

    /**
     * 根据CommentId更新
     */
    Integer updateByCommentId(@Param("bean") T t, @Param("commentId") Integer commentId);

    /**
     * 根据CommentId删除
     */
    Integer deleteByCommentId(@Param("commentId") Integer commentId);


    /**
     * 更新评论的点赞数量
     *
     * @param count     改变的数量
     * @param commentId 评论ID
     */
    void updateCommentCount(@Param("count") Integer count, @Param("commentId") Integer commentId);

    /**
     * 根据用户Id更新评论
     *
     * @param status 评论状态
     * @param userId 用户Id
     */
    void updateStatusBatchByUserId(@Param("status") Integer status, @Param("userId") String userId);
}
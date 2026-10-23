package com.nlizzard.horizonhub.mappers;


import com.nlizzard.horizonhub.mappers.basemapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Description:文章信息Mapper
 * @author:nlizzard
 * @date:2026/03/08
 */
public interface ForumArticleMapper<T, P> extends BaseMapper<T, P> {
    /**
     * 根据ArticleId查询
     */
    T selectByArticleId(@Param("articleId") String articleId);

    /**
     * 根据ArticleId更新
     */
    Integer updateByArticleId(@Param("bean") T t, @Param("articleId") String articleId);

    /**
     * 根据ArticleId删除
     */
    Integer deleteByArticleId(@Param("articleId") String articleId);

    /**
     * 更新文章相关计数（如浏览量、点赞数、评论数等）
     */
    void updateArticleCount(@Param("updateType") Integer updateType, @Param("changeCount") Integer changeCount, @Param("articleId") String articleId);

    /**
     * 批量更新文章的板块名称
     *
     * @param boardType 板块类型，0-一级板块，1-二级板块
     * @param boardName 板块名称
     * @param boardId   板块ID
     */
    void updateBoardNameBatch(@Param("boardType") Integer boardType, @Param("boardName") String boardName, @Param("boardId") Integer boardId);

    /**
     * 批量重置文章的评论数
     *
     * @param articleIds 文章ID列表
     */
    void resetCommentCount(@Param("articleIds") List<String> articleIds);

    /**
     * 根据UserId，批量更新文章状态
     */
    void updateStatusBatchByUserId(@Param("status") Integer status, @Param("userId") String userId);
}
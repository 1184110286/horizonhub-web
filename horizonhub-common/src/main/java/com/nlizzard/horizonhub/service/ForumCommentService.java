package com.nlizzard.horizonhub.service;

import com.nlizzard.horizonhub.entity.pojo.ForumComment;
import com.nlizzard.horizonhub.entity.query.ForumCommentQuery;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @Description:评论Service
 * @author:nlizzard
 * @date:2026/03/08
 */
public interface ForumCommentService {

    /**
     * 根据条件查询列表
     */
    List<ForumComment> findListByParam(ForumCommentQuery query);

    /**
     * 根据条件查询数量
     */
    Integer findCountByParam(ForumCommentQuery query);

    /**
     * 分页查询
     */
    PaginationResultVO<ForumComment> findListByPage(ForumCommentQuery query);

    /**
     * 新增
     */
    Integer add(ForumComment bean);

    /**
     * 批量新增
     */
    Integer addBatch(List<ForumComment> listBean);

    /**
     * 批量新增或修改
     */
    Integer addOrUpdateBatch(List<ForumComment> listBean);

    /**
     * 根据CommentId查询
     */
    ForumComment getForumCommentByCommentId(Integer commentId);

    /**
     * 根据CommentId更新
     */
    Integer updateForumCommentByCommentId(ForumComment bean, Integer commentId);

    /**
     * 根据CommentId删除
     */
    Integer deleteForumCommentByCommentId(Integer commentId);

    /**
     * 置顶/取消置顶评论
     *
     * @param userId    操作人用户 ID
     * @param commentId 评论 ID
     * @param topType   置顶类型（0-未置顶，1-置顶）
     */
    void changeTopType(String userId, Integer commentId, Integer topType);

    /**
     * 发表评论
     *
     * @param comment 评论信息
     * @param file    评论图片
     */
    void postComment(ForumComment comment, MultipartFile file);

    /**
     * 删除评论
     *
     * @param commentIds 评论 ID，逗号分隔
     */
    void delComment(String commentIds);

    /**
     * 删除单条评论
     *
     * @param commentId 评论 ID
     */
    void delCommentSingle(Integer commentId);

    /**
     * 审核评论
     *
     * @param commentIds 评论 ID，逗号分割
     */
    void auditComment(String commentIds);

    /**
     * 审核单条评论
     *
     * @param commentId 评论 ID
     */
    void auditCommentSingle(Integer commentId);
}
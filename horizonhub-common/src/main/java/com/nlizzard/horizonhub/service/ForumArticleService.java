package com.nlizzard.horizonhub.service;

import com.nlizzard.horizonhub.entity.pojo.ForumArticle;
import com.nlizzard.horizonhub.entity.pojo.ForumArticleAttachment;
import com.nlizzard.horizonhub.entity.query.ForumArticleQuery;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @Description:文章信息Service
 * @author:nlizzard
 * @date:2026/03/08
 */
public interface ForumArticleService {

    /**
     * 根据条件查询列表
     */
    List<ForumArticle> findListByParam(ForumArticleQuery query);

    /**
     * 根据条件查询数量
     */
    Integer findCountByParam(ForumArticleQuery query);

    /**
     * 分页查询
     */
    PaginationResultVO<ForumArticle> findListByPage(ForumArticleQuery query);

    /**
     * 新增
     */
    Integer add(ForumArticle bean);

    /**
     * 批量新增
     */
    Integer addBatch(List<ForumArticle> listBean);

    /**
     * 批量新增或修改
     */
    Integer addOrUpdateBatch(List<ForumArticle> listBean);

    /**
     * 根据ArticleId查询
     */
    ForumArticle getForumArticleByArticleId(String articleId);

    /**
     * 根据ArticleId更新
     */
    void updateForumArticleByArticleId(ForumArticle bean, String articleId);

    /**
     * 根据ArticleId删除
     */
    Integer deleteForumArticleByArticleId(String articleId);

    /**
     * 文章详情获取
     *
     * @param articleId 文章 ID
     */
    ForumArticle readArticle(String articleId);

    /**
     * 发帖
     *
     * @param isAdmin                是否管理员发帖
     * @param forumArticle           文章信息
     * @param forumArticleAttachment 文章附件信息
     * @param cover                  封面图片
     * @param attachment             附件
     */
    void postArticle(Boolean isAdmin, ForumArticle forumArticle, ForumArticleAttachment forumArticleAttachment, MultipartFile cover, MultipartFile attachment);

    /**
     * 更新文章
     *
     * @param isAdmin                是否管理员更新
     * @param article                文章信息
     * @param forumArticleAttachment 文章附件信息
     * @param cover                  封面图片
     * @param attachment             附件
     */
    void updateArticle(Boolean isAdmin, ForumArticle article, ForumArticleAttachment forumArticleAttachment, MultipartFile cover, MultipartFile attachment);

    /**
     * 更新板块
     *
     * @param articleId 文章 ID
     * @param pBoardId  父板块 ID
     * @param boardId   板块 ID
     */
    void updateBoard(String articleId, Integer pBoardId, Integer boardId);

    /**
     * 删除文章
     *
     * @param articleIds 文章 ID，逗号分隔
     */
    void delArticle(String articleIds);

    /**
     * 删除单篇文章
     *
     * @param articleId 文章 ID
     */
    void delArticleSingle(String articleId);

    /**
     * 审核文章
     *
     * @param articleIds 文章 ID，逗号分隔
     */
    void auditArticle(String articleIds);

    /**
     * 审核单篇文章
     *
     * @param articleId 文章 ID
     */
    void auditArticleSingle(String articleId);
}
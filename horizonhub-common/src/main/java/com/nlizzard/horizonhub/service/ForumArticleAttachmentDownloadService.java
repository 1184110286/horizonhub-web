package com.nlizzard.horizonhub.service;

import com.nlizzard.horizonhub.entity.pojo.ForumArticleAttachmentDownload;
import com.nlizzard.horizonhub.entity.query.ForumArticleAttachmentDownloadQuery;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;

import java.util.List;

/**
 * @Description:用户附件下载Service
 * @author:nlizzard
 * @date:2026/03/08
 */
public interface ForumArticleAttachmentDownloadService {

    /**
     * 根据条件查询列表
     */
    List<ForumArticleAttachmentDownload> findListByParam(ForumArticleAttachmentDownloadQuery query);

    /**
     * 根据条件查询数量
     */
    Integer findCountByParam(ForumArticleAttachmentDownloadQuery query);

    /**
     * 分页查询
     */
    PaginationResultVO<ForumArticleAttachmentDownload> findListByPage(ForumArticleAttachmentDownloadQuery query);

    /**
     * 新增
     */
    Integer add(ForumArticleAttachmentDownload bean);

    /**
     * 批量新增
     */
    Integer addBatch(List<ForumArticleAttachmentDownload> listBean);

    /**
     * 批量新增或修改
     */
    Integer addOrUpdateBatch(List<ForumArticleAttachmentDownload> listBean);

    /**
     * 根据FileIdAndUserId查询
     */
    ForumArticleAttachmentDownload getForumArticleAttachmentDownloadByFileIdAndUserId(String fileId, String userId);

    /**
     * 根据FileIdAndUserId更新
     */
    void updateForumArticleAttachmentDownloadByFileIdAndUserId(ForumArticleAttachmentDownload bean, String fileId, String userId);

    /**
     * 根据FileIdAndUserId删除
     */
    void deleteForumArticleAttachmentDownloadByFileIdAndUserId(String fileId, String userId);

}
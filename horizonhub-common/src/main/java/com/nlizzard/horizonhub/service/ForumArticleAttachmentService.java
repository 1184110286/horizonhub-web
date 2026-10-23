package com.nlizzard.horizonhub.service;

import com.nlizzard.horizonhub.entity.dto.SessionWebUserDto;
import com.nlizzard.horizonhub.entity.pojo.ForumArticleAttachment;
import com.nlizzard.horizonhub.entity.query.ForumArticleAttachmentQuery;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;

import java.util.List;

/**
 * @Description:文件信息Service
 * @author:nlizzard
 * @date:2026/03/08
 */
public interface ForumArticleAttachmentService {

    /**
     * 根据条件查询列表
     */
    List<ForumArticleAttachment> findListByParam(ForumArticleAttachmentQuery query);

    /**
     * 根据条件查询数量
     */
    Integer findCountByParam(ForumArticleAttachmentQuery query);

    /**
     * 分页查询
     */
    PaginationResultVO<ForumArticleAttachment> findListByPage(ForumArticleAttachmentQuery query);

    /**
     * 新增
     */
    Integer add(ForumArticleAttachment bean);

    /**
     * 批量新增
     */
    Integer addBatch(List<ForumArticleAttachment> listBean);

    /**
     * 批量新增或修改
     */
    Integer addOrUpdateBatch(List<ForumArticleAttachment> listBean);

    /**
     * 根据FileId查询
     */
    ForumArticleAttachment getForumArticleAttachmentByFileId(String fileId);

    /**
     * 根据FileId更新
     */
    void updateForumArticleAttachmentByFileId(ForumArticleAttachment bean, String fileId);

    /**
     * 根据FileId删除
     */
    void deleteForumArticleAttachmentByFileId(String fileId);

    /**
     * 判断附件是否存在，判断附件/用户是否下载过/积分是否够/是否是用户发布的附件，如果可以下载，记录下载记录，更新附件下载次数，并返回附件信息
     *
     * @param fileId            附件文件 ID
     * @param sessionWebUserDto 当前登录用户信息（session中）
     * @return ForumArticleAttachment 附件信息
     */
    ForumArticleAttachment downloadAttachment(String fileId, SessionWebUserDto sessionWebUserDto);
}
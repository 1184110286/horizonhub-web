package com.nlizzard.horizonhub.service.impl;

import com.nlizzard.horizonhub.entity.enums.PageSize;
import com.nlizzard.horizonhub.entity.pojo.ForumArticleAttachmentDownload;
import com.nlizzard.horizonhub.entity.query.ForumArticleAttachmentDownloadQuery;
import com.nlizzard.horizonhub.entity.query.basequery.SimplePage;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;
import com.nlizzard.horizonhub.mappers.ForumArticleAttachmentDownloadMapper;
import com.nlizzard.horizonhub.service.ForumArticleAttachmentDownloadService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Description:用户附件下载ServiceImpl
 * @author:nlizzard
 * @date:2026/03/08
 */
@Service("forumArticleAttachmentDownloadService")
public class ForumArticleAttachmentDownloadServiceImpl implements ForumArticleAttachmentDownloadService {

    @Resource
    private ForumArticleAttachmentDownloadMapper<ForumArticleAttachmentDownload, ForumArticleAttachmentDownloadQuery> forumArticleAttachmentDownloadMapper;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<ForumArticleAttachmentDownload> findListByParam(ForumArticleAttachmentDownloadQuery query) {
        return this.forumArticleAttachmentDownloadMapper.selectList(query);
    }

    /**
     * 根据条件查询数量
     */
    @Override
    public Integer findCountByParam(ForumArticleAttachmentDownloadQuery query) {
        return this.forumArticleAttachmentDownloadMapper.selectCount(query);
    }

    /**
     * 分页查询
     */
    @Override
    public PaginationResultVO<ForumArticleAttachmentDownload> findListByPage(ForumArticleAttachmentDownloadQuery query) {
        Integer count = this.findCountByParam(query);
        Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
        SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
        query.setSimplePage(page);
        List<ForumArticleAttachmentDownload> list = this.findListByParam(query);
        return new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
    }

    /**
     * 新增
     */
    @Override
    public Integer add(ForumArticleAttachmentDownload bean) {
        return this.forumArticleAttachmentDownloadMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<ForumArticleAttachmentDownload> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.forumArticleAttachmentDownloadMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或修改
     */
    @Override
    public Integer addOrUpdateBatch(List<ForumArticleAttachmentDownload> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.forumArticleAttachmentDownloadMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据FileIdAndUserId查询
     */
    @Override
    public ForumArticleAttachmentDownload getForumArticleAttachmentDownloadByFileIdAndUserId(String fileId, String userId) {
        return this.forumArticleAttachmentDownloadMapper.selectByFileIdAndUserId(fileId, userId);
    }

    /**
     * 根据FileIdAndUserId更新
     */
    @Override
    public void updateForumArticleAttachmentDownloadByFileIdAndUserId(ForumArticleAttachmentDownload bean, String fileId, String userId) {
        this.forumArticleAttachmentDownloadMapper.updateByFileIdAndUserId(bean, fileId, userId);
    }

    /**
     * 根据FileIdAndUserId删除
     */
    @Override
    public void deleteForumArticleAttachmentDownloadByFileIdAndUserId(String fileId, String userId) {
        this.forumArticleAttachmentDownloadMapper.deleteByFileIdAndUserId(fileId, userId);
    }

}
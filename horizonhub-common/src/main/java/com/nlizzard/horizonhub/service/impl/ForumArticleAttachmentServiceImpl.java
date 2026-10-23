package com.nlizzard.horizonhub.service.impl;

import com.nlizzard.horizonhub.entity.dto.SessionWebUserDto;
import com.nlizzard.horizonhub.entity.enums.*;
import com.nlizzard.horizonhub.entity.pojo.*;
import com.nlizzard.horizonhub.entity.query.ForumArticleAttachmentDownloadQuery;
import com.nlizzard.horizonhub.entity.query.ForumArticleAttachmentQuery;
import com.nlizzard.horizonhub.entity.query.ForumArticleQuery;
import com.nlizzard.horizonhub.entity.query.basequery.SimplePage;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;
import com.nlizzard.horizonhub.exception.BusinessException;
import com.nlizzard.horizonhub.mappers.ForumArticleAttachmentDownloadMapper;
import com.nlizzard.horizonhub.mappers.ForumArticleAttachmentMapper;
import com.nlizzard.horizonhub.mappers.ForumArticleMapper;
import com.nlizzard.horizonhub.service.ForumArticleAttachmentService;
import com.nlizzard.horizonhub.service.UserInfoService;
import com.nlizzard.horizonhub.service.UserMessageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @Description:文件信息ServiceImpl
 * @author:nlizzard
 * @date:2026/03/08
 */
@Service("forumArticleAttachmentService")
public class ForumArticleAttachmentServiceImpl implements ForumArticleAttachmentService {

    @Resource
    private ForumArticleAttachmentMapper<ForumArticleAttachment, ForumArticleAttachmentQuery> forumArticleAttachmentMapper;

    @Resource
    private ForumArticleAttachmentDownloadMapper<ForumArticleAttachmentDownload, ForumArticleAttachmentDownloadQuery> forumArticleAttachmentDownloadMapper;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private ForumArticleMapper<ForumArticle, ForumArticleQuery> forumArticleMapper;

    @Resource
    private UserMessageService userMessageService;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<ForumArticleAttachment> findListByParam(ForumArticleAttachmentQuery query) {
        return this.forumArticleAttachmentMapper.selectList(query);
    }

    /**
     * 根据条件查询数量
     */
    @Override
    public Integer findCountByParam(ForumArticleAttachmentQuery query) {
        return this.forumArticleAttachmentMapper.selectCount(query);
    }

    /**
     * 分页查询
     */
    @Override
    public PaginationResultVO<ForumArticleAttachment> findListByPage(ForumArticleAttachmentQuery query) {
        Integer count = this.findCountByParam(query);
        Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
        SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
        query.setSimplePage(page);
        List<ForumArticleAttachment> list = this.findListByParam(query);
        return new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
    }

    /**
     * 新增
     */
    @Override
    public Integer add(ForumArticleAttachment bean) {
        return this.forumArticleAttachmentMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<ForumArticleAttachment> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.forumArticleAttachmentMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或修改
     */
    @Override
    public Integer addOrUpdateBatch(List<ForumArticleAttachment> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.forumArticleAttachmentMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据FileId查询
     */
    @Override
    public ForumArticleAttachment getForumArticleAttachmentByFileId(String fileId) {
        return this.forumArticleAttachmentMapper.selectByFileId(fileId);
    }

    /**
     * 根据FileId更新
     */
    @Override
    public void updateForumArticleAttachmentByFileId(ForumArticleAttachment bean, String fileId) {
        this.forumArticleAttachmentMapper.updateByFileId(bean, fileId);
    }

    /**
     * 根据FileId删除
     */
    @Override
    public void deleteForumArticleAttachmentByFileId(String fileId) {
        this.forumArticleAttachmentMapper.deleteByFileId(fileId);
    }

    /**
     * 下载附件，扣除积分，记录下载日志和消息
     *
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ForumArticleAttachment downloadAttachment(String fileId, SessionWebUserDto sessionWebUserDto) {
        // 查询附件信息
        ForumArticleAttachment attachment = this.forumArticleAttachmentMapper.selectByFileId(fileId);
        if (null == attachment) {
            throw new BusinessException(ResponseCodeEnum.CODE_404.getCode(), "附件不存在");
        }
        //判断下载积分，如果已经下载过，无需积分
        ForumArticleAttachmentDownload download = null;
        // 如果附件需要下载积分，并且下载用户不是附件提供者
        // 则判断（是否已有下载记录来判断用户有没有购买过该附件），用户积分是否充足
        if (attachment.getIntegral() > 0 && !sessionWebUserDto.getUserId().equals(attachment.getUserId())) {
            // 查询用户是否已有下载记录来判断用户有没有购买过该附件
            download = this.forumArticleAttachmentDownloadMapper.selectByFileIdAndUserId(fileId, sessionWebUserDto.getUserId());
            if (download == null) {
                //用户未下载过，判断用户积分是否充足
                UserInfo userInfo = userInfoService.getUserInfoByUserId(sessionWebUserDto.getUserId());
                if (userInfo.getCurrentIntegral() - attachment.getIntegral() < 0) {
                    throw new BusinessException(ResponseCodeEnum.CODE_404.getCode(), "积分不够");
                }
            }
        }
        //在下载记录表中记录用户下载次数
        ForumArticleAttachmentDownload updateDownload = new ForumArticleAttachmentDownload();
        updateDownload.setArticleId(attachment.getArticleId());
        updateDownload.setFileId(attachment.getFileId());
        updateDownload.setUserId(sessionWebUserDto.getUserId());
        this.forumArticleAttachmentDownloadMapper.insertOrUpdate(updateDownload);

        //更新文章附件下载次数
        this.forumArticleAttachmentMapper.updateDownloadCount(fileId);

        //自己下载自己的附件 无需计算积分
        if (sessionWebUserDto.getUserId().equals(attachment.getUserId())) {
            return attachment;
        }
        if (download != null) {
            return attachment;
        }

        //扣除下载用户积分
        userInfoService.updateUserIntegral(sessionWebUserDto.getUserId(), UserIntegralOperTypeEnum.USER_DOWNLOAD_ATTACHMENT,
                UserIntegralChangeTypeEnum.REDUCE.getChangeType(), attachment.getIntegral());

        //给提供附件的用户增加积分
        userInfoService.updateUserIntegral(attachment.getUserId(), UserIntegralOperTypeEnum.DOWNLOAD_ATTACHMENT,
                UserIntegralChangeTypeEnum.ADD.getChangeType(), attachment.getIntegral());

        //记录消息
        if (!sessionWebUserDto.getUserId().equals(attachment.getUserId())) {
            ForumArticle forumArticle = forumArticleMapper.selectByArticleId(attachment.getArticleId());
            UserMessage userMessage = new UserMessage();
            userMessage.setMessageType(MessageTypeEnum.DOWNLOAD_ATTACHMENT.getType());
            userMessage.setCreateTime(new Date());
            userMessage.setArticleId(attachment.getArticleId());
            userMessage.setCommentId(0);
            userMessage.setSendUserId(sessionWebUserDto.getUserId());
            userMessage.setSendNickName(sessionWebUserDto.getNickName());
            userMessage.setStatus(MessageStatusEnum.NO_READ.getStatus());
            userMessage.setReceivedUserId(attachment.getUserId());
            userMessage.setArticleTitle(forumArticle.getTitle());
            userMessageService.add(userMessage);
        }
        // 返回附件信息
        return attachment;
    }
}
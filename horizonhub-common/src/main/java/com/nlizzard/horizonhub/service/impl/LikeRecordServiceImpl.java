package com.nlizzard.horizonhub.service.impl;

import com.nlizzard.horizonhub.entity.enums.*;
import com.nlizzard.horizonhub.entity.pojo.ForumArticle;
import com.nlizzard.horizonhub.entity.pojo.ForumComment;
import com.nlizzard.horizonhub.entity.pojo.LikeRecord;
import com.nlizzard.horizonhub.entity.pojo.UserMessage;
import com.nlizzard.horizonhub.entity.query.ForumArticleQuery;
import com.nlizzard.horizonhub.entity.query.ForumCommentQuery;
import com.nlizzard.horizonhub.entity.query.LikeRecordQuery;
import com.nlizzard.horizonhub.entity.query.UserMessageQuery;
import com.nlizzard.horizonhub.entity.query.basequery.SimplePage;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;
import com.nlizzard.horizonhub.exception.BusinessException;
import com.nlizzard.horizonhub.mappers.ForumArticleMapper;
import com.nlizzard.horizonhub.mappers.ForumCommentMapper;
import com.nlizzard.horizonhub.mappers.LikeRecordMapper;
import com.nlizzard.horizonhub.mappers.UserMessageMapper;
import com.nlizzard.horizonhub.service.LikeRecordService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @Description:点赞记录ServiceImpl
 * @author:nlizzard
 * @date:2026/03/08
 */
@Service("likeRecordService")
public class LikeRecordServiceImpl implements LikeRecordService {

    @Resource
    private LikeRecordMapper<LikeRecord, LikeRecordQuery> likeRecordMapper;

    @Resource
    private UserMessageMapper<UserMessage, UserMessageQuery> userMessageMapper;

    @Resource
    private ForumArticleMapper<ForumArticle, ForumArticleQuery> forumArticleMapper;

    @Resource
    private ForumCommentMapper<ForumComment, ForumCommentQuery> forumCommentMapper;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<LikeRecord> findListByParam(LikeRecordQuery query) {
        return this.likeRecordMapper.selectList(query);
    }

    /**
     * 根据条件查询数量
     */
    @Override
    public Integer findCountByParam(LikeRecordQuery query) {
        return this.likeRecordMapper.selectCount(query);
    }

    /**
     * 分页查询
     */
    @Override
    public PaginationResultVO<LikeRecord> findListByPage(LikeRecordQuery query) {
        Integer count = this.findCountByParam(query);
        Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
        SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
        query.setSimplePage(page);
        List<LikeRecord> list = this.findListByParam(query);
        return new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
    }

    /**
     * 新增
     */
    @Override
    public Integer add(LikeRecord bean) {
        return this.likeRecordMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<LikeRecord> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.likeRecordMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或修改
     */
    @Override
    public Integer addOrUpdateBatch(List<LikeRecord> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.likeRecordMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据OpId查询
     */
    @Override
    public LikeRecord getLikeRecordByOpId(Integer opId) {
        return this.likeRecordMapper.selectByOpId(opId);
    }

    /**
     * 根据OpId更新
     */
    @Override
    public Integer updateLikeRecordByOpId(LikeRecord bean, Integer opId) {
        return this.likeRecordMapper.updateByOpId(bean, opId);
    }

    /**
     * 根据OpId删除
     */
    @Override
    public Integer deleteLikeRecordByOpId(Integer opId) {
        return this.likeRecordMapper.deleteByOpId(opId);
    }

    /**
     * 根据ObjectIdAndUserIdAndOpType查询
     */
    @Override
    public LikeRecord getLikeRecordByObjectIdAndUserIdAndOpType(String objectId, String userId, Integer opType) {
        return this.likeRecordMapper.selectByObjectIdAndUserIdAndOpType(objectId, userId, opType);
    }

    /**
     * 根据ObjectIdAndUserIdAndOpType更新
     */
    @Override
    public Integer updateLikeRecordByObjectIdAndUserIdAndOpType(LikeRecord bean, String objectId, String userId, Integer opType) {
        return this.likeRecordMapper.updateByObjectIdAndUserIdAndOpType(bean, objectId, userId, opType);
    }

    /**
     * 根据ObjectIdAndUserIdAndOpType删除
     */
    @Override
    public Integer deleteLikeRecordByObjectIdAndUserIdAndOpType(String objectId, String userId, Integer opType) {
        return this.likeRecordMapper.deleteByObjectIdAndUserIdAndOpType(objectId, userId, opType);
    }

    /**
     * 根据ObjectIdAndUserIdAndOpType获取对象
     */
    @Override
    public LikeRecord getUserOperRecordByObjectIdAndUserIdAndOpType(String objectId, String userId, Integer opType) {
        return this.likeRecordMapper.selectByObjectIdAndUserIdAndOpType(objectId, userId, opType);
    }

    /**
     * 点赞/取消点赞
     */
    @Transactional(rollbackFor = Exception.class)
    public void doLike(String objectId, String userId, String nickName, OperRecordOpTypeEnum opTypeEnum) {
        UserMessage userMessage = new UserMessage();
        userMessage.setCreateTime(new Date());
        switch (opTypeEnum) {
            case ARTICLE_LIKE: // 文章点赞
                ForumArticle forumArticle = forumArticleMapper.selectByArticleId(objectId);
                if (null == forumArticle) {
                    throw new BusinessException("文章不存在");
                }
                articleLike(forumArticle, objectId, userId, opTypeEnum);
                userMessage.setArticleId(objectId);
                userMessage.setArticleTitle(forumArticle.getTitle());
                userMessage.setMessageType(MessageTypeEnum.ARTICLE_LIKE.getType());
                userMessage.setCommentId(0);
                userMessage.setReceivedUserId(forumArticle.getUserId());

                userMessage.setSendUserId(userId);
                userMessage.setSendNickName(nickName);
                userMessage.setStatus(MessageStatusEnum.NO_READ.getStatus());
                if (!userId.equals(userMessage.getReceivedUserId())) {
                    UserMessage userMessage1 = userMessageMapper.selectByArticleIdAndSendUserIdAndMessageType(objectId, userId, MessageTypeEnum.ARTICLE_LIKE.getType());
                    if (userMessage1 == null) {
                        userMessageMapper.insert(userMessage);
                    }
                }
                break;
            case COMMENT_LIKE: // 评论点赞
                ForumComment forumComment = forumCommentMapper.selectByCommentId(Integer.parseInt(objectId));
                if (null == forumComment) {
                    throw new BusinessException(ResponseCodeEnum.CODE_404.getCode(), "评论不存在");
                }
                commentLike(forumComment, objectId, userId, opTypeEnum);
                ForumArticle article = forumArticleMapper.selectByArticleId(forumComment.getArticleId());
                if (null == article) {
                    throw new BusinessException(ResponseCodeEnum.CODE_404.getCode(), "文章不存在");
                }
                userMessage.setArticleId(article.getArticleId());
                userMessage.setArticleTitle(article.getTitle());
                userMessage.setMessageType(MessageTypeEnum.COMMENT_LIKE.getType());
                userMessage.setCommentId(Integer.parseInt(objectId));
                userMessage.setReceivedUserId(forumComment.getUserId());
                userMessage.setMessageContent(forumComment.getContent());

                userMessage.setSendUserId(userId);
                userMessage.setSendNickName(nickName);
                userMessage.setStatus(MessageStatusEnum.NO_READ.getStatus());
                if (!userId.equals(userMessage.getReceivedUserId())) {
                    UserMessage userMessage1 = userMessageMapper.selectByArticleIdAndCommentIdAndSendUserIdAndMessageType(article.getArticleId(), Integer.parseInt(objectId), userId, MessageTypeEnum.COMMENT_LIKE.getType());
                    if (userMessage1 == null) {
                        userMessageMapper.insert(userMessage);
                    }
                }
                break;
        }

    }

    /**
     * 文章点赞，取消点赞
     *
     * @param forumArticle 文章对象
     * @param articleId    文章ID
     * @param userId       当前用户ID
     * @param opTypeEnum   记录表操作类型枚举（0:文章点赞，1:评论点赞）
     */
    public void articleLike(ForumArticle forumArticle, String articleId, String userId, OperRecordOpTypeEnum opTypeEnum) {
        int changCount;
        LikeRecord record = this.likeRecordMapper.selectByObjectIdAndUserIdAndOpType(articleId, userId, opTypeEnum.getType());
        if (record != null) { // 点赞记录不为空，取消点赞并且更新文章点赞数
            this.likeRecordMapper.deleteByObjectIdAndUserIdAndOpType(articleId, userId, opTypeEnum.getType());
            changCount = -1;
        } else {
            LikeRecord operRecord = new LikeRecord();
            operRecord.setObjectId(articleId);
            operRecord.setUserId(userId);
            operRecord.setOpType(opTypeEnum.getType());
            operRecord.setCreateTime(new Date());
            operRecord.setAuthorUserId(forumArticle.getUserId());
            this.likeRecordMapper.insert(operRecord);
            changCount = 1;
        }
        forumArticleMapper.updateArticleCount(UpdateArticleCountTypeEnum.GOOD_COUNT.getType(), changCount, articleId);
    }

    /**
     * 评论 点赞 取消点赞
     *
     * @param forumComment 评论对象
     * @param commentId    评论ID
     * @param userId       当前用户 ID
     * @param opTypeEnum   记录表操作类型枚举（0:文章点赞，1:评论点赞）
     */
    public void commentLike(ForumComment forumComment, String commentId, String userId, OperRecordOpTypeEnum opTypeEnum) {
        LikeRecord record = this.likeRecordMapper.selectByObjectIdAndUserIdAndOpType(commentId, userId, opTypeEnum.getType());
        if (record != null) { // 评论点赞记录不为空，取消点赞并且减少评论点赞数
            this.likeRecordMapper.deleteByObjectIdAndUserIdAndOpType(commentId, userId, opTypeEnum.getType());
            forumCommentMapper.updateCommentCount(-1, Integer.parseInt(commentId));
        } else {
            LikeRecord likeRecord = new LikeRecord();
            likeRecord.setObjectId(commentId);
            likeRecord.setUserId(userId);
            likeRecord.setOpType(opTypeEnum.getType());
            likeRecord.setCreateTime(new Date());
            likeRecord.setAuthorUserId(forumComment.getUserId());
            this.likeRecordMapper.insert(likeRecord);
            forumCommentMapper.updateCommentCount(1, Integer.parseInt(commentId));
        }
    }
}
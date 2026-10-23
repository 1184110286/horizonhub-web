package com.nlizzard.horizonhub.service.impl;

import com.nlizzard.horizonhub.entity.dto.UserMessageCountDto;
import com.nlizzard.horizonhub.entity.enums.MessageStatusEnum;
import com.nlizzard.horizonhub.entity.enums.MessageTypeEnum;
import com.nlizzard.horizonhub.entity.enums.PageSize;
import com.nlizzard.horizonhub.entity.pojo.UserMessage;
import com.nlizzard.horizonhub.entity.query.UserMessageQuery;
import com.nlizzard.horizonhub.entity.query.basequery.SimplePage;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;
import com.nlizzard.horizonhub.mappers.UserMessageMapper;
import com.nlizzard.horizonhub.service.UserMessageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @Description:用户消息ServiceImpl
 * @author:nlizzard
 * @date:2026/03/08
 */
@Service("userMessageService")
public class UserMessageServiceImpl implements UserMessageService {

    @Resource
    private UserMessageMapper<UserMessage, UserMessageQuery> userMessageMapper;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<UserMessage> findListByParam(UserMessageQuery query) {
        return this.userMessageMapper.selectList(query);
    }

    /**
     * 根据条件查询数量
     */
    @Override
    public Integer findCountByParam(UserMessageQuery query) {
        return this.userMessageMapper.selectCount(query);
    }

    /**
     * 分页查询
     */
    @Override
    public PaginationResultVO<UserMessage> findListByPage(UserMessageQuery query) {
        Integer count = this.findCountByParam(query);
        Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
        SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
        query.setSimplePage(page);
        List<UserMessage> list = this.findListByParam(query);
        return new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
    }

    /**
     * 新增
     */
    @Override
    public Integer add(UserMessage bean) {
        return this.userMessageMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<UserMessage> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userMessageMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或修改
     */
    @Override
    public Integer addOrUpdateBatch(List<UserMessage> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userMessageMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据MessageId查询
     */
    @Override
    public UserMessage getUserMessageByMessageId(Integer messageId) {
        return this.userMessageMapper.selectByMessageId(messageId);
    }

    /**
     * 根据MessageId更新
     */
    @Override
    public Integer updateUserMessageByMessageId(UserMessage bean, Integer messageId) {
        return this.userMessageMapper.updateByMessageId(bean, messageId);
    }

    /**
     * 根据MessageId删除
     */
    @Override
    public Integer deleteUserMessageByMessageId(Integer messageId) {
        return this.userMessageMapper.deleteByMessageId(messageId);
    }

    /**
     * 根据ArticleIdAndCommentIdAndSendUserIdAndMessageType查询
     */
    @Override
    public UserMessage getUserMessageByArticleIdAndCommentIdAndSendUserIdAndMessageType(String articleId, Integer commentId, String sendUserId, Integer messageType) {
        return this.userMessageMapper.selectByArticleIdAndCommentIdAndSendUserIdAndMessageType(articleId, commentId, sendUserId, messageType);
    }

    /**
     * 根据ArticleIdAndCommentIdAndSendUserIdAndMessageType更新
     */
    @Override
    public Integer updateUserMessageByArticleIdAndCommentIdAndSendUserIdAndMessageType(UserMessage bean, String articleId, Integer commentId, String sendUserId, Integer messageType) {
        return this.userMessageMapper.updateByArticleIdAndCommentIdAndSendUserIdAndMessageType(bean, articleId, commentId, sendUserId, messageType);
    }

    /**
     * 根据ArticleIdAndCommentIdAndSendUserIdAndMessageType删除
     */
    @Override
    public Integer deleteUserMessageByArticleIdAndCommentIdAndSendUserIdAndMessageType(String articleId, Integer commentId, String sendUserId, Integer messageType) {
        return this.userMessageMapper.deleteByArticleIdAndCommentIdAndSendUserIdAndMessageType(articleId, commentId, sendUserId, messageType);
    }

    /**
     * 获取用户未读消息数量
     */
    @Override
    public UserMessageCountDto getUserMessageCount(String userId) {
        UserMessageCountDto messageCountDto = new UserMessageCountDto();
        long totalCount = 0L;
        List<Map<Object, Object>> mapList = this.userMessageMapper.selectUserMessageCount(userId);
        for (Map<Object, Object> item : mapList) {
            Integer type = (Integer) item.get("messageType");
            Long count = (Long) item.get("count");
            totalCount = totalCount + count;
            MessageTypeEnum messageType = MessageTypeEnum.getByType(type);
            if (messageType != null) {
                switch (messageType) {
                    case SYS:
                        messageCountDto.setSys(count);
                        break;
                    case COMMENT:
                        messageCountDto.setReply(count);
                        break;
                    case ARTICLE_LIKE:
                        messageCountDto.setLikePost(count);
                        break;
                    case COMMENT_LIKE:
                        messageCountDto.setLikeComment(count);
                        break;
                    case DOWNLOAD_ATTACHMENT:
                        messageCountDto.setDownloadAttachment(count);
                        break;
                    default:
                }
            }
        }
        messageCountDto.setTotal(totalCount);
        return messageCountDto;
    }

    @Override
    public void readMessageByType(String receivedUserId, Integer messageType) {
        this.userMessageMapper.updateMessageStatusBatch(null, receivedUserId, messageType, MessageStatusEnum.READ.getStatus());
    }
}
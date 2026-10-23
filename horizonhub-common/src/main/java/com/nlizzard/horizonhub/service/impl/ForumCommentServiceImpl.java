package com.nlizzard.horizonhub.service.impl;

import com.nlizzard.horizonhub.constants.Constants;
import com.nlizzard.horizonhub.entity.dto.FileUploadDto;
import com.nlizzard.horizonhub.entity.enums.*;
import com.nlizzard.horizonhub.entity.pojo.ForumArticle;
import com.nlizzard.horizonhub.entity.pojo.ForumComment;
import com.nlizzard.horizonhub.entity.pojo.UserInfo;
import com.nlizzard.horizonhub.entity.pojo.UserMessage;
import com.nlizzard.horizonhub.entity.query.ForumArticleQuery;
import com.nlizzard.horizonhub.entity.query.ForumCommentQuery;
import com.nlizzard.horizonhub.entity.query.basequery.SimplePage;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;
import com.nlizzard.horizonhub.exception.BusinessException;
import com.nlizzard.horizonhub.mappers.ForumArticleMapper;
import com.nlizzard.horizonhub.mappers.ForumCommentMapper;
import com.nlizzard.horizonhub.service.ForumCommentService;
import com.nlizzard.horizonhub.service.UserInfoService;
import com.nlizzard.horizonhub.service.UserMessageService;
import com.nlizzard.horizonhub.utils.FileUtils;
import com.nlizzard.horizonhub.utils.SysCacheUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description:评论ServiceImpl
 * @author:nlizzard
 * @date:2026/03/08
 */
@Service("forumCommentService")
public class ForumCommentServiceImpl implements ForumCommentService {

    @Resource
    private ForumCommentMapper<ForumComment, ForumCommentQuery> forumCommentMapper;

    @Resource
    private ForumArticleMapper<ForumArticle, ForumArticleQuery> forumArticleMapper;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private UserMessageService userMessageService;

    @Resource
    private FileUtils fileUtils;

    @Lazy
    @Resource
    private ForumCommentService forumCommentService;

    @Resource
    private SysCacheUtils sysCacheUtils;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<ForumComment> findListByParam(ForumCommentQuery query) {
        // 拿到一级评论
        List<ForumComment> list = this.forumCommentMapper.selectList(query);
        //获取二级评论
        if (query.getLoadChildren() != null && query.getLoadChildren()) {
            // 复制部分一级评论查询条件
            ForumCommentQuery subQuery = new ForumCommentQuery();
            subQuery.setQueryIsLike(query.getQueryIsLike());
            subQuery.setCurrentUserId(query.getCurrentUserId());
            subQuery.setArticleId(query.getArticleId());
            subQuery.setLoadChildren(query.getLoadChildren());
            subQuery.setStatus(CommentStatusEnum.AUDIT.getStatus());
            subQuery.setOrderBy(CommentSortTypeEnum.SECOND_LEVEL_COMMENT_SORT_TYPE.getSortSQLField());
            // 根据当前分页结果中的一级评论查询二级评论
            List<Integer> parentCommentIdList = list.stream().map(ForumComment::getCommentId).collect(Collectors.toList());
            subQuery.setParentCommentIdList(parentCommentIdList);
            List<ForumComment> subCommentList = this.forumCommentMapper.selectList(subQuery);
            Map<Integer, List<ForumComment>> tempMap = subCommentList.stream().collect(Collectors.groupingBy(ForumComment::getPCommentId));
            list.forEach(item -> {
                item.setChildren(tempMap.get(item.getCommentId()));
            });
        }
        return list;
    }

    /**
     * 根据条件查询数量
     */
    @Override
    public Integer findCountByParam(ForumCommentQuery query) {
        return this.forumCommentMapper.selectCount(query);
    }

    /**
     * 分页查询
     */
    @Override
    public PaginationResultVO<ForumComment> findListByPage(ForumCommentQuery query) {
        Integer count = this.findCountByParam(query);
        Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
        SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
        query.setSimplePage(page);
        List<ForumComment> list = this.findListByParam(query);
        return new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
    }

    /**
     * 新增
     */
    @Override
    public Integer add(ForumComment bean) {
        return this.forumCommentMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<ForumComment> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.forumCommentMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或修改
     */
    @Override
    public Integer addOrUpdateBatch(List<ForumComment> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.forumCommentMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据CommentId查询
     */
    @Override
    public ForumComment getForumCommentByCommentId(Integer commentId) {
        return this.forumCommentMapper.selectByCommentId(commentId);
    }

    /**
     * 根据CommentId更新
     */
    @Override
    public Integer updateForumCommentByCommentId(ForumComment bean, Integer commentId) {
        return this.forumCommentMapper.updateByCommentId(bean, commentId);
    }

    /**
     * 根据CommentId删除
     */
    @Override
    public Integer deleteForumCommentByCommentId(Integer commentId) {
        return this.forumCommentMapper.deleteByCommentId(commentId);
    }

    /**
     * 置顶/取消置顶评论
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeTopType(String userId, Integer commentId, Integer topType) {
        // 判断是否置顶参数合法性
        CommentTopTypeEnum commentTopTypeEnum = CommentTopTypeEnum.getByType(topType);
        if (commentTopTypeEnum == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        // 检验评论是否存在，是否是一级评论（只能置顶一级评论）
        ForumComment forumComment = forumCommentMapper.selectByCommentId(commentId);
        if (forumComment == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_404.getCode(), "评论不存在");
        }
        if (forumComment.getPCommentId() != 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "只能置顶一级评论");
        }
        // 检验文章是否存在,是否是作者，是否具备置顶权限
        ForumArticle forumArticle = forumArticleMapper.selectByArticleId(forumComment.getArticleId());
        if (forumArticle == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_404.getCode(), "文章不存在");
        }
        if (!forumArticle.getUserId().equals(userId)) {
            throw new BusinessException("没有权限操作");
        }
        // 判断评论当前置顶状态与目标置顶状态是否一致，如果一致则无需操作
        if (forumComment.getTopType().equals(topType)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "评论已处于当前置顶状态，无需重复操作");
        }

        // 置顶评论
        ForumComment updateInfo = new ForumComment();
        updateInfo.setTopType(topType);
        forumCommentMapper.updateByCommentId(updateInfo, forumComment.getCommentId());
    }

    /**
     * 发表评论
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void postComment(ForumComment comment, MultipartFile image) {
        //判断文章是否存在
        ForumArticle forumArticle = forumArticleMapper.selectByArticleId(comment.getArticleId());
        if (forumArticle == null || !ArticleStatusEnum.AUDIT.getStatus().equals(forumArticle.getStatus())) {
            throw new BusinessException("评论的文章不存在");
        }
        //判断父级评论是否存在
        ForumComment pComment = null;
        if (comment.getPCommentId() != 0) {
            pComment = forumCommentMapper.selectByCommentId(comment.getPCommentId());
            if (pComment == null) {
                throw new BusinessException("回复的评论不存在");
            }
        }
        //判断被回复的用户是否存在
        if (!StringUtils.isBlank(comment.getReplyUserId())) {
            UserInfo userInfo = userInfoService.getUserInfoByUserId(comment.getReplyUserId());
            if (userInfo == null) {
                throw new BusinessException("回复的用户不存在");
            }
            comment.setReplyNickName(userInfo.getNickName());
        }
        comment.setPostTime(new Date());
        // 如果评论图片不为空，则上传图片并保存图片路径
        if (image != null) {
            FileUploadDto fileUploadDto = fileUtils.uploadFile2Local(image, FileUploadTypeEnum.COMMENT_IMAGE, Constants.FILE_FOLDER_IMAGE);
            comment.setImgPath(fileUploadDto.getLocalPath());
        }

        // 是否开启评论审核
        Boolean needAudit = sysCacheUtils.getSysSetting().getAuditSetting().getCommentAudit();

        //设置状态
        comment.setStatus(needAudit ? CommentStatusEnum.NO_AUDIT.getStatus() : CommentStatusEnum.AUDIT.getStatus());
        this.forumCommentMapper.insert(comment);

        // 如果需要审核，则不执行后续操作，等管理员审核通过后再执行后续操作
        if (needAudit) {
            return;
        }
        updateCommentInfo(comment, forumArticle, pComment);
    }

    /**
     * 更新评论相关信息（文章评论数、用户积分、消息记录等）
     *
     * @param comment      新发布的评论信息
     * @param forumArticle 评论所属的文章信息
     * @param pComment     父级评论信息
     */
    public void updateCommentInfo(ForumComment comment, ForumArticle forumArticle, ForumComment pComment) {
        // 拿到系统设置的发表评论奖励积分数
        Integer commentIntegral = sysCacheUtils.getSysSetting().getCommentSetting().getCommentIntegral();
        if (commentIntegral > 0) {
            this.userInfoService.updateUserIntegral(comment.getUserId(), UserIntegralOperTypeEnum.POST_COMMENT, UserIntegralChangeTypeEnum.ADD.getChangeType(), commentIntegral);
        }

        if (comment.getPCommentId() == 0) {
            this.forumArticleMapper.updateArticleCount(UpdateArticleCountTypeEnum.COMMENT_COUNT.getType(), 1, comment.getArticleId());
        }

        //记录消息
        UserMessage userMessage = new UserMessage();
        userMessage.setMessageType(MessageTypeEnum.COMMENT.getType());
        userMessage.setCreateTime(new Date());
        userMessage.setArticleId(forumArticle.getArticleId());
        userMessage.setCommentId(comment.getCommentId());
        userMessage.setSendUserId(comment.getUserId());
        userMessage.setSendNickName(comment.getNickName());
        userMessage.setStatus(MessageStatusEnum.NO_READ.getStatus());
        userMessage.setMessageContent(comment.getContent());
        userMessage.setArticleTitle(forumArticle.getTitle());
        if (comment.getPCommentId() == 0) {
            userMessage.setReceivedUserId(forumArticle.getUserId());
        } else if (comment.getPCommentId() != 0 && StringUtils.isEmpty(comment.getReplyUserId())) {
            userMessage.setReceivedUserId(pComment.getUserId());
        } else if (comment.getPCommentId() != 0 && !StringUtils.isEmpty(comment.getReplyUserId())) {
            userMessage.setReceivedUserId(comment.getReplyUserId());
        }
        if (!comment.getUserId().equals(userMessage.getReceivedUserId())) {
            userMessageService.add(userMessage);
        }
    }

    /**
     * 删除评论
     *
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delComment(String commentIds) {
        String[] commentIdArray = commentIds.split(",");
        for (String commentIdStr : commentIdArray) {
            Integer commentId = Integer.parseInt(commentIdStr);
            forumCommentService.delCommentSingle(commentId);
        }
    }

    /**
     * 删除单条评论
     *
     * @param commentId 评论 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void delCommentSingle(Integer commentId) {
        ForumComment comment = forumCommentMapper.selectByCommentId(commentId);
        // 如果评论不存在或者评论已经被删除，则不执行任何操作
        if (null == comment || CommentStatusEnum.DEL.getStatus().equals(comment.getStatus())) {
            return;
        }
        // 删除评论
        ForumComment forumComment = new ForumComment();
        forumComment.setStatus(CommentStatusEnum.DEL.getStatus());
        forumCommentMapper.updateByCommentId(forumComment, commentId);

        // 如果评论处于审核通过状态，则需要更新文章评论数、用户积分，并记录消息
        if (CommentStatusEnum.AUDIT.getStatus().equals(comment.getStatus())) {
            // 如果是一级评论被删除，则文章评论数减1
            if (comment.getPCommentId() == 0) {
                forumArticleMapper.updateArticleCount(UpdateArticleCountTypeEnum.COMMENT_COUNT.getType(), -1, comment.getArticleId());
            }
            // 拿到系统设置的发表评论奖励积分数，并扣除相应积分
            Integer integral = sysCacheUtils.getSysSetting().getCommentSetting().getCommentIntegral();
            userInfoService.updateUserIntegral(comment.getUserId(), UserIntegralOperTypeEnum.DEL_COMMENT, UserIntegralChangeTypeEnum.REDUCE.getChangeType(), integral);
        }
        // 记录消息
        UserMessage userMessage = new UserMessage();
        userMessage.setReceivedUserId(comment.getUserId());
        userMessage.setMessageType(MessageTypeEnum.SYS.getType());
        userMessage.setCreateTime(new Date());
        userMessage.setStatus(MessageStatusEnum.NO_READ.getStatus());
        userMessage.setMessageContent("评论【" + (comment.getContent() == null ? "图片" : comment.getContent()) + "】被管理员删除");
        userMessageService.add(userMessage);
    }

    /**
     * 审核评论
     *
     * @param commentIds 评论 ID，逗号分割
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditComment(String commentIds) {
        String[] commentIdArray = commentIds.split(",");
        for (String commentIdStr : commentIdArray) {
            Integer commentId = Integer.parseInt(commentIdStr);
            forumCommentService.auditCommentSingle(commentId);
        }
    }

    /**
     * 审核单条评论
     *
     * @param commentId 评论 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void auditCommentSingle(Integer commentId) {
        ForumComment comment = forumCommentMapper.selectByCommentId(commentId);
        // 如果评论不存在或者评论状态不是待审核状态，则不执行任何操作
        if (!CommentStatusEnum.NO_AUDIT.getStatus().equals(comment.getStatus())) {
            return;
        }
        // 更新评论状态
        ForumComment forumComment = new ForumComment();
        forumComment.setStatus(CommentStatusEnum.AUDIT.getStatus());
        forumCommentMapper.updateByCommentId(forumComment, commentId);

        // 获取评论所属文章信息和父级评论信息
        ForumArticle forumArticle = forumArticleMapper.selectByArticleId(comment.getArticleId());
        ForumComment pComment = null;
        if (comment.getPCommentId() != 0 && StringUtils.isBlank(comment.getReplyUserId())) {
            pComment = forumCommentMapper.selectByCommentId(comment.getPCommentId());
        }
        //更新评论相关信息
        updateCommentInfo(comment, forumArticle, pComment);
    }
}
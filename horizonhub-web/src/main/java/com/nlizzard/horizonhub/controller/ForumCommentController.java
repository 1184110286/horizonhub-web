package com.nlizzard.horizonhub.controller;

import com.nlizzard.horizonhub.annotation.GlobalInterceptor;
import com.nlizzard.horizonhub.annotation.VerifyParam;
import com.nlizzard.horizonhub.basecontroller.BaseController;
import com.nlizzard.horizonhub.entity.dto.SessionWebUserDto;
import com.nlizzard.horizonhub.entity.enums.*;
import com.nlizzard.horizonhub.entity.pojo.ForumComment;
import com.nlizzard.horizonhub.entity.pojo.LikeRecord;
import com.nlizzard.horizonhub.entity.query.ForumCommentQuery;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;
import com.nlizzard.horizonhub.entity.vo.ResponseVO;
import com.nlizzard.horizonhub.exception.BusinessException;
import com.nlizzard.horizonhub.service.ForumCommentService;
import com.nlizzard.horizonhub.service.LikeRecordService;
import com.nlizzard.horizonhub.utils.StringTools;
import com.nlizzard.horizonhub.utils.SysCacheUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/comment")
public class ForumCommentController extends BaseController {

    @Resource
    private ForumCommentService forumCommentService;

    @Resource
    private LikeRecordService likeRecordService;

    @Resource
    private SysCacheUtils sysCacheUtils;

    /**
     * 分页加载评论
     *
     * @param articleId 文章 ID
     * @param pageNo    当前页码
     * @param orderType 排序类型，0-默认排序（按点赞数和评论ID升序），1-按评论ID降序
     */
    @RequestMapping("/loadComment")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<PaginationResultVO<ForumComment>> loadComment(HttpSession session,
                                                                    @VerifyParam(required = true) String articleId,
                                                                    Integer pageNo,
                                                                    Integer orderType) {
        // 检查系统设置中是否开启评论功能
        if (!sysCacheUtils.getSysSetting().getCommentSetting().getCommentOpen()) {
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
        // 构建评论查询对象
        ForumCommentQuery commentQuery = new ForumCommentQuery();
        commentQuery.setArticleId(articleId);
        commentQuery.setLoadChildren(true);
        //置顶排序
        String commentSortFiled = CommentSortTypeEnum.TOP_SORT_TYPE.getSortSQLField();
        // 默认使用最热排序
        if (orderType == null || !orderType.equals(CommentSortTypeEnum.NEW_SORT_TYPE.getType())) {
            commentSortFiled += CommentSortTypeEnum.HOT_SORT_TYPE.getSortSQLField();
        } else {
            commentSortFiled += CommentSortTypeEnum.NEW_SORT_TYPE.getSortSQLField();
        }
        commentQuery.setOrderBy(commentSortFiled);

        SessionWebUserDto userDto = getUserInfoFromSession(session);
        if (userDto != null) {
            commentQuery.setQueryIsLike(true);
            commentQuery.setCurrentUserId(userDto.getUserId());
        }
        // 设置分页参数
        commentQuery.setPageNo(pageNo);
        commentQuery.setPageSize(PageSize.SIZE50.getSize());
        commentQuery.setStatus(CommentStatusEnum.AUDIT.getStatus());
        // 只查询一级评论，子评论通过 loadChildren 参数加载
        commentQuery.setPCommentId(0);
        return getSuccessResponseVO(forumCommentService.findListByPage(commentQuery));
    }

    /**
     * 点赞/取消点赞评论
     *
     * @param session   用户会话
     * @param commentId 评论 ID
     * @return 更新后的评论信息
     */
    @PostMapping("/doLike")
    @GlobalInterceptor(checkLogin = true, checkParams = true, frequencyType = UserOperFrequencyTypeEnum.DO_LIKE)
    public ResponseVO<ForumComment> doLike(HttpSession session,
                                           @VerifyParam(required = true) Integer commentId) {
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        // 更新点赞记录表（类型为评论点赞）
        likeRecordService.doLike(String.valueOf(commentId), userDto.getUserId(), userDto.getNickName(), OperRecordOpTypeEnum.COMMENT_LIKE);
        // 获取用户对该评论的点赞记录，判断是否已点赞
        LikeRecord userOperRecord = likeRecordService.getUserOperRecordByObjectIdAndUserIdAndOpType(String.valueOf(commentId), userDto.getUserId(),
                OperRecordOpTypeEnum.COMMENT_LIKE.getType());
        ForumComment comment = forumCommentService.getForumCommentByCommentId(commentId);
        comment.setLikeType(userOperRecord == null ? 0 : 1);
        return getSuccessResponseVO(comment);
    }

    /**
     * 置顶/取消置顶评论
     *
     * @param commentId 评论 ID
     * @param topType   置顶类型，0-取消置顶，1-置顶
     */
    @PostMapping("/changeTopType")
    @GlobalInterceptor(checkLogin = true, checkParams = true)
    public ResponseVO<Void> changeTopType(HttpSession session,
                                          @VerifyParam(required = true) Integer commentId,
                                          @VerifyParam(required = true) Integer topType) {
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        forumCommentService.changeTopType(userDto.getUserId(), commentId, topType);
        return getSuccessResponseVO(null);
    }

    /**
     * 发表评论
     *
     * @param articleId   文章 ID
     * @param pCommentId  父评论 ID
     * @param content     评论内容
     * @param replyUserId 被回复用户 ID（如果是回复评论，则传入被回复用户 ID）
     * @param image       评论图片
     */
    @PostMapping("/postComment")
    @GlobalInterceptor(checkLogin = true, checkParams = true, frequencyType = UserOperFrequencyTypeEnum.POST_COMMENT)
    public ResponseVO<Object> postComment(HttpSession session,
                                          @VerifyParam(required = true) String articleId,
                                          @VerifyParam(required = true) Integer pCommentId,
                                          @VerifyParam(min = 5, max = 800) String content,
                                          String replyUserId,
                                          MultipartFile image) {
        // 系统是否关闭评论
        if (!sysCacheUtils.getSysSetting().getCommentSetting().getCommentOpen()) {
            throw new BusinessException(ResponseCodeEnum.CODE_404.getCode(), "当前系统已关闭评论功能");
        }
        // 评论内容和图片不能同时为空
        if (image == null && StringUtils.isBlank(content)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "评论内容和图片不能同时为空");
        }
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        ForumComment comment = new ForumComment();
        // 对评论内容进行 HTML 转义，防止 XSS 攻击
        content = StringTools.escapeHtml(content);
        comment.setUserId(userDto.getUserId());
        comment.setNickName(userDto.getNickName());
        comment.setUserIpAddress(userDto.getProvince());
        comment.setPCommentId(pCommentId);
        comment.setArticleId(articleId);
        comment.setContent(content);
        comment.setReplyUserId(replyUserId);
        comment.setTopType(CommentTopTypeEnum.NO_TOP.getType());
        forumCommentService.postComment(comment, image);
        // 如果是二级评论
        if (pCommentId != 0) {
            ForumCommentQuery commentQuery = new ForumCommentQuery();
            commentQuery.setArticleId(articleId);
            commentQuery.setPCommentId(pCommentId);
            commentQuery.setOrderBy(CommentSortTypeEnum.SECOND_LEVEL_COMMENT_SORT_TYPE.getSortSQLField());
            commentQuery.setCurrentUserId(userDto.getUserId()); // 设置当前用户 ID，查询时会把自己发表的评论（未审核）查出来
            List<ForumComment> children = forumCommentService.findListByParam(commentQuery);
            return getSuccessResponseVO(children);
        }
        return getSuccessResponseVO(comment);
    }
}

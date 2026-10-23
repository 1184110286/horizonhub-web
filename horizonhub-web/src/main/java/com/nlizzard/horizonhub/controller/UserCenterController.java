package com.nlizzard.horizonhub.controller;

import com.nlizzard.horizonhub.annotation.GlobalInterceptor;
import com.nlizzard.horizonhub.annotation.VerifyParam;
import com.nlizzard.horizonhub.basecontroller.BaseController;
import com.nlizzard.horizonhub.entity.dto.SessionWebUserDto;
import com.nlizzard.horizonhub.entity.dto.UserMessageCountDto;
import com.nlizzard.horizonhub.entity.enums.ArticleStatusEnum;
import com.nlizzard.horizonhub.entity.enums.MessageTypeEnum;
import com.nlizzard.horizonhub.entity.enums.ResponseCodeEnum;
import com.nlizzard.horizonhub.entity.enums.UserStatusEnum;
import com.nlizzard.horizonhub.entity.pojo.ForumArticle;
import com.nlizzard.horizonhub.entity.pojo.UserInfo;
import com.nlizzard.horizonhub.entity.pojo.UserIntegralRecord;
import com.nlizzard.horizonhub.entity.pojo.UserMessage;
import com.nlizzard.horizonhub.entity.query.ForumArticleQuery;
import com.nlizzard.horizonhub.entity.query.LikeRecordQuery;
import com.nlizzard.horizonhub.entity.query.UserIntegralRecordQuery;
import com.nlizzard.horizonhub.entity.query.UserMessageQuery;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;
import com.nlizzard.horizonhub.entity.vo.ResponseVO;
import com.nlizzard.horizonhub.entity.vo.web.ForumArticleVO;
import com.nlizzard.horizonhub.entity.vo.web.UserInfoVO;
import com.nlizzard.horizonhub.entity.vo.web.UserMessageVO;
import com.nlizzard.horizonhub.exception.BusinessException;
import com.nlizzard.horizonhub.service.*;
import com.nlizzard.horizonhub.utils.CopyTools;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ucenter")
public class UserCenterController extends BaseController {

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private ForumArticleService forumArticleService;

    @Resource
    private UserMessageService userMessageService;

    @Resource
    private LikeRecordService likeRecordService;

    @Resource
    private UserIntegralRecordService userIntegralRecordService;

    /**
     * 获取用户信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    @RequestMapping("/getUserInfo")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<UserInfoVO> getUserInfo(@VerifyParam(required = true) String userId) {
        UserInfo userInfo = userInfoService.getUserInfoByUserId(userId);
        if (null == userInfo || UserStatusEnum.DISABLE.getStatus().equals(userInfo.getStatus())) {
            throw new BusinessException(ResponseCodeEnum.CODE_404.getCode(), "用户不存在或已被封禁");
        }
        // 统计已发表的文章数量
        ForumArticleQuery articleQuery = new ForumArticleQuery();
        articleQuery.setUserId(userId);
        articleQuery.setStatus(ArticleStatusEnum.AUDIT.getStatus());
        Integer postCount = forumArticleService.findCountByParam(articleQuery);

        UserInfoVO userInfoVO = CopyTools.copy(userInfo, UserInfoVO.class);
        userInfoVO.setPostCount(postCount);
        // 统计已获得的全部点赞数量(文章和评论的点赞数量)
        LikeRecordQuery recordQuery = new LikeRecordQuery();
        recordQuery.setAuthorUserId(userId);
        Integer likeCount = likeRecordService.findCountByParam(recordQuery);
        userInfoVO.setLikeCount(likeCount);
        // 当前积分
        userInfoVO.setCurrentIntegral(userInfo.getCurrentIntegral());
        return getSuccessResponseVO(userInfoVO);
    }

    /**
     * 更新用户信息
     *
     * @param session           会话
     * @param sex               性别
     * @param personDescription 个人简介
     * @param avatar            头像
     * @return 响应结果
     */
    @PostMapping("/updateUserInfo")
    @GlobalInterceptor(checkParams = true, checkLogin = true)
    public ResponseVO<Void> updateUserInfo(HttpSession session,
                                           Integer sex,
                                           @VerifyParam(max = 100) String personDescription,
                                           MultipartFile avatar) {
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userDto.getUserId());
        userInfo.setSex(sex);
        userInfo.setPersonDescription(personDescription);
        userInfoService.updateUserInfo(userInfo, avatar);
        return getSuccessResponseVO(null);
    }

    /**
     * 用户积分记录分页查询
     *
     * @param session         会话
     * @param pageNo          页码
     * @param createTimeStart 创建时间开始
     * @param createTimeEnd   创建时间结束
     * @return 分页结果
     */
    @RequestMapping("/loadUserIntegralRecord")
    @GlobalInterceptor(checkParams = true, checkLogin = true)
    public ResponseVO<PaginationResultVO<UserIntegralRecord>> loadUserIntegralRecord(HttpSession session,
                                                                                     Integer pageNo,
                                                                                     String createTimeStart,
                                                                                     String createTimeEnd) {
        // 构建查询类
        UserIntegralRecordQuery recordQuery = new UserIntegralRecordQuery();
        recordQuery.setUserId(getUserInfoFromSession(session).getUserId());
        recordQuery.setPageNo(pageNo);
        recordQuery.setCreateTimeStart(createTimeStart);
        recordQuery.setCreateTimeEnd(createTimeEnd);
        recordQuery.setOrderBy("record_id desc");
        PaginationResultVO<UserIntegralRecord> resultVO = userIntegralRecordService.findListByPage(recordQuery);
        return getSuccessResponseVO(resultVO);
    }

    /**
     * 加载用户文章列表
     *
     * @param session 会话
     * @param userId  用户ID
     * @param type    类型：0-我发布的，1-我评论的，2-我点赞的
     * @param pageNo  页码
     * @return
     */
    @RequestMapping("/loadUserArticle")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<PaginationResultVO<ForumArticleVO>> loadUserArticle(HttpSession session,
                                                                          @VerifyParam(required = true) String userId,
                                                                          @VerifyParam(required = true) Integer type,
                                                                          Integer pageNo) {
        // 验证用户是否存在且未被封禁
        UserInfo userInfo = userInfoService.getUserInfoByUserId(userId);
        if (null == userInfo || UserStatusEnum.DISABLE.getStatus().equals(userInfo.getStatus())) {
            throw new BusinessException(ResponseCodeEnum.CODE_404.getCode(), "用户不存在或已被封禁");
        }

        ForumArticleQuery articleQuery = new ForumArticleQuery();
        articleQuery.setOrderBy("post_time desc");
        if (type == 0) {
            articleQuery.setUserId(userId);
        } else if (type == 1) {
            articleQuery.setCommentUserId(userId);
        } else if (type == 2) {
            articleQuery.setLikeUserId(userId);
        }
        // 如果用户登录，需要展示当前用户待审核的文章，否则只展示审核通过的文章
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        if (userDto != null) {
            articleQuery.setCurrentUserId(userDto.getUserId());
        } else {
            articleQuery.setStatus(ArticleStatusEnum.AUDIT.getStatus());
        }
        articleQuery.setPageNo(pageNo);
        PaginationResultVO<ForumArticle> result = forumArticleService.findListByPage(articleQuery);
        return getSuccessResponseVO(convert2PaginationVO(result, ForumArticleVO.class));
    }

    /**
     * 获取用户未读消息数量
     *
     * @param session 会话
     * @return 消息数量
     */
    @RequestMapping("/getMessageCount")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<UserMessageCountDto> getMessageCount(HttpSession session) {
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        if (null == userDto) {
            return getSuccessResponseVO(new UserMessageCountDto());
        }
        return getSuccessResponseVO(userMessageService.getUserMessageCount(userDto.getUserId()));
    }

    /**
     * 加载用户消息列表
     *
     * @param session 会话
     * @param code    消息类型编码
     * @param pageNo  页码
     * @return 消息列表
     */
    @RequestMapping("/loadMessageList")
    @GlobalInterceptor(checkLogin = true, checkParams = true)
    public ResponseVO<PaginationResultVO<UserMessageVO>> loadMessageList(HttpSession session,
                                                                         @VerifyParam(required = true) String code,
                                                                         Integer pageNo) {
        MessageTypeEnum messageTypeEnum = MessageTypeEnum.getByCode(code);
        if (null == messageTypeEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "消息类型不存在");
        }
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        UserMessageQuery userMessageQuery = new UserMessageQuery();
        userMessageQuery.setPageNo(pageNo);
        userMessageQuery.setReceivedUserId(userDto.getUserId());
        userMessageQuery.setMessageType(messageTypeEnum.getType());
        // message_id(自增)倒序，保证最新的消息在前面
        userMessageQuery.setOrderBy("message_id desc");
        PaginationResultVO<UserMessage> result = userMessageService.findListByPage(userMessageQuery);
        if (pageNo == null || pageNo == 1) {
            //根据类型将所有消息变更为为已读状态
            userMessageService.readMessageByType(userDto.getUserId(), messageTypeEnum.getType());
        }
        PaginationResultVO<UserMessageVO> resultVO = convert2PaginationVO(result, UserMessageVO.class);
        return getSuccessResponseVO(resultVO);
    }
}

package com.nlizzard.horizonhub.controller;

import com.nlizzard.horizonhub.annotation.GlobalInterceptor;
import com.nlizzard.horizonhub.annotation.VerifyParam;
import com.nlizzard.horizonhub.basecontroller.BaseController;
import com.nlizzard.horizonhub.constants.Constants;
import com.nlizzard.horizonhub.entity.config.WebConfig;
import com.nlizzard.horizonhub.entity.dto.SessionWebUserDto;
import com.nlizzard.horizonhub.entity.enums.*;
import com.nlizzard.horizonhub.entity.pojo.*;
import com.nlizzard.horizonhub.entity.query.ForumArticleAttachmentQuery;
import com.nlizzard.horizonhub.entity.query.ForumArticleQuery;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;
import com.nlizzard.horizonhub.entity.vo.ResponseVO;
import com.nlizzard.horizonhub.entity.vo.web.FormArticleDetailVO;
import com.nlizzard.horizonhub.entity.vo.web.FormArticleUpdateDetailVO;
import com.nlizzard.horizonhub.entity.vo.web.ForumArticleAttachmentVO;
import com.nlizzard.horizonhub.entity.vo.web.ForumArticleVO;
import com.nlizzard.horizonhub.exception.BusinessException;
import com.nlizzard.horizonhub.service.*;
import com.nlizzard.horizonhub.utils.CopyTools;
import com.nlizzard.horizonhub.utils.StringTools;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/forum")
public class ForumArticleController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(ForumArticleController.class);

    @Resource
    private ForumArticleService forumArticleService;

    @Resource
    private ForumArticleAttachmentService forumArticleAttachmentService;

    @Resource
    private LikeRecordService likeRecordService;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private ForumArticleAttachmentDownloadService forumArticleAttachmentDownloadService;

    @Resource
    private WebConfig webConfig;

    @Resource
    private ForumBoardService forumBoardService;

    /**
     * 加载文章列表（首页）
     *
     * @param session   session
     * @param boardId   文章板块 ID
     * @param pBoardId  文章父级板块 ID
     * @param orderType 排序类型 0:默认排序 1:最新发布 2:最热
     * @param pageNo    页码
     * @return
     */
    @RequestMapping("/loadArticle")
    public ResponseVO<PaginationResultVO<ForumArticleVO>> loadArticle(HttpSession session,
                                                                      Integer boardId,
                                                                      Integer pBoardId,
                                                                      Integer orderType,
                                                                      Integer pageNo) {
        ForumArticleQuery articleQuery = new ForumArticleQuery();
        // 如果boardId为0或null，则查询所有板块的文章
        articleQuery.setBoardId(boardId == null || boardId == 0 ? null : boardId);
        articleQuery.setpBoardId(pBoardId);
        articleQuery.setPageNo(pageNo);

        SessionWebUserDto userDto = getUserInfoFromSession(session);
        if (userDto != null) {
            // 已登录用户可以看到自己的文章和审核通过的文章
            articleQuery.setCurrentUserId(userDto.getUserId());
        } else {
            // 未登录用户只能看到审核通过的文章
            articleQuery.setStatus(ArticleStatusEnum.AUDIT.getStatus());
        }
        // 设置排序方式 根据orderType参数设置排序方式，默认为最热
        ArticleOrderTypeEnum orderTypeEnum = ArticleOrderTypeEnum.getByType(orderType);
        orderTypeEnum = orderTypeEnum == null ? ArticleOrderTypeEnum.HOT : orderTypeEnum;
        articleQuery.setOrderBy(orderTypeEnum.getOrderSql());

        PaginationResultVO<ForumArticle> resultVO = forumArticleService.findListByPage(articleQuery);
        // 转换为 VO 对象并返回
        return getSuccessResponseVO(convert2PaginationVO(resultVO, ForumArticleVO.class));
    }

    /**
     * 加载文章板块树（发帖页）
     *
     * @param session
     * @return
     */
    @RequestMapping("/loadBoard4Post")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<List<ForumBoard>> loadBoard4Post(HttpSession session) {
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        Integer postType = ForumBoardPostTypeEnum.ALL_BOARD.getCode(); // 传null,sql语句就不会加上post_type条件,就可以查询所有板块
        if (!userDto.getAdmin()) {
            postType = ForumBoardPostTypeEnum.USER_BOARD.getCode();
        }
        return getSuccessResponseVO(forumBoardService.getBoardTree(postType));
    }


    /**
     * 发布文章
     *
     * @param session
     * @param cover           封面图片
     * @param attachment      附件
     * @param integral        下载附件需要的积分
     * @param pBoardId        父级板块 ID
     * @param boardId         板块 ID
     * @param title           文章标题
     * @param content         文章内容（HTML）
     * @param markdownContent 文章内容（Markdown）
     * @param editorType      编辑器类型 1:富文本编辑器 2:Markdown 编辑器
     * @param summary         文章摘要
     * @return
     */
    @PostMapping("/postArticle")
    @GlobalInterceptor(checkLogin = true, checkParams = true, frequencyType = UserOperFrequencyTypeEnum.POST_ARTICLE)
    public ResponseVO<String> postArticle(HttpSession session,
                                          MultipartFile cover,
                                          MultipartFile attachment,
                                          Integer integral,
                                          @VerifyParam(required = true) Integer pBoardId,
                                          Integer boardId,
                                          @VerifyParam(required = true, max = 50) String title,
                                          @VerifyParam String content,
                                          String markdownContent,
                                          @VerifyParam(required = true) Integer editorType,
                                          @VerifyParam(max = 200) String summary) {
        title = StringTools.escapeTitle(title);
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        ForumArticle forumArticle = new ForumArticle();
        forumArticle.setPBoardId(pBoardId);
        forumArticle.setBoardId(boardId);
        forumArticle.setTitle(title);
        forumArticle.setContent(content);
        if (EditorTypeEnum.MARKDOWN.getType().equals(editorType) && StringUtils.isEmpty(markdownContent)) {
            throw new BusinessException("编辑器为Markdown编辑器,Markdown内容不能为空");
        }
        forumArticle.setMarkdownContent(markdownContent);
        forumArticle.setEditorType(editorType);
        forumArticle.setSummary(summary);
        forumArticle.setUserId(userDto.getUserId());
        forumArticle.setNickName(userDto.getNickName());
        forumArticle.setUserIpAddress(userDto.getProvince());
        //附件信息
        ForumArticleAttachment forumArticleAttachment = new ForumArticleAttachment();
        forumArticleAttachment.setIntegral(integral == null ? 0 : integral);
        forumArticleService.postArticle(userDto.getAdmin(), forumArticle, forumArticleAttachment, cover, attachment);
        return getSuccessResponseVO(forumArticle.getArticleId());
    }

    /**
     * 获取文章详情
     *
     * @param session   session
     * @param articleId 文章 ID
     * @return
     */
    @RequestMapping("/getArticleDetail")
    public ResponseVO<FormArticleDetailVO> getArticleDetail(HttpSession session, String articleId) {
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);

        // 根据文章ID查询文章详情
        ForumArticle forumArticle = forumArticleService.readArticle(articleId);

        // 文章不存在，或者文章未审核且当前用户不是作者或管理员，或者文章已删除，则返回404
        if (forumArticle == null
                || (ArticleStatusEnum.NO_AUDIT.getStatus().equals(forumArticle.getStatus()) && (sessionWebUserDto == null || !sessionWebUserDto.getUserId().equals(forumArticle.getUserId()) && !sessionWebUserDto.getAdmin()))
                || ArticleStatusEnum.DEL.getStatus().equals(forumArticle.getStatus())) {
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
        // 组装文章详情VO,如果文章有附件，则查询附件信息并设置到VO中，如果用户已登录，则查询用户是否点赞过该文章并设置到VO中
        FormArticleDetailVO detailVO = new FormArticleDetailVO();
        detailVO.setForumArticle(CopyTools.copy(forumArticle, ForumArticleVO.class));
        // 如果文章有附件，则查询附件信息并设置到VO中
        if (forumArticle.getAttachmentType() == 1) {
            ForumArticleAttachmentQuery articleAttachmentQuery = new ForumArticleAttachmentQuery();
            articleAttachmentQuery.setArticleId(forumArticle.getArticleId());
            List<ForumArticleAttachment> forumArticleAttachmentList = forumArticleAttachmentService.findListByParam(articleAttachmentQuery);
            if (!forumArticleAttachmentList.isEmpty()) {
                detailVO.setAttachment(CopyTools.copy(forumArticleAttachmentList.get(0), ForumArticleAttachmentVO.class));
            }
        }
        // 如果用户已登录，则查询用户是否点赞过该文章并设置到VO中
        if (sessionWebUserDto != null) {
            LikeRecord like = likeRecordService.getUserOperRecordByObjectIdAndUserIdAndOpType(articleId, sessionWebUserDto.getUserId(), OperRecordOpTypeEnum.ARTICLE_LIKE.getType());
            if (like != null) {
                detailVO.setHaveLike(true);
            }
        }
        return getSuccessResponseVO(detailVO);
    }

    /**
     * 获取文章详情（编辑文章时调用）
     *
     * @param session
     * @param articleId 文章 ID
     * @return
     */
    @RequestMapping("/articleDetail4Update")
    @GlobalInterceptor(checkLogin = true, checkParams = true)
    public ResponseVO<FormArticleUpdateDetailVO> articleDetail4Update(HttpSession session,
                                                                      @VerifyParam(required = true) String articleId) {
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        ForumArticle forumArticle = forumArticleService.getForumArticleByArticleId(articleId);
        if (forumArticle == null || !forumArticle.getUserId().equals(userDto.getUserId())) {
            throw new BusinessException("文章不存在或你没有权限编辑该文章");
        }
        // 组装文章详情和附件信息VO
        FormArticleUpdateDetailVO detailVO = new FormArticleUpdateDetailVO();
        detailVO.setForumArticle(forumArticle);
        // 如果文章有附件，则查询附件信息并设置到VO中
        if (forumArticle.getAttachmentType().equals(HasAttachmentEnum.YES.getCode())) {
            ForumArticleAttachmentQuery articleAttachmentQuery = new ForumArticleAttachmentQuery();
            articleAttachmentQuery.setArticleId(forumArticle.getArticleId());
            List<ForumArticleAttachment> forumArticleAttachmentList = forumArticleAttachmentService.findListByParam(articleAttachmentQuery);
            if (!forumArticleAttachmentList.isEmpty()) {
                detailVO.setAttachment(CopyTools.copy(forumArticleAttachmentList.get(0), ForumArticleAttachmentVO.class));
            }
        }
        return getSuccessResponseVO(detailVO);
    }

    /**
     * 更新文章
     *
     * @param session
     * @param cover           封面图片
     * @param attachment      附件
     * @param integral        下载附件需要的积分
     * @param articleId       文章 ID
     * @param pBoardId        父级板块 ID
     * @param boardId         板块 ID
     * @param title           文章标题
     * @param content         文章内容（HTML）
     * @param markdownContent 文章内容（Markdown）
     * @param editorType      编辑器类型 1:富文本编辑器 2:Markdown 编辑器
     * @param summary         文章摘要
     * @param attachmentType  附件类型 0:无附件 1:有附件
     * @return
     */
    @PostMapping("/updateArticle")
    @GlobalInterceptor(checkLogin = true, checkParams = true)
    public ResponseVO<String> updateArticle(HttpSession session,
                                            MultipartFile cover,
                                            MultipartFile attachment,
                                            Integer integral,
                                            @VerifyParam(required = true) String articleId,
                                            @VerifyParam(required = true) Integer pBoardId,
                                            Integer boardId,
                                            @VerifyParam(required = true, max = 150) String title,
                                            @VerifyParam(required = true) String content,
                                            String markdownContent,
                                            @VerifyParam(required = true) Integer editorType,
                                            @VerifyParam(max = 200) String summary,
                                            @VerifyParam(required = true) Integer attachmentType) {
        title = StringTools.escapeTitle(title);
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        ForumArticle forumArticle = new ForumArticle();
        forumArticle.setArticleId(articleId);
        forumArticle.setPBoardId(pBoardId);
        forumArticle.setBoardId(boardId);
        forumArticle.setTitle(title);
        forumArticle.setContent(content);
        forumArticle.setMarkdownContent(markdownContent);
        forumArticle.setEditorType(editorType);
        forumArticle.setSummary(summary);
        forumArticle.setUserIpAddress(userDto.getProvince());
        forumArticle.setAttachmentType(attachmentType);
        forumArticle.setUserId(userDto.getUserId());
        //附件信息
        ForumArticleAttachment forumArticleAttachment = new ForumArticleAttachment();
        forumArticleAttachment.setIntegral(integral == null ? 0 : integral);

        forumArticleService.updateArticle(userDto.getAdmin(), forumArticle, forumArticleAttachment, cover, attachment);
        return getSuccessResponseVO(forumArticle.getArticleId());
    }

    /**
     * 文章点赞/取消点赞
     *
     * @param session
     * @param articleId 文章 ID
     * @return
     */
    @PostMapping("/doLike")
    @GlobalInterceptor(checkLogin = true, checkParams = true, frequencyType = UserOperFrequencyTypeEnum.DO_LIKE)
    public ResponseVO<Void> doLike(HttpSession session, @VerifyParam(required = true) String articleId) {
        SessionWebUserDto userDto = getUserInfoFromSession(session);
        likeRecordService.doLike(articleId, userDto.getUserId(), userDto.getNickName(), OperRecordOpTypeEnum.ARTICLE_LIKE);
        return getSuccessResponseVO(null);
    }

    /**
     * 获取用户下载信息（积分和是否已下载过文件）
     *
     * @param session
     * @param fileId  附件文件 ID
     * @return
     */
    @RequestMapping("/getUserDownloadInfo")
    @GlobalInterceptor(checkLogin = true, checkParams = true)
    public ResponseVO<Map<String, Object>> getUserDownloadInfo(HttpSession session,
                                                               @VerifyParam(required = true) String fileId) {
        Map<String, Object> result = new HashMap<>();
        // 取用户当前积分
        UserInfo userInfo = userInfoService.getUserInfoByUserId(getUserInfoFromSession(session).getUserId());
        result.put("userIntegral", userInfo.getCurrentIntegral());
        // 查询用户是否已下载过该附件
        ForumArticleAttachmentDownload attachmentDownload = forumArticleAttachmentDownloadService.getForumArticleAttachmentDownloadByFileIdAndUserId(fileId,
                getUserInfoFromSession(session).getUserId());
        result.put("haveDownload", attachmentDownload != null);
        return getSuccessResponseVO(result);
    }

    /**
     * 附件下载
     *
     * @param session
     * @param request
     * @param response
     * @param fileId   附件文件 ID
     */
    // 注意：附件下载必须用 GET。浏览器通过 <a href> / window.open 触发下载，
    // POST 响应无法作为文件下载。故此接口保留 GET（与写操作改 POST 的 CSRF 防护不冲突：
    // 下载虽有扣积分副作用，但由登录态保护；纯 GET 文件流）。
    @GetMapping("/attachmentDownload")
    @GlobalInterceptor(checkLogin = true, checkParams = true)
    public void attachmentDownload(HttpSession session, HttpServletRequest request, HttpServletResponse response,
                                   @VerifyParam(required = true) String fileId) {
        // 扣除积分，记录下载记录和消息，并获取附件信息
        ForumArticleAttachment attachment = forumArticleAttachmentService.downloadAttachment(fileId, getUserInfoFromSession(session));
        InputStream in = null;
        OutputStream out = null;
        String downloadFileName = attachment.getFileName();
        // 拼接附件文件存放路径
        String filePath = webConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + File.separator + Constants.FILE_FOLDER_ATTACHMENT + File.separator + attachment.getFilePath();
        File file = new File(filePath);
        try {
            in = new FileInputStream(file);
            out = response.getOutputStream();
            response.setContentType("application/x-msdownload; charset=UTF-8");
            // 解决中文文件名乱码问题
            if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0) {//IE浏览器
                downloadFileName = URLEncoder.encode(downloadFileName, "UTF-8");
            } else {
                downloadFileName = new String(downloadFileName.getBytes("UTF-8"), "ISO8859-1");
            }
            response.setHeader("Content-Disposition", "attachment;filename=\"" + downloadFileName + "\"");
            byte[] byteData = new byte[1024];
            int len = 0;
            while ((len = in.read(byteData)) != -1) {
                out.write(byteData, 0, len); // write
            }
            out.flush();
        } catch (Exception e) {
            logger.error("下载异常", e);
            throw new BusinessException("下载失败");
        } finally {
            try {
                if (in != null) {
                    in.close();
                }

            } catch (IOException e) {
                logger.error("IO异常", e);
            }
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                logger.error("IO异常", e);
            }
        }
    }

    /**
     * 搜索
     *
     * @param keyword 关键词
     * @return 分页的文章列表
     */
    @RequestMapping("/search")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<PaginationResultVO<ForumArticle>> updateArticle(@VerifyParam(required = true) String keyword) {
        ForumArticleQuery query = new ForumArticleQuery();
        query.setTitleFuzzy(keyword);
        PaginationResultVO<ForumArticle> result = forumArticleService.findListByPage(query);
        return getSuccessResponseVO(result);
    }
}

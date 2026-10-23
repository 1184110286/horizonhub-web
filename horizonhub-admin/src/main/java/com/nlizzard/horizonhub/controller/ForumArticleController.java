package com.nlizzard.horizonhub.controller;


import com.nlizzard.horizonhub.annotation.GlobalInterceptor;
import com.nlizzard.horizonhub.annotation.VerifyParam;
import com.nlizzard.horizonhub.basecontroller.BaseController;
import com.nlizzard.horizonhub.constants.Constants;
import com.nlizzard.horizonhub.entity.config.AdminConfig;
import com.nlizzard.horizonhub.entity.pojo.ForumArticle;
import com.nlizzard.horizonhub.entity.pojo.ForumArticleAttachment;
import com.nlizzard.horizonhub.entity.pojo.ForumComment;
import com.nlizzard.horizonhub.entity.query.ForumArticleAttachmentQuery;
import com.nlizzard.horizonhub.entity.query.ForumArticleQuery;
import com.nlizzard.horizonhub.entity.query.ForumCommentQuery;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;
import com.nlizzard.horizonhub.entity.vo.ResponseVO;
import com.nlizzard.horizonhub.exception.BusinessException;
import com.nlizzard.horizonhub.service.ForumArticleAttachmentService;
import com.nlizzard.horizonhub.service.ForumArticleService;
import com.nlizzard.horizonhub.service.ForumCommentService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.net.URLEncoder;
import java.util.List;

@RestController
@RequestMapping("/forum")
public class ForumArticleController extends BaseController {
    private static final Logger logger = LoggerFactory.getLogger(ForumArticleController.class);

    @Resource
    private ForumArticleService forumArticleService;

    @Resource
    private ForumCommentService forumCommentService;

    @Resource
    private ForumArticleAttachmentService forumArticleAttachmentService;

    @Resource
    private AdminConfig adminConfig;

    /**
     * 分页加载全部文章
     *
     * @param articleQuery 查询类
     */
    @RequestMapping("/loadArticle")
    public ResponseVO<PaginationResultVO<ForumArticle>> loadArticle(ForumArticleQuery articleQuery) {
        articleQuery.setOrderBy("post_time desc");
        return getSuccessResponseVO(forumArticleService.findListByPage(articleQuery));
    }


    /**
     * 批量删除文章
     *
     * @param articleIds 文章 ID，逗号分隔
     */
    @PostMapping("/delArticle")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<Void> delArticle(@VerifyParam(required = true) String articleIds) {
        forumArticleService.delArticle(articleIds);
        return getSuccessResponseVO(null);
    }

    /**
     * 更新文章版块
     *
     * @param articleId 文章 ID
     * @param pBoardId  父板块 ID
     * @param boardId   板块ID
     */
    @PostMapping("/updateBoard")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<Void> updateBoard(@VerifyParam(required = true) String articleId, @VerifyParam(required = true) Integer pBoardId, Integer boardId) {
        boardId = boardId == null ? 0 : boardId;
        forumArticleService.updateBoard(articleId, pBoardId, boardId);
        return getSuccessResponseVO(null);
    }

    /**
     * 获取文章附件
     *
     * @param articleId 文章 ID
     */
    @RequestMapping("/getAttachment")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<ForumArticleAttachment> getAttachment(@VerifyParam(required = true) String articleId) {
        ForumArticleAttachmentQuery articleAttachmentQuery = new ForumArticleAttachmentQuery();
        articleAttachmentQuery.setArticleId(articleId);
        List<ForumArticleAttachment> attachmentList = forumArticleAttachmentService.findListByParam(articleAttachmentQuery);

        if (attachmentList.isEmpty()) {
            throw new BusinessException("附件不存在");
        }

        return getSuccessResponseVO(attachmentList.get(0));
    }

    /**
     * 下载文章附件
     *
     * @param fileId 附件文件 ID
     */
    @RequestMapping("/attachmentDownload")
    @GlobalInterceptor(checkParams = true)
    public void attachmentDownload(HttpServletRequest request,
                                   HttpServletResponse response,
                                   @VerifyParam(required = true) String fileId) {
        // 查附件
        ForumArticleAttachment attachment = forumArticleAttachmentService.getForumArticleAttachmentByFileId(fileId);
        InputStream in = null;
        OutputStream out = null;
        String downloadFileName = attachment.getFileName();
        String filePath = adminConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + "/" + Constants.FILE_FOLDER_ATTACHMENT + "/" + attachment.getFilePath();
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
     * 置顶/取消置顶文章
     *
     * @param topType   0：取消置顶，1：置顶
     * @param articleId 文章 ID
     * @return
     */
    @PostMapping("/topArticle")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<Void> topArticle(@VerifyParam(required = true) Integer topType,
                                       @VerifyParam(required = true) String articleId) {
        ForumArticle forumArticle = new ForumArticle();
        forumArticle.setTopType(topType);
        forumArticleService.updateForumArticleByArticleId(forumArticle, articleId);
        return getSuccessResponseVO(null);
    }

    /**
     * 审核文章
     *
     * @param articleIds 文章 ID，逗号分隔
     */
    @PostMapping("/auditArticle")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<Void> auditArticle(@VerifyParam(required = true) String articleIds) {
        forumArticleService.auditArticle(articleIds);
        return getSuccessResponseVO(null);
    }


    /**
     * 分页加载评论
     *
     * @param commentQuery 评论查询类
     */
    @RequestMapping("/loadComment")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<PaginationResultVO<ForumComment>> loadComment(ForumCommentQuery commentQuery) {
        // 加载二级评论
        commentQuery.setLoadChildren(true);
        // 默认按发帖时间倒序
        commentQuery.setOrderBy("post_time desc");
        return getSuccessResponseVO(forumCommentService.findListByPage(commentQuery));
    }


    /**
     * 加载对应文章的评论
     *
     * @param commentQuery 评论查询类
     */
    @RequestMapping("/loadComment4Article")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<List<ForumComment>> loadComment4Article(ForumCommentQuery commentQuery) {
        commentQuery.setLoadChildren(true);
        commentQuery.setOrderBy("post_time desc");
        commentQuery.setPCommentId(0);
        return getSuccessResponseVO(forumCommentService.findListByParam(commentQuery));
    }

    /**
     * 删除评论
     *
     * @param commentIds 评论id
     */
    @PostMapping("/delComment")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<Void> delComment(@VerifyParam(required = true) String commentIds) {
        forumCommentService.delComment(commentIds);
        return getSuccessResponseVO(null);
    }

    /**
     * 审核评论
     *
     * @param commentIds 评论ID
     */
    @PostMapping("/auditComment")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<Void> auditComment(@VerifyParam(required = true) String commentIds) {
        forumCommentService.auditComment(commentIds);
        return getSuccessResponseVO(null);
    }
}

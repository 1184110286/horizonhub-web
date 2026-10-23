package com.nlizzard.horizonhub.service.impl;

import cn.hutool.core.util.IdUtil;
import com.nlizzard.horizonhub.constants.Constants;
import com.nlizzard.horizonhub.entity.config.AppConfig;
import com.nlizzard.horizonhub.entity.dto.FileUploadDto;
import com.nlizzard.horizonhub.entity.dto.SysSetting4AuditDto;
import com.nlizzard.horizonhub.entity.enums.*;
import com.nlizzard.horizonhub.entity.pojo.ForumArticle;
import com.nlizzard.horizonhub.entity.pojo.ForumArticleAttachment;
import com.nlizzard.horizonhub.entity.pojo.ForumBoard;
import com.nlizzard.horizonhub.entity.pojo.UserMessage;
import com.nlizzard.horizonhub.entity.query.ForumArticleAttachmentQuery;
import com.nlizzard.horizonhub.entity.query.ForumArticleQuery;
import com.nlizzard.horizonhub.entity.query.basequery.SimplePage;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;
import com.nlizzard.horizonhub.exception.BusinessException;
import com.nlizzard.horizonhub.mappers.ForumArticleAttachmentMapper;
import com.nlizzard.horizonhub.mappers.ForumArticleMapper;
import com.nlizzard.horizonhub.service.ForumArticleService;
import com.nlizzard.horizonhub.service.ForumBoardService;
import com.nlizzard.horizonhub.service.UserInfoService;
import com.nlizzard.horizonhub.service.UserMessageService;
import com.nlizzard.horizonhub.utils.FileUtils;
import com.nlizzard.horizonhub.utils.HtmlSanitizer;
import com.nlizzard.horizonhub.utils.ImageUtils;
import com.nlizzard.horizonhub.utils.SysCacheUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * @Description:文章信息ServiceImpl
 * @author:nlizzard
 * @date:2026/03/08
 */
@Service("forumArticleService")
public class ForumArticleServiceImpl implements ForumArticleService {

    private static final Logger logger = LoggerFactory.getLogger(ForumArticleServiceImpl.class);

    @Resource
    private ForumArticleMapper<ForumArticle, ForumArticleQuery> forumArticleMapper;

    @Resource
    private ForumBoardService forumBoardService;

    @Resource
    private FileUtils fileUtils;

    @Resource
    private HtmlSanitizer htmlSanitizer;

    @Resource
    private ImageUtils imageUtils;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private AppConfig appConfig;

    @Resource
    private ForumArticleAttachmentMapper<ForumArticleAttachment, ForumArticleAttachmentQuery> forumArticleAttachmentMapper;

    @Lazy
    @Resource
    private ForumArticleService forumArticleService;

    @Resource
    private UserMessageService userMessageService;

    @Resource
    private SysCacheUtils sysCacheUtils;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<ForumArticle> findListByParam(ForumArticleQuery query) {
        return this.forumArticleMapper.selectList(query);
    }

    /**
     * 根据条件查询数量
     */
    @Override
    public Integer findCountByParam(ForumArticleQuery query) {
        return this.forumArticleMapper.selectCount(query);
    }

    /**
     * 分页查询
     */
    @Override
    public PaginationResultVO<ForumArticle> findListByPage(ForumArticleQuery query) {
        Integer count = this.findCountByParam(query);
        Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
        SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
        query.setSimplePage(page);
        List<ForumArticle> list = this.findListByParam(query);
        return new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
    }

    /**
     * 新增
     */
    @Override
    public Integer add(ForumArticle bean) {
        return this.forumArticleMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<ForumArticle> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.forumArticleMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或修改
     */
    @Override
    public Integer addOrUpdateBatch(List<ForumArticle> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.forumArticleMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据ArticleId查询
     */
    @Override
    public ForumArticle getForumArticleByArticleId(String articleId) {
        return this.forumArticleMapper.selectByArticleId(articleId);
    }

    /**
     * 根据ArticleId更新
     */
    @Override
    public void updateForumArticleByArticleId(ForumArticle bean, String articleId) {
        this.forumArticleMapper.updateByArticleId(bean, articleId);
    }

    /**
     * 根据ArticleId删除
     */
    @Override
    public Integer deleteForumArticleByArticleId(String articleId) {
        return this.forumArticleMapper.deleteByArticleId(articleId);
    }

    /**
     * 文章详情获取
     */
    @Override
    public ForumArticle readArticle(String articleId) {
        ForumArticle forumArticle = this.forumArticleMapper.selectByArticleId(articleId);
        if (forumArticle == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_404);
        }
        // 只有审核通过的文章才增加阅读数
        if (ArticleStatusEnum.AUDIT.getStatus().equals(forumArticle.getStatus())) {
            forumArticleMapper.updateArticleCount(UpdateArticleCountTypeEnum.READ_COUNT.getType(), 1, articleId);
        }
        return forumArticle;
    }

    /**
     * 发帖
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void postArticle(Boolean isAdmin, ForumArticle article, ForumArticleAttachment forumArticleAttachment, MultipartFile cover, MultipartFile attachment) {
        // 检查文章信息是否合法（不能相信任何前端传过来的字段）
        checkArticle(isAdmin, article);

        String articleId = IdUtil.getSnowflakeNextIdStr();
        article.setArticleId(articleId);
        article.setPostTime(new Date());
        article.setLastUpdateTime(new Date());

        // 封面不为空则上传封面
        if (cover != null) {
            FileUploadDto fileUploadDto = fileUtils.uploadFile2Local(cover, FileUploadTypeEnum.ARTICLE_COVER, Constants.FILE_FOLDER_IMAGE);
            article.setCover(fileUploadDto.getLocalPath());
        }

        // 附件不为空则上传附件
        if (attachment != null) {
            article.setAttachmentType(HasAttachmentEnum.YES.getCode());
            uploadAttachment(article, forumArticleAttachment, attachment, false);
        } else {
            article.setAttachmentType(HasAttachmentEnum.NO.getCode());
        }

        // 是否管理员发帖，管理员发帖直接审核通过，普通用户发帖根据系统设置判断是否需要审核
        if (isAdmin) {
            article.setStatus(ArticleStatusEnum.AUDIT.getStatus());
        } else {
            SysSetting4AuditDto auditDto = sysCacheUtils.getSysSetting().getAuditSetting();
            article.setStatus(auditDto.getPostAudit() ? ArticleStatusEnum.NO_AUDIT.getStatus() :
                    ArticleStatusEnum.AUDIT.getStatus());
        }

        // 先对富文本正文做 XSS 白名单清洗，再处理图片路径
        String content = htmlSanitizer.clean(article.getContent());
        if (!StringUtils.isBlank(content)) {
            String month = imageUtils.resetImagePathInHtml(content);
            //避免替换文章中用户编写的temp，所以前后带上/，只换了图片路径中的temp
            String replaceMonth = "/" + month + "/";
            content = content.replace("/" + Constants.FILE_FOLDER_TEMP + "/", replaceMonth);
            article.setContent(content);
        }
        // markdown 正文独立处理图片路径（纯 Markdown 帖子的富文本 content 为空，
        // 不能嵌套在上面 if 里，否则 temp 图片不会被移动、路径不会被替换，导致图片不显示）
        String markdownContent = article.getMarkdownContent();
        if (!StringUtils.isBlank(markdownContent)) {
            String mdMonth = imageUtils.resetImagePathInMarkdown(markdownContent);
            String mdReplaceMonth = "/" + mdMonth + "/";
            markdownContent = markdownContent.replace("/" + Constants.FILE_FOLDER_TEMP + "/", mdReplaceMonth);
            article.setMarkdownContent(markdownContent);
        }

        this.forumArticleMapper.insert(article);

        //增加积分
        Integer postIntegral = sysCacheUtils.getSysSetting().getPostSetting().getPostIntegral();
        if (postIntegral > 0 && ArticleStatusEnum.AUDIT.getStatus().equals(article.getStatus())) {
            this.userInfoService.updateUserIntegral(article.getUserId(),
                    UserIntegralOperTypeEnum.POST_COMMENT, UserIntegralChangeTypeEnum.ADD.getChangeType(), postIntegral);
        }
    }

    /**
     * 检查文章信息是否合法
     *
     * @param isAdmin 是否是管理员
     * @param article 文章信息
     */
    private void checkArticle(Boolean isAdmin, ForumArticle article) {
        // 检查编辑器类型参数是否合法
        EditorTypeEnum editorTypeEnum = EditorTypeEnum.getByType(article.getEditorType());
        if (null == editorTypeEnum) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        // 检查文章摘要是否合法
        if (!StringUtils.isEmpty(article.getSummary()) && article.getSummary().length() > Constants.LENGTH_200) {
            throw new BusinessException("文章摘要信息非法（为空或长度超过200）");
        }
        // 检查板块内容是否合法
        checkBoardInfo(isAdmin, article);
    }

    /**
     * 检查板块信息是否合法
     *
     * @param isAdmin 是否是管理员
     * @param article 文章信息
     */
    private void checkBoardInfo(Boolean isAdmin, ForumArticle article) {
        ForumBoard board = forumBoardService.getForumBoardByBoardId(article.getPBoardId());
        if (null == board || board.getPostType() == 0 && !isAdmin) {
            throw new BusinessException("一级板块不存在或者没有权限在该板块发帖");
        }
        article.setPBoardName(board.getBoardName());
        if (article.getBoardId() != null && article.getBoardId() != 0) {
            board = forumBoardService.getForumBoardByBoardId(article.getBoardId());
            if (null == board || board.getPostType() == 0 && !isAdmin) {
                throw new BusinessException("二级板块不存在或者没有权限在该板块发帖");
            }
            article.setBoardName(board.getBoardName());
        } else {
            article.setBoardId(0);
            article.setBoardName("");
        }
    }

    /**
     * 上传文章附件
     *
     * @param article    文章信息
     * @param attachment 附件信息
     * @param file       附件文件对象
     * @param isUpdate   是否是更新附件（如果是更新附件，则先删除之前的附件）
     */
    public void uploadAttachment(ForumArticle article, ForumArticleAttachment attachment, MultipartFile file, Boolean isUpdate) {
        // 查询系统设置的附件大小限制，单位MB，转换成字节
        Integer allowSizeMb = sysCacheUtils.getSysSetting().getPostSetting().getAttachmentSize();
        long allowSize = (long) allowSizeMb * Constants.FILE_SIZE_1M;
        if (file.getSize() > allowSize) {
            throw new BusinessException("附件最大只能" + allowSizeMb + "MB");
        }
        ForumArticleAttachment dbInfo = null;
        // 检查是否更新，如果是更新附件，则先删除之前的附件（数据库记录和本地文件都删除）
        if (isUpdate) {
            ForumArticleAttachmentQuery attachmentQuery = new ForumArticleAttachmentQuery();
            attachmentQuery.setArticleId(article.getArticleId());
            List<ForumArticleAttachment> articleAttachmentList = forumArticleAttachmentMapper.selectList(attachmentQuery);
            if (!articleAttachmentList.isEmpty()) {
                // 当前系统设置了文章只能有一个附件，所以只会有一条记录  （可扩展）
                dbInfo = articleAttachmentList.get(0);
                //删除之前的附件
                File formerAttachment = new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + "/" + Constants.FILE_FOLDER_ATTACHMENT + "/" + dbInfo.getFilePath());
                if (!formerAttachment.delete()) {
                    logger.error("删除附件文件失败，文件路径：{}", formerAttachment.getPath());
                }
            }
        }
        // 上传附件
        FileUploadDto fileUploadDto = fileUtils.uploadFile2Local(file, FileUploadTypeEnum.ARTICLE_ATTACHMENT, Constants.FILE_FOLDER_ATTACHMENT);
        if (dbInfo == null) {
            attachment.setFileId(IdUtil.getSnowflakeNextIdStr());
            attachment.setArticleId(article.getArticleId());
            attachment.setFileName(fileUploadDto.getOriginalFilename());
            attachment.setFilePath(fileUploadDto.getLocalPath());
            attachment.setFileSize(file.getSize());
            attachment.setDownloadCount(0);
            attachment.setUserId(article.getUserId());
            attachment.setFileType(AttachmentFileTypeEnum.ZIP.getType());
            forumArticleAttachmentMapper.insert(attachment);
        } else {
            ForumArticleAttachment updateInfo = new ForumArticleAttachment();
            updateInfo.setFileName(fileUploadDto.getOriginalFilename());
            updateInfo.setFileSize(file.getSize());
            updateInfo.setFilePath(fileUploadDto.getLocalPath());
            forumArticleAttachmentMapper.updateByFileId(updateInfo, dbInfo.getFileId());
        }
    }

    /**
     * 更新文章
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArticle(Boolean isAdmin, ForumArticle article, ForumArticleAttachment forumArticleAttachment, MultipartFile cover, MultipartFile attachment) throws BusinessException {
        // 判断文章所属权
        ForumArticle dbInfo = forumArticleMapper.selectByArticleId(article.getArticleId());
        if (!isAdmin && !dbInfo.getUserId().equals(article.getUserId())) {
            throw new BusinessException("你没有权限修改该文章");
        }
        // 检验文章信息和所属的板块信息是否合法
        checkArticle(isAdmin, article);
        article.setLastUpdateTime(new Date());

        // 上传封面
        if (cover != null) {
            FileUploadDto fileUploadDto = fileUtils.uploadFile2Local(cover, FileUploadTypeEnum.ARTICLE_COVER, Constants.FILE_FOLDER_IMAGE);
            article.setCover(fileUploadDto.getLocalPath());
        }
        // 如果更新了附件，则上传附件（如果之前有附件则先删除之前的附件）
        if (attachment != null) {
            article.setAttachmentType(HasAttachmentEnum.YES.getCode());
            //上传附件
            uploadAttachment(article, forumArticleAttachment, attachment, true);
        }

        // 获取数据库中的附件
        ForumArticleAttachmentQuery attachmentQuery = new ForumArticleAttachmentQuery();
        attachmentQuery.setArticleId(article.getArticleId());
        List<ForumArticleAttachment> articleAttachmentList = this.forumArticleAttachmentMapper.selectList(attachmentQuery);
        ForumArticleAttachment dbAttachment = null;
        if (!articleAttachmentList.isEmpty()) {
            dbAttachment = articleAttachmentList.get(0);
        }

        // 数据库有附件
        if (dbAttachment != null) {
            // 此次更新把附件删了，则删除之前的附件（数据库记录和本地文件都删除）
            if (article.getAttachmentType().equals(HasAttachmentEnum.NO.getCode())) {
                //删除之前的附件
                new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + "/" + Constants.FILE_FOLDER_ATTACHMENT + "/" + dbAttachment.getFilePath()).delete();
                this.forumArticleAttachmentMapper.deleteByFileId(dbAttachment.getFileId());
            } else {
                // 此次更新没有更改附件，检查是否需要修改信息
                if (!dbAttachment.getIntegral().equals(forumArticleAttachment.getIntegral())) {
                    ForumArticleAttachment integralUpdate = new ForumArticleAttachment();
                    integralUpdate.setIntegral(forumArticleAttachment.getIntegral());
                    this.forumArticleAttachmentMapper.updateByFileId(integralUpdate, dbAttachment.getFileId());
                }
            }
        }
        // 文章是否需要审核
        if (isAdmin) {
            article.setStatus(ArticleStatusEnum.AUDIT.getStatus());
        } else {
            SysSetting4AuditDto auditDto = sysCacheUtils.getSysSetting().getAuditSetting();
            article.setStatus(auditDto.getPostAudit() ? ArticleStatusEnum.NO_AUDIT.getStatus() :
                    ArticleStatusEnum.AUDIT.getStatus());
        }

        //先对富文本正文做 XSS 白名单清洗，再处理图片路径
        String content = htmlSanitizer.clean(article.getContent());
        if (!StringUtils.isBlank(content)) {
            String month = imageUtils.resetImagePathInHtml(content);
            //避免替换博客中template关键，所以前后带上/
            String replaceMonth = "/" + month + "/";
            content = content.replace("/" + Constants.FILE_FOLDER_TEMP + "/", replaceMonth);
            article.setContent(content);
        }
        // markdown 正文独立处理图片路径（见 postArticle 同款说明）
        String markdownContent = article.getMarkdownContent();
        if (!StringUtils.isBlank(markdownContent)) {
            String mdMonth = imageUtils.resetImagePathInMarkdown(markdownContent);
            String mdReplaceMonth = "/" + mdMonth + "/";
            markdownContent = markdownContent.replace("/" + Constants.FILE_FOLDER_TEMP + "/", mdReplaceMonth);
            article.setMarkdownContent(markdownContent);
        }


        forumArticleMapper.updateByArticleId(article, article.getArticleId());
    }

    /**
     * 更新文章板块信息
     *
     * @param articleId 文章 ID
     * @param pBoardId  父板块 ID
     * @param boardId   板块 ID
     */
    @Override
    public void updateBoard(String articleId, Integer pBoardId, Integer boardId) {
        ForumArticle forumArticle = new ForumArticle();
        forumArticle.setPBoardId(pBoardId);
        forumArticle.setBoardId(boardId);
        checkBoardInfo(true, forumArticle);
        forumArticleMapper.updateByArticleId(forumArticle, articleId);
    }

    /**
     * 批量删除文章
     *
     * @param articleIds 文章 ID，逗号分隔
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delArticle(String articleIds) {
        String[] articleIdArray = articleIds.split(",");
        for (String articleId : articleIdArray) {
            forumArticleService.delArticleSingle(articleId);
        }
    }

    /**
     * 删除单个文章
     *
     * @param articleId 文章 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void delArticleSingle(String articleId) {
        ForumArticle article = forumArticleMapper.selectByArticleId(articleId);
        // 文章不存在或者已经被删除了，则不进行任何操作
        if (null == article || ArticleStatusEnum.DEL.getStatus().equals(article.getStatus())) {
            return;
        }
        ForumArticle updateInfo = new ForumArticle();
        updateInfo.setStatus(ArticleStatusEnum.DEL.getStatus());
        forumArticleMapper.updateByArticleId(updateInfo, articleId);

        // 没收发帖积分
        Integer integral = sysCacheUtils.getSysSetting().getPostSetting().getPostIntegral();
        if (integral > 0 && ArticleStatusEnum.AUDIT.getStatus().equals(article.getStatus())) {
            userInfoService.updateUserIntegral(article.getUserId(), UserIntegralOperTypeEnum.DEL_ARTICLE, UserIntegralChangeTypeEnum.REDUCE.getChangeType(),
                    integral);
        }
        // 发送系统消息
        UserMessage userMessage = new UserMessage();
        userMessage.setReceivedUserId(article.getUserId());
        userMessage.setMessageType(MessageTypeEnum.SYS.getType());
        userMessage.setCreateTime(new Date());
        userMessage.setStatus(MessageStatusEnum.NO_READ.getStatus());
        userMessage.setMessageContent("文章【" + article.getTitle() + "】被管理员删除");
        userMessageService.add(userMessage);
    }

    /**
     * 批量审核文章
     *
     * @param articleIds 文章 ID，逗号分隔
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditArticle(String articleIds) {
        String[] articleIdArray = articleIds.split(",");
        for (String articleId : articleIdArray) {
            forumArticleService.auditArticleSingle(articleId);
        }
    }

    /**
     * 审核单个文章
     *
     * @param articleId 文章 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void auditArticleSingle(String articleId) {
        ForumArticle article = getForumArticleByArticleId(articleId);
        // 文章不存在或者已经审核通过了，则不进行任何操作
        if (article == null || !ArticleStatusEnum.NO_AUDIT.getStatus().equals(article.getStatus())) {
            return;
        }
        ForumArticle updateInfo = new ForumArticle();
        updateInfo.setStatus(ArticleStatusEnum.AUDIT.getStatus());
        forumArticleMapper.updateByArticleId(updateInfo, articleId);
        // 发帖积分发放
        Integer integral = sysCacheUtils.getSysSetting().getPostSetting().getPostIntegral();
        if (integral > 0) {
            userInfoService.updateUserIntegral(article.getUserId(), UserIntegralOperTypeEnum.POST_ARTICLE, UserIntegralChangeTypeEnum.ADD.getChangeType(),
                    integral);
        }
    }
}
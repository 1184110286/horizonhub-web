package com.nlizzard.horizonhub.entity.vo.web;

import java.io.Serializable;

// 组合了文章信息和附件信息VO
public class FormArticleDetailVO implements Serializable {
    private ForumArticleVO forumArticle;

    private ForumArticleAttachmentVO attachment;

    private Boolean haveLike = false;

    public Boolean getHaveLike() {
        return haveLike;
    }

    public void setHaveLike(Boolean haveLike) {
        this.haveLike = haveLike;
    }

    public ForumArticleVO getForumArticle() {
        return forumArticle;
    }

    public void setForumArticle(ForumArticleVO forumArticle) {
        this.forumArticle = forumArticle;
    }

    public ForumArticleAttachmentVO getAttachment() {
        return attachment;
    }

    public void setAttachment(ForumArticleAttachmentVO attachment) {
        this.attachment = attachment;
    }

}

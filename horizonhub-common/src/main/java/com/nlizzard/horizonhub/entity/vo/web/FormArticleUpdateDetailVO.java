package com.nlizzard.horizonhub.entity.vo.web;

import com.nlizzard.horizonhub.entity.pojo.ForumArticle;

import java.io.Serializable;


public class FormArticleUpdateDetailVO implements Serializable {

    private ForumArticle forumArticle;

    private ForumArticleAttachmentVO attachment;

    public ForumArticle getForumArticle() {
        return forumArticle;
    }

    public void setForumArticle(ForumArticle forumArticle) {
        this.forumArticle = forumArticle;
    }

    public ForumArticleAttachmentVO getAttachment() {
        return attachment;
    }

    public void setAttachment(ForumArticleAttachmentVO attachment) {
        this.attachment = attachment;
    }
}

package com.nlizzard.horizonhub.entity.dto;

// 用户消息统计DTO
public class UserMessageCountDto {

    // 消息总数
    private Long total = 0L;
    // 系统消息数
    public Long sys = 0L;
    // 回复消息数
    public Long reply = 0L;
    // 文章被点赞消息数
    private Long likePost = 0L;
    // 评论被点赞消息数
    private Long likeComment = 0L;
    // 附件被下载消息数
    private Long downloadAttachment = 0L;

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Long getSys() {
        return sys;
    }

    public void setSys(Long sys) {
        this.sys = sys;
    }

    public Long getReply() {
        return reply;
    }

    public void setReply(Long reply) {
        this.reply = reply;
    }

    public Long getLikePost() {
        return likePost;
    }

    public void setLikePost(Long likePost) {
        this.likePost = likePost;
    }

    public Long getLikeComment() {
        return likeComment;
    }

    public void setLikeComment(Long likeComment) {
        this.likeComment = likeComment;
    }

    public Long getDownloadAttachment() {
        return downloadAttachment;
    }

    public void setDownloadAttachment(Long downloadAttachment) {
        this.downloadAttachment = downloadAttachment;
    }
}

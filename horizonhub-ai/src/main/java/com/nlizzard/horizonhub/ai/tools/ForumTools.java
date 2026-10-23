package com.nlizzard.horizonhub.ai.tools;

import com.nlizzard.horizonhub.entity.enums.ArticleStatusEnum;
import com.nlizzard.horizonhub.entity.enums.CommentStatusEnum;
import com.nlizzard.horizonhub.entity.enums.DateTimePatternEnum;
import com.nlizzard.horizonhub.entity.enums.PageSize;
import com.nlizzard.horizonhub.entity.pojo.ForumArticle;
import com.nlizzard.horizonhub.entity.pojo.ForumBoard;
import com.nlizzard.horizonhub.entity.pojo.ForumComment;
import com.nlizzard.horizonhub.entity.query.ForumArticleQuery;
import com.nlizzard.horizonhub.entity.query.ForumCommentQuery;
import com.nlizzard.horizonhub.service.ForumArticleService;
import com.nlizzard.horizonhub.service.ForumBoardService;
import com.nlizzard.horizonhub.service.ForumCommentService;
import com.nlizzard.horizonhub.utils.DateUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 论坛工具集：用 {@code @Tool} 把论坛查询能力暴露给大模型，由 LLM 根据用户意图自主决定
 * 调用哪个工具、传什么参数（Tool Calling / Function Calling）。
 * <p>
 * 这是 AI 从「被动接受预取上下文」升级为「主动检索业务数据」的关键：模型在对话中可自行
 * 搜索帖子、查详情、查板块、查评论，多步检索压缩成单轮命中。
 * <p>
 * 设计原则：
 * <ul>
 *   <li>只暴露查询类方法，绝不暴露写操作（发帖/评论/删除等），避免 LLM 越权修改数据；</li>
 *   <li>列表查询用 {@code findListByParam}（不取正文，省 token），详情查询才取正文并截断；</li>
 *   <li>返回紧凑文本（而非原始实体），信息密度高且不含敏感字段（IP、密码、邮箱等）。返回值供
 *       LLM 阅读而非直接回显给用户，由 LLM 据此组织自然语言回答。</li>
 * </ul>
 */
@Component
public class ForumTools {

    /** 列表查询返回条数上限，避免上下文过长 */
    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 10;

    /** 详情查询正文截断长度（字符） */
    private static final int DETAIL_CONTENT_MAX = 800;

    @Resource
    private ForumArticleService forumArticleService;

    @Resource
    private ForumBoardService forumBoardService;

    @Resource
    private ForumCommentService forumCommentService;

    /**
     * 按关键词搜索已审核的帖子（匹配标题）。
     */
    @Tool(description = "根据关键词搜索论坛已审核的帖子（按标题匹配），返回标题、所属板块、作者、阅读数、点赞数和摘要。当用户想找某个主题或关键词的帖子时调用。")
    public String searchArticles(
            @ToolParam(description = "搜索关键词，如某技术名词、帖子主题") String keyword,
            @ToolParam(description = "最多返回的帖子条数，可省略，默认5", required = false) Integer limit) {
        if (StringUtils.isBlank(keyword)) {
            return "关键词为空，无法搜索。";
        }
        int n = clamp(limit);
        ForumArticleQuery query = new ForumArticleQuery();
        query.setTitleFuzzy(keyword.trim());
        query.setStatus(ArticleStatusEnum.AUDIT.getStatus());
        query.setPageNo(1);
        query.setPageSize(PageSize.SIZE15.getSize());
        List<ForumArticle> list = forumArticleService.findListByParam(query);

        return formatArticleList("与「" + keyword.trim() + "」相关的帖子", subList(list, n));
    }

    /**
     * 获取热门帖子（按点赞、阅读数排序）。
     */
    @Tool(description = "获取论坛当前的帖子，按点赞数和阅读数从高到低排序。当用户想看热门内容、推荐帖子或没有明确关键词时调用。")
    public String getHotArticles(
            @ToolParam(description = "最多返回的帖子条数，可省略，默认5", required = false) Integer limit) {
        int n = clamp(limit);
        ForumArticleQuery query = new ForumArticleQuery();
        query.setStatus(ArticleStatusEnum.AUDIT.getStatus());
        // 热榜排序（白名单允许，见 BaseQuery.setOrderBy）
        query.setOrderBy("good_count desc,read_count desc");
        query.setPageNo(1);
        query.setPageSize(PageSize.SIZE15.getSize());
        List<ForumArticle> list = forumArticleService.findListByParam(query);

        return formatArticleList("当前帖子", subList(list, n));
    }

    /**
     * 查询某篇帖子的详情（含正文，截断控制长度）。
     * 用 getForumArticleByArticleId（不自增阅读数），避免污染统计。
     */
    @Tool(description = "根据帖子ID查询某篇帖子的详情，返回标题、板块、作者、发布时间、阅读/点赞/评论数、摘要和正文（过长会截断）。当用户想知道某篇帖子的具体内容时调用，调用前可先用搜索/热门工具拿到帖子ID。")
    public String getArticleDetail(
            @ToolParam(description = "帖子ID（articleId）") String articleId) {
        if (StringUtils.isBlank(articleId)) {
            return "帖子ID为空，无法查询。";
        }
        ForumArticle a = forumArticleService.getForumArticleByArticleId(articleId.trim());
        if (a == null) {
            return "未找到ID为 " + articleId + " 的帖子（可能不存在或未通过审核）。";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("《").append(safe(a.getTitle())).append("》\n");
        sb.append("板块：").append(boardPath(a)).append("\n");
        sb.append("作者：").append(safe(a.getNickName())).append("\n");
        sb.append("发布时间：").append(formatDate(a.getPostTime())).append("\n");
        sb.append("阅读：").append(num(a.getReadCount()))
          .append("，点赞：").append(num(a.getGoodCount()))
          .append("，评论：").append(num(a.getCommentCount())).append("\n");
        if (StringUtils.isNotBlank(a.getSummary())) {
            sb.append("摘要：").append(a.getSummary()).append("\n");
        }
        sb.append("正文：").append(extractContent(a));
        return sb.toString();
    }

    /**
     * 查询论坛板块结构。
     */
    @Tool(description = "获取论坛全部板块（含子板块）的名称与简介。当用户想了解论坛有哪些板块、某个话题属于哪个板块时调用。")
    public String getBoards() {
        List<ForumBoard> boards = forumBoardService.getBoardTree(null);
        if (boards == null || boards.isEmpty()) {
            return "论坛暂无板块。";
        }
        StringBuilder sb = new StringBuilder("论坛板块：\n");
        for (ForumBoard b : boards) {
            sb.append("- ").append(safe(b.getBoardName()));
            if (StringUtils.isNotBlank(b.getBoardDesc())) {
                sb.append("（").append(b.getBoardDesc()).append("）");
            }
            if (b.getChildren() != null && !b.getChildren().isEmpty()) {
                sb.append("，含子板块：");
                for (int i = 0; i < b.getChildren().size(); i++) {
                    if (i > 0) {
                        sb.append("、");
                    }
                    sb.append(safe(b.getChildren().get(i).getBoardName()));
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 查询某篇帖子的一级评论（含二级回复）。
     */
    @Tool(description = "根据帖子ID查询该帖子的评论列表，返回评论者、内容、点赞数。当用户想了解大家对某篇帖子的讨论时调用。")
    public String getArticleComments(
            @ToolParam(description = "帖子ID（articleId）") String articleId,
            @ToolParam(description = "最多返回的评论条数，可省略，默认5", required = false) Integer limit) {
        if (StringUtils.isBlank(articleId)) {
            return "帖子ID为空，无法查询评论。";
        }
        int n = clamp(limit);
        ForumCommentQuery query = new ForumCommentQuery();
        query.setArticleId(articleId.trim());
        query.setStatus(CommentStatusEnum.AUDIT.getStatus());
        query.setPCommentId(0); // 只取一级评论
        query.setLoadChildren(true); // 自动加载二级回复
        query.setPageNo(1);
        query.setPageSize(PageSize.SIZE15.getSize());
        List<ForumComment> list = forumCommentService.findListByParam(query);
        if (list == null || list.isEmpty()) {
            return "该帖子暂无评论。";
        }
        List<ForumComment> top = subList(list, n);
        StringBuilder sb = new StringBuilder("帖子评论：\n");
        for (ForumComment c : top) {
            sb.append("- ").append(safe(c.getNickName()));
            if (StringUtils.isNotBlank(c.getReplyNickName())) {
                sb.append(" 回复 @").append(c.getReplyNickName());
            }
            sb.append("：").append(truncate(safe(c.getContent()), 200));
            if (c.getGoodCount() != null && c.getGoodCount() > 0) {
                sb.append("（赞").append(c.getGoodCount()).append("）");
            }
            // 简要展示二级回复
            if (c.getChildren() != null && !c.getChildren().isEmpty()) {
                sb.append("\n  回复：").append(c.getChildren().size()).append("条");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    // ===== 格式化与工具方法 =====

    private String formatArticleList(String title, List<ForumArticle> list) {
        if (list == null || list.isEmpty()) {
            return title + "：暂无数据。";
        }
        StringBuilder sb = new StringBuilder(title).append("（共").append(list.size()).append("条）：\n");
        for (int i = 0; i < list.size(); i++) {
            ForumArticle a = list.get(i);
            sb.append(i + 1).append(". 《").append(safe(a.getTitle())).append("》")
              .append(" [ID:").append(safe(a.getArticleId())).append("]");
            if (StringUtils.isNotBlank(boardPath(a))) {
                sb.append(" 板块:").append(boardPath(a));
            }
            sb.append(" 作者:").append(safe(a.getNickName()))
              .append(" 阅读:").append(num(a.getReadCount()))
              .append(" 赞:").append(num(a.getGoodCount()))
              .append(" 评论:").append(num(a.getCommentCount()));
            if (StringUtils.isNotBlank(a.getSummary())) {
                sb.append("\n   摘要：").append(truncate(a.getSummary(), 80));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /** 提取帖子正文：优先 markdown 正文，其次摘要，并截断控制长度 */
    private String extractContent(ForumArticle a) {
        String md = a.getMarkdownContent();
        if (StringUtils.isNotBlank(md)) {
            return truncate(md, DETAIL_CONTENT_MAX);
        }
        if (StringUtils.isNotBlank(a.getSummary())) {
            return truncate(a.getSummary(), DETAIL_CONTENT_MAX);
        }
        return "（无正文）";
    }

    private String boardPath(ForumArticle a) {
        String p = safe(a.getPBoardName());
        String c = safe(a.getBoardName());
        if (StringUtils.isNotBlank(p) && StringUtils.isNotBlank(c) && !p.equals(c)) {
            return p + "/" + c;
        }
        return StringUtils.isNotBlank(p) ? p : c;
    }

    private int clamp(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private <T> List<T> subList(List<T> list, int limit) {
        if (list == null) {
            return List.of();
        }
        return list.size() > limit ? list.subList(0, limit) : list;
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String num(Integer n) {
        return n == null ? "0" : String.valueOf(n);
    }

    private String formatDate(java.util.Date d) {
        return d == null ? "未知" : DateUtils.format(d, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern());
    }
}

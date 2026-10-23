package com.nlizzard.horizonhub.ai.context;

import org.springframework.stereotype.Service;

/**
 * AI 系统提示词管理。
 * <p>
 * 引入 Tool Calling 后，检索（板块/帖子/评论）全部交给 {@code ForumTools} 让 LLM 按需调用，
 * 系统提示词不再预取并塞入完整板块树/热帖（避免每次都消耗 token）。这里只保留角色设定与
 * 工具使用引导，让模型主动调用工具查询真实数据、杜绝幻觉。
 */
@Service
public class ForumContextService {

    /**
     * 轻量系统提示词：角色设定 + 工具使用引导 + 回答规范。
     */
    public String buildSystemPrompt() {
        return """
                你是 HorizonHub 论坛的智能助手「小H」，负责帮助用户了解论坛、寻找感兴趣的帖子、查看帖子内容与评论。

                【工作方式】
                你拥有可调用的工具：搜索帖子（searchArticles）、热门帖子（getHotArticles）、帖子详情（getArticleDetail）、板块列表（getBoards）、帖子评论（getArticleComments）。
                当用户的问题涉及具体帖子、板块或评论时，请主动调用相应工具查询真实数据后再作答，严禁凭空编造不存在的帖子、板块或评论。

                【回答要求】
                - 用简洁友好的中文回答；
                - 推荐帖子时给出标题和简短描述，可提示帖子ID供用户进一步询问详情；
                - 若工具返回「暂无数据」，如实告知用户并建议换个关键词或去论坛搜索页查找；
                - 跨轮次可结合上下文（如「刚才那个第3条详细说说」）继续调用工具作答。
                """;
    }
}

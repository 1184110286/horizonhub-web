package com.nlizzard.horizonhub.ai.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * AI 对话服务：基于 Spring AI {@link ChatClient} 流式生成回答。
 * <p>
 * 通过 {@link ChatClient} 统一编排（装配见 {@code AiConfig}）：
 * <ul>
 *   <li>多轮记忆：{@code MessageChatMemoryAdvisor} 按 {@code conversationId} 隔离会话上下文，
 *       历史持久化于 Redis（{@code RedisChatMemoryRepository}）；</li>
 *   <li>工具调用：{@code ForumTools} 让 LLM 主动检索论坛数据；</li>
 *   <li>系统提示词：由 {@code ChatClient.defaultSystem} 设定。</li>
 * </ul>
 * <p>
 * 流式：WebMVC 下用 {@link SseEmitter} 订阅 {@code ChatClient.stream().content()} 返回的
 * {@code Flux<String>}，逐段推送给前端。
 */
@Service
public class AiChatService {

    private static final Logger logger = LoggerFactory.getLogger(AiChatService.class);

    /** SSE 超时（LLM 生成 + 工具调用可能较慢，给 120 秒） */
    private static final long SSE_TIMEOUT = 120_000L;

    /** 缺失 conversationId 时使用的回退会话 ID（仍可对话，但记忆无法跨轮累积） */
    private static final String DEFAULT_CONVERSATION_ID = "anonymous";

    /**
     * ChatClient 可选注入：未配置 AI_API_KEY 时为 null（AiConfig 不装配该 bean），
     * 此时 AI 接口返回「未配置」提示，系统其余功能不受影响。
     */
    @Autowired(required = false)
    private ChatClient chatClient;

    /**
     * 流式对话：返回 SseEmitter，逐段推送 LLM 输出。
     *
     * @param userMessage     用户提问
     * @param conversationId  会话 ID，用于隔离多轮记忆（缺失则回退默认值）
     * @return SSE 流
     */
    public SseEmitter streamChat(String userMessage, String conversationId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 未配置 ChatClient（缺 AI_API_KEY）：通过 SSE 返回提示而非抛异常，前端友好展示
        if (chatClient == null) {
            sendError(emitter, "AI 助手未配置（缺少 AI_API_KEY）");
            emitter.complete();
            return emitter;
        }

        String convId = (conversationId == null || conversationId.isBlank())
                ? DEFAULT_CONVERSATION_ID : conversationId.trim();

        // ChatClient 流式调用：记忆按 conversationId 隔离，工具由 LLM 自主编排。
        // .stream().content() 返回 Flux<String>，框架已过滤仅含元数据的分帧，文本片段非 null。
        chatClient.prompt()
                .user(userMessage)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, convId))
                .stream()
                .content()
                .subscribe(
                        chunk -> {
                            if (chunk == null || chunk.isEmpty()) {
                                return;
                            }
                            try {
                                emitter.send(SseEmitter.event().data(chunk));
                            } catch (IOException e) {
                                logger.warn("SSE 发送失败：{}", e.getMessage());
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            logger.error("AI 流式生成失败", error);
                            sendError(emitter, "AI 回复出错，请稍后重试");
                            emitter.completeWithError(error);
                        },
                        emitter::complete
                );

        // 客户端断开或超时时的兜底
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> logger.warn("SSE 连接异常：{}", e.getMessage()));

        return emitter;
    }

    private void sendError(SseEmitter emitter, String msg) {
        try {
            emitter.send(SseEmitter.event().data("[ERROR] " + msg));
        } catch (IOException ignored) {
        }
    }
}

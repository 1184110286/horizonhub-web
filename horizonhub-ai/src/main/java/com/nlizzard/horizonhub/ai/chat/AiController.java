package com.nlizzard.horizonhub.ai.chat;

import com.nlizzard.horizonhub.ai.ratelimit.AiRateLimiter;
import com.nlizzard.horizonhub.basecontroller.BaseController;
import com.nlizzard.horizonhub.entity.dto.SessionWebUserDto;
import com.nlizzard.horizonhub.entity.vo.ResponseVO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * AI 助手接口。
 * <p>
 * 不强制登录：未登录也可用（游客可咨询论坛内容）；登录则问候个性化。
 * 登录态由 {@link BaseController#getUserInfoFromSession} 获取（兼容 Session 与 Token 双轨）。
 */
@RestController
@RequestMapping("/ai")
public class AiController extends BaseController {

    @Resource
    private AiChatService aiChatService;

    @Resource
    private AiRateLimiter aiRateLimiter;

    /**
     * 检测当前登录态，供前端决定是否显示登录引导。
     * 走 web 的 getUserInfoFromSession（已兼容 Token）。
     */
    @RequestMapping("/status")
    public ResponseVO<SessionWebUserDto> status(HttpSession session) {
        return getSuccessResponseVO(getUserInfoFromSession(session));
    }

    /**
     * 流式对话（SSE）。前端用 EventSource 监听。
     * <p>
     * 接入限流：登录用户按 userId 计数、游客按 IP 计数（双窗口），超限则通过 SSE 推送
     * {@code [ERROR]} 提示而非直接抛异常——兼容前端现有的错误解析逻辑。鉴权沿用「游客可用、
     * 登录更佳」的既有策略（EventSource 无法携带 Token，故不强制登录；限流即游客的访问控制）。
     *
     * @param message        用户提问
     * @param conversationId 会话 ID，用于隔离多轮对话记忆（前端生成 UUID 持久化于 localStorage）
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(HttpSession session,
                           HttpServletRequest request,
                           @RequestParam("message") String message,
                           @RequestParam(value = "conversationId", required = false) String conversationId) {
        // 登录态仅用于个性化与限流标识（非强制）；未登录 userDto 为 null，AI 正常回答
        SessionWebUserDto userDto = getUserInfoFromSession(session);

        // 限流校验：登录按 userId、游客按 IP，超限直接以 SSE 提示返回，不消耗 LLM 调用
        String userId = (userDto != null) ? userDto.getUserId() : null;
        AiRateLimiter.RateCheckResult rateCheck = aiRateLimiter.check(userId, getIpAddr(request));
        if (!rateCheck.isAllowed()) {
            return rateLimitedEmitter(rateCheck.getReason());
        }

        String finalMessage = (userId != null)
                ? ("（当前用户：" + userDto.getNickName() + "）" + message)
                : message;
        return aiChatService.streamChat(finalMessage, conversationId);
    }

    /**
     * 限流被拒时：构造一个立即完成的 SSE，推送 [ERROR] 提示，前端按现有逻辑解析展示。
     */
    private SseEmitter rateLimitedEmitter(String reason) {
        SseEmitter emitter = new SseEmitter(5_000L);
        try {
            emitter.send(SseEmitter.event().data("[ERROR] " + reason));
        } catch (IOException e) {
            // 忽略：客户端可能已断开
        }
        emitter.complete();
        return emitter;
    }
}

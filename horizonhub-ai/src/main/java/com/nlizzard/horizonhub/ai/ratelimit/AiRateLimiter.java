package com.nlizzard.horizonhub.ai.ratelimit;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI 接口限流器：基于 Redis Lua 原子「固定窗口计数」算法，防止 AI 接口被恶意刷量导致
 * LLM 调用成本失控。
 * <p>
 * 不复用 {@code FrequencyLimitAspect}：那个切面（1）强制要求登录、（2）key 只用 userId 不支持
 * 游客 IP、（3）靠返回值是 {@code ResponseVO} 判断成功来回滚，而 AI 接口允许游客、返回
 * {@code SseEmitter}（异步流），三者都不兼容。故为 AI 单独实现限流。
 * <p>
 * 设计要点：
 * <ul>
 *   <li><b>双标识</b>：登录用户按 userId 计数（额度高，可封号）；游客按客户端 IP 计数（额度低，
 *       抑制匿名刷量）。</li>
 *   <li><b>双窗口</b>：分钟级窗口防「短时突发」、天级窗口防「长期总量」。任一窗口超限即拒绝。</li>
 *   <li><b>原子性</b>：用单条 Lua 脚本完成「自增 + 首次设置过期 + 判定」，避免并发下检查-自增
 *       之间的竞态穿透。</li>
 *   <li><b>容错</b>：Redis 不可用时降级为「放行」（不因限流组件故障拖垮 AI 主流程），仅记日志。</li>
 * </ul>
 */
@Component
public class AiRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(AiRateLimiter.class);

    private static final String KEY_PREFIX = "horizonHub:ai:rate:";

    /** 登录用户标识前缀 */
    private static final String SCOPE_USER = "user:";
    /** 游客标识前缀 */
    private static final String SCOPE_IP = "ip:";
    /** 分钟级窗口 */
    private static final String WIN_MINUTE = "min";
    /** 天级窗口 */
    private static final String WIN_DAY = "day";

    /**
     * 原子「自增 + 首次设过期 + 判定」固定窗口脚本。
     * <p>ARGV[1]=上限，ARGV[2]=窗口秒数。返回：-1 表示超限；否则返回自增后的当前计数。</p>
     */
    private static final DefaultRedisScript<Long> FIXED_WINDOW_SCRIPT;

    static {
        FIXED_WINDOW_SCRIPT = new DefaultRedisScript<>();
        FIXED_WINDOW_SCRIPT.setResultType(Long.class);
        FIXED_WINDOW_SCRIPT.setScriptText(
                "local current = redis.call('INCR', KEYS[1]) \n" +
                        "if current == 1 then \n" +
                        "    redis.call('EXPIRE', KEYS[1], ARGV[2]) \n" +
                        "end \n" +
                        "if current > tonumber(ARGV[1]) then \n" +
                        "    return -1 \n" +
                        "end \n" +
                        "return current"
        );
    }

    /** 登录用户：分钟级上限 */
    @Value("${spring.ai.rate-limit.user.per-minute:10}")
    private int userPerMinute;

    /** 登录用户：天级上限 */
    @Value("${spring.ai.rate-limit.user.per-day:100}")
    private int userPerDay;

    /** 游客：分钟级上限（更严格） */
    @Value("${spring.ai.rate-limit.guest.per-minute:5}")
    private int guestPerMinute;

    /** 游客：天级上限（更严格） */
    @Value("${spring.ai.rate-limit.guest.per-day:20}")
    private int guestPerDay;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 校验是否允许调用。登录用户按 userId、游客按 ip。
     *
     * @param userId 登录用户 ID，null 表示游客
     * @param ip     客户端 IP（游客或兜底标识）
     * @return 校验结果
     */
    public RateCheckResult check(String userId, String ip) {
        boolean isLogin = userId != null && !userId.isBlank();
        String scope = isLogin ? SCOPE_USER : SCOPE_IP;
        String identity = isLogin ? userId.trim() : (ip == null || ip.isBlank() ? "unknown" : ip.trim());

        int perMinute = isLogin ? userPerMinute : guestPerMinute;
        int perDay = isLogin ? userPerDay : guestPerDay;

        // 分钟窗口先判：最常触发的瓶颈放前面
        Long minuteResult = tryAcquire(buildKey(scope, identity, WIN_MINUTE), perMinute, 60);
        if (minuteResult == null) {
            // Redis 故障：降级放行
            return RateCheckResult.allowed();
        }
        if (minuteResult == -1L) {
            return RateCheckResult.rejected("提问过于频繁，请每分钟内不超过 " + perMinute + " 次");
        }
        // 分钟窗口通过后再判天级
        Long dayResult = tryAcquire(buildKey(scope, identity, WIN_DAY), perDay, 24 * 60 * 60);
        if (dayResult == null) {
            return RateCheckResult.allowed();
        }
        if (dayResult == -1L) {
            return RateCheckResult.rejected("今日提问次数已达上限（" + perDay + " 次），请明天再来");
        }
        return RateCheckResult.allowed();
    }

    /**
     * 执行一次窗口的原子计数。返回 null 表示 Redis 故障（降级放行）。
     */
    private Long tryAcquire(String key, int limit, int windowSeconds) {
        try {
            return stringRedisTemplate.execute(
                    FIXED_WINDOW_SCRIPT,
                    List.of(key),
                    String.valueOf(limit), String.valueOf(windowSeconds)
            );
        } catch (DataAccessException e) {
            logger.warn("AI 限流 Redis 故障，降级放行：{}", e.getMessage());
            return null;
        }
    }

    private String buildKey(String scope, String identity, String window) {
        return KEY_PREFIX + window + ":" + scope + identity;
    }

    /**
     * 限流校验结果。
     */
    public static class RateCheckResult {
        private final boolean allowed;
        private final String reason;

        private RateCheckResult(boolean allowed, String reason) {
            this.allowed = allowed;
            this.reason = reason;
        }

        static RateCheckResult allowed() {
            return new RateCheckResult(true, null);
        }

        static RateCheckResult rejected(String reason) {
            return new RateCheckResult(false, reason);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getReason() {
            return reason;
        }
    }
}

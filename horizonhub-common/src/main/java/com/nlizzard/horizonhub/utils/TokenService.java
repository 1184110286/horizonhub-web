package com.nlizzard.horizonhub.utils;

import cn.hutool.core.util.IdUtil;
import com.nlizzard.horizonhub.constants.Constants;
import com.nlizzard.horizonhub.entity.config.TokenConfig;
import com.nlizzard.horizonhub.entity.dto.LoginUserContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Token 服务：JWT 外壳 + Redis 登录态（有状态 Token）。
 * <p>
 * 设计要点：
 * <ul>
 *     <li>JWT 仅承载 {@code tokenId}（subject）与 {@code scope}，并用 HMAC-SHA256 签名防伪造。</li>
 *     <li>真正的登录态（{@link LoginUserContext}）以 JSON 存入 Redis，key 为 {@code horizonHub:token:{tokenId}}，
 *         带过期时间。</li>
 *     <li>校验 = 验签 + 查 Redis。Redis 无记录（过期 / 已吊销）即视为失效，从而支持「主动吊销」「强制下线」。</li>
 * </ul>
 * 这是有状态 Token：相比纯 JWT 自验证牺牲了「无状态」，换来可控性与可吊销性，
 * 适合本系统「能管理登录态、AI 代理可被随时封禁」的需求。
 */
@Component
public class TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    @Resource
    private TokenConfig tokenConfig;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] secretBytes = tokenConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            // fail-fast：密钥不足 32 字节直接启动失败（dev yml 已提供默认 55 字节密钥，prod 用 TOKEN_SECRET 注入）
            throw new IllegalStateException("token.secret 必须至少 32 字节（HS256 要求）；生产环境请通过 TOKEN_SECRET 注入强随机密钥");
        }
        this.secretKey = Keys.hmacShaKeyFor(secretBytes);
    }

    private String redisKey(String tokenId) {
        return Constants.TOKEN_REDIS_KEY_PREFIX + tokenId;
    }

    /**
     * 签发 Token：生成 tokenId，把登录上下文写入 Redis，返回 JWT。
     *
     * @param context 登录上下文（tokenId 由本方法填充）
     * @return JWT 字符串
     */
    public String createToken(LoginUserContext context) {
        String tokenId = IdUtil.simpleUUID();
        context.setTokenId(tokenId);

        long ttlSeconds = tokenConfig.getTtlMinutes() * 60L;
        stringRedisTemplate.opsForValue().set(redisKey(tokenId), JsonUtils.object2Json(context), ttlSeconds, TimeUnit.SECONDS);

        Date now = new Date();
        Date expiration = new Date(now.getTime() + ttlSeconds * 1000L);
        return Jwts.builder()
                .issuer(tokenConfig.getIssuer())
                .subject(tokenId)
                .claim("scope", context.getScope() != null ? context.getScope().name() : null)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析并校验 Token：验签 + 查 Redis。无效或已失效返回 null。
     */
    public LoginUserContext parseToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
            String tokenId = claims.getSubject();
            if (tokenId == null) {
                return null;
            }
            String json = stringRedisTemplate.opsForValue().get(redisKey(tokenId));
            if (json == null) {
                // Redis 无记录 = 过期 / 已吊销
                return null;
            }
            return JsonUtils.json2Object(json, LoginUserContext.class);
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Token 解析失败：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 续期 Token 对应的 Redis 登录态（滑动过期）。
     */
    public void renew(String token) {
        String tokenId = extractTokenId(token);
        if (tokenId != null) {
            stringRedisTemplate.expire(redisKey(tokenId), tokenConfig.getTtlMinutes(), TimeUnit.MINUTES);
        }
    }

    /**
     * 吊销 Token：删除 Redis 登录态（立即失效，用于 logout / 强制下线）。
     */
    public void invalidate(String token) {
        String tokenId = extractTokenId(token);
        if (tokenId != null) {
            stringRedisTemplate.delete(redisKey(tokenId));
        }
    }

    private String extractTokenId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("提取 tokenId 失败：{}", e.getMessage());
            return null;
        }
    }
}

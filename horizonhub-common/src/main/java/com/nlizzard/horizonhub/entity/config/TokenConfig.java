package com.nlizzard.horizonhub.entity.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Token（JWT + Redis 登录态）配置。
 * <p>
 * JWT 仅作为「载 tokenId + 防伪造」的令牌外壳，真正的登录态存于 Redis；
 * 密钥、有效期、签发者走 {@code ${ENV}} 外置，dev 提供默认值、prod 不给默认（fail-fast）。
 */
@Component
public class TokenConfig {

    /**
     * HMAC-SHA 签名密钥（明文），需 >= 32 字节（HS256 要求）。
     * 生产环境务必通过环境变量 {@code TOKEN_SECRET} 注入强随机值。
     */
    @Value("${token.secret:}")
    private String secret;

    /**
     * Token / 登录态有效期，单位分钟，默认 7 天（10080）。
     */
    @Value("${token.ttl-minutes:10080}")
    private long ttlMinutes;

    /**
     * 签发者（iss 声明），默认 horizonhub。
     */
    @Value("${token.issuer:horizonhub}")
    private String issuer;

    public String getSecret() {
        return secret;
    }

    public long getTtlMinutes() {
        return ttlMinutes;
    }

    public String getIssuer() {
        return issuer;
    }
}

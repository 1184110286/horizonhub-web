package com.nlizzard.horizonhub.utils;

import cn.hutool.crypto.SecureUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码加密与校验工具。
 * <p>
 * 新口令统一使用 BCrypt（自带盐、强度可调）。为兼容历史数据，{@link #matches(String, String)}
 * 同时识别旧版无盐 MD5 口令（32 位十六进制），并在 {@link #isLegacyMd5(String)} 为真时，
 * 由调用方在登录成功后用 {@link #encode(String)} 惰性升级为 BCrypt。
 * <p>
 * 前端应直接发送明文口令（必须走 HTTPS），哈希只在服务端进行。
 */
@Component
public class PasswordUtils {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 是否为旧版无盐 MD5（32 位十六进制）。BCrypt 摘要以 {@code $2} 开头，长度约 60，不会误判。
     */
    public boolean isLegacyMd5(String stored) {
        return stored != null && stored.length() == 32 && stored.matches("[0-9a-fA-F]{32}");
    }

    /**
     * 将明文口令加密为 BCrypt 摘要。
     */
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * 校验明文口令：支持 BCrypt 摘要，兼容旧版无盐 MD5（用于惰性升级场景）。
     */
    public boolean matches(String rawPassword, String storedPassword) {
        if (storedPassword == null || storedPassword.isEmpty()) {
            return false;
        }
        if (isLegacyMd5(storedPassword)) {
            return SecureUtil.md5(rawPassword).equals(storedPassword);
        }
        return encoder.matches(rawPassword, storedPassword);
    }
}

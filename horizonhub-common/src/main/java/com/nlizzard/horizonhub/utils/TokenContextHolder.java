package com.nlizzard.horizonhub.utils;

import com.nlizzard.horizonhub.entity.dto.LoginUserContext;

/**
 * Token 认证上下文持有者（请求级 ThreadLocal）。
 * <p>
 * Token 拦截器在 preHandle 解析出 {@link LoginUserContext} 后存入此处，
 * 业务代码（Service / 切面）通过 {@link #get()} 取当前用户；请求结束时由拦截器
 * {@link #clear()} 清理，避免线程复用导致的上下文泄漏。
 * <p>
 * 与 {@code Session} 路径并存：取当前用户处优先读本持有者，为空再回落 session。
 */
public final class TokenContextHolder {

    private static final ThreadLocal<LoginUserContext> HOLDER = new ThreadLocal<>();

    private TokenContextHolder() {
    }

    public static void set(LoginUserContext context) {
        HOLDER.set(context);
    }

    public static LoginUserContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}

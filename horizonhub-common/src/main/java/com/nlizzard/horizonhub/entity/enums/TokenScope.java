package com.nlizzard.horizonhub.entity.enums;

/**
 * Token 作用域，区分不同调用方。
 * <ul>
 *     <li>{@link #WEB} —— 前台用户（浏览器 / 移动端用 Token 时）</li>
 *     <li>{@link #ADMIN} —— 后台管理员</li>
 *     <li>{@link #AI_AGENT} —— AI 代理（后续阶段接入，可做更细的 scope 限制）</li>
 * </ul>
 */
public enum TokenScope {
    WEB,
    ADMIN,
    AI_AGENT
}

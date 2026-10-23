package com.nlizzard.horizonhub.entity.dto;

import com.nlizzard.horizonhub.entity.enums.TokenScope;

/**
 * 统一登录上下文（Token 路径产出）。
 * <p>
 * 与基于 {@code HttpSession} 的 {@link SessionWebUserDto} / {@link SessionAdminUserDto}
 * 并存：Token 路径（AI / 第三方 / 移动端）认证后产出本对象并存入请求上下文
 * {@code TokenContextHolder}，业务代码可统一从中取当前用户。
 */
public class LoginUserContext {

    /** Token 唯一标识，即 Redis 中登录态的 key 后缀 */
    private String tokenId;

    /** 用户 ID（web 用户） */
    private String userId;

    /** 昵称（web 用户） */
    private String nickName;

    /** 账号（admin） */
    private String account;

    /** 是否管理员 */
    private Boolean isAdmin;

    /** 调用方作用域 */
    private TokenScope scope;

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public Boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public TokenScope getScope() {
        return scope;
    }

    public void setScope(TokenScope scope) {
        this.scope = scope;
    }

    public boolean isAdmin() {
        return isAdmin != null && isAdmin;
    }
}

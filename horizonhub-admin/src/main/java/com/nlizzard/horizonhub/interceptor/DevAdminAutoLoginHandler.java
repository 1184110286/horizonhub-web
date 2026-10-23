package com.nlizzard.horizonhub.interceptor;

import com.nlizzard.horizonhub.constants.Constants;
import com.nlizzard.horizonhub.entity.dto.SessionAdminUserDto;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 开发环境免登录自动登录处理器（仅 dev profile 生效）。
 * <p>
 * 用 {@code @Profile("dev")} 在 Bean 层面物理隔离：生产环境（未激活 dev profile）
 * 该 Bean 不会被创建，{@link AppInterceptor} 注入得到 {@code null}，自动登录分支不可达，
 * 杜绝仅靠 yml 一个布尔值 {@code isDev.open=true} 误开造成的无密码管理员后门。
 * <p>
 * 启用方式：以 {@code --spring.profiles.active=dev} 启动。
 */
@Component
@Profile("dev")
public class DevAdminAutoLoginHandler {

    /**
     * 创建开发用管理员会话，写入 session 并返回该 DTO。
     */
    public SessionAdminUserDto createDevSession(HttpSession session) {
        SessionAdminUserDto sessionUser = new SessionAdminUserDto();
        sessionUser.setAccount("管理员");
        session.setAttribute(Constants.SESSION_KEY, sessionUser);
        return sessionUser;
    }
}

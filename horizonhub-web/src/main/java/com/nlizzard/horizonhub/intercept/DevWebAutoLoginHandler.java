package com.nlizzard.horizonhub.intercept;

import cn.hutool.core.util.IdUtil;
import com.nlizzard.horizonhub.constants.Constants;
import com.nlizzard.horizonhub.entity.config.WebConfig;
import com.nlizzard.horizonhub.entity.dto.SessionWebUserDto;
import com.nlizzard.horizonhub.entity.enums.UserStatusEnum;
import com.nlizzard.horizonhub.entity.pojo.UserInfo;
import com.nlizzard.horizonhub.entity.query.UserInfoQuery;
import com.nlizzard.horizonhub.service.UserInfoService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 开发环境免登录自动登录处理器（仅 dev profile 生效）。
 * <p>
 * 用 {@code @Profile("dev")} 在 Bean 层面物理隔离：生产环境（未激活 dev profile）
 * 该 Bean 不会被创建，{@link LoginInterceptor} 注入得到 {@code null}，自动登录分支不可达，
 * 从而杜绝仅靠 yml 一个布尔值 {@code isDev.open=true} 误开造成的无密码管理员后门。
 * <p>
 * 启用方式：以 {@code --spring.profiles.active=dev} 启动，并在配置中提供
 * {@code isDev.testUserEmail}。
 */
@Component
@Profile("dev")
public class DevWebAutoLoginHandler {

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private WebConfig webConfig;

    /**
     * 创建开发用测试会话：若不存在测试账号则自动创建（管理员权限），写入 session 后返回 true。
     */
    public boolean createDevSession(HttpSession session) {
        UserInfoQuery userInfoQuery = new UserInfoQuery();
        userInfoQuery.setEmail(webConfig.getDevTestEmail());
        List<UserInfo> userInfoList = userInfoService.findListByParam(userInfoQuery);
        UserInfo testUser;
        if (userInfoList.isEmpty()) {
            // 没有则生成测试用户
            testUser = new UserInfo();
            testUser.setCurrentIntegral(10000);
            testUser.setUserId(IdUtil.getSnowflakeNextIdStr());
            testUser.setEmail(webConfig.getDevTestEmail());
            testUser.setStatus(UserStatusEnum.ENABLE.getStatus());
            testUser.setNickName("test");
            userInfoService.add(testUser);
        } else {
            testUser = userInfoList.get(0);
        }
        SessionWebUserDto sessionUser = new SessionWebUserDto();
        sessionUser.setUserId(testUser.getUserId());
        sessionUser.setNickName(testUser.getNickName());
        sessionUser.setProvince("中国");
        sessionUser.setAdmin(true);
        session.setAttribute(Constants.SESSION_KEY, sessionUser);
        return true;
    }
}

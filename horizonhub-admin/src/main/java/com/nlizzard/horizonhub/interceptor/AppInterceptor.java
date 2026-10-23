package com.nlizzard.horizonhub.interceptor;


import com.nlizzard.horizonhub.constants.Constants;
import com.nlizzard.horizonhub.entity.dto.LoginUserContext;
import com.nlizzard.horizonhub.entity.dto.SessionAdminUserDto;
import com.nlizzard.horizonhub.entity.enums.ResponseCodeEnum;
import com.nlizzard.horizonhub.entity.enums.TokenScope;
import com.nlizzard.horizonhub.exception.BusinessException;
import com.nlizzard.horizonhub.utils.TokenContextHolder;
import com.nlizzard.horizonhub.utils.TokenService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Set;


/**
 * 后台登录校验拦截器（双轨：Token 优先，回落 Session）。
 * <p>
 * <ul>
 *     <li>Token 路径：仅接受 ADMIN scope 的 Token（前台 WEB/AI_AGENT token 不可串用后台）。</li>
 *     <li>Session 路径：无 Token 或 Token 无效时回落原有 Session 认证。</li>
 * </ul>
 */
// 拦截器校验登录
@Component
public class AppInterceptor implements HandlerInterceptor {

    /**
     * 免登录白名单：用 servletPath（不含 context-path）精确匹配，避免子串匹配带来的绕过风险。
     */
    private static final Set<String> WHITELIST = Set.of(
            "/checkCode",
            "/login",
            "/tokenLogin"
    );

    /**
     * 开发环境自动登录处理器。仅 dev profile 下存在该 Bean；生产环境为 null，
     * 因此 isDev.open 之类的布尔开关即便被误配也无法触发免登录后门。
     */
    @Autowired(required = false)
    private DevAdminAutoLoginHandler devAdminAutoLoginHandler;

    @Resource
    private TokenService tokenService;

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        // 全局拦截，白名单（精确匹配）放行，避免方法未设置拦截器导致权限/日志缺失
        if (WHITELIST.contains(request.getServletPath())) {
            return true;
        }

        // 1. 优先 Token 认证（仅 ADMIN scope）
        String token = extractToken(request);
        if (token != null) {
            LoginUserContext context = tokenService.parseToken(token);
            if (context != null && TokenScope.ADMIN.equals(context.getScope())) {
                TokenContextHolder.set(context);
                tokenService.renew(token);
                return true;
            }
            // Token 无效或非后台 scope：继续回落 Session
        }

        // 2. 回落 Session 认证
        checkLogin();
        return true;
    }

    /**
     * 校验登录状态（Session 路径）
     */
    private void checkLogin() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession();
        SessionAdminUserDto sessionUser = (SessionAdminUserDto) session.getAttribute(Constants.SESSION_KEY);
        // 仅 dev profile 下 Bean 存在；生产环境 devAdminAutoLoginHandler == null，此分支不可达
        if (sessionUser == null && devAdminAutoLoginHandler != null) {
            sessionUser = devAdminAutoLoginHandler.createDevSession(session);
        }
        if (null == sessionUser) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
    }

    /**
     * 从 Authorization 头提取 Bearer Token。
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(Constants.TOKEN_HEADER);
        if (header != null && header.startsWith(Constants.TOKEN_PREFIX)) {
            return header.substring(Constants.TOKEN_PREFIX.length()).trim();
        }
        return null;
    }

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, Exception ex) {
        // 清理请求级上下文，避免线程复用导致的上下文泄漏
        TokenContextHolder.clear();
    }

    @Override
    public void postHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }
}

package com.nlizzard.horizonhub.intercept;

import com.nlizzard.horizonhub.annotation.GlobalInterceptor;
import com.nlizzard.horizonhub.constants.Constants;
import com.nlizzard.horizonhub.entity.dto.LoginUserContext;
import com.nlizzard.horizonhub.entity.dto.SessionWebUserDto;
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
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.nlizzard.horizonhub.constants.Constants.SESSION_KEY;

/**
 * 登录校验拦截器（双轨：Token 优先，回落 Session）。
 * <p>
 * <ul>
 *     <li>Token 路径：请求带 {@code Authorization: Bearer <token>}，解析成功且 scope 非 ADMIN
 *         （前台接受 WEB / AI_AGENT）→ 存入 {@link TokenContextHolder}、滑动续期、放行。</li>
 *     <li>Session 路径：无 Token 或 Token 无效时，回落原有 Session cookie 认证（现有前端零改动）。</li>
 *     <li>两者都不满足：dev profile 下自动登录，否则抛 CODE_901。</li>
 * </ul>
 */
// 登录校验
@Component
public class LoginInterceptor implements HandlerInterceptor {

    /**
     * 开发环境自动登录处理器。仅 dev profile 下存在该 Bean；生产环境为 null，
     * 因此 isDev.open 之类的布尔开关即便被误配也无法触发免登录后门。
     */
    @Autowired(required = false)
    private DevWebAutoLoginHandler devWebAutoLoginHandler;

    @Resource
    private TokenService tokenService;

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {
        // 访问静态资源，直接放行。
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        // 没有全局校验注解直接放行
        boolean hasGlobalInterceptor = method.hasMethodAnnotation(GlobalInterceptor.class) || method.getBeanType().isAnnotationPresent(GlobalInterceptor.class);
        if (!hasGlobalInterceptor) {
            return true;
        }
        GlobalInterceptor methodAnnotation = method.getMethodAnnotation(GlobalInterceptor.class);
        // 无需校验登录，放行
        if (methodAnnotation == null || !methodAnnotation.checkLogin()) {
            return true;
        }

        // 1. 优先 Token 认证（AI / 第三方 / 移动端）
        String token = extractToken(request);
        if (token != null) {
            LoginUserContext context = tokenService.parseToken(token);
            // 前台接受 WEB / AI_AGENT scope，拒绝后台 ADMIN token 串用
            if (context != null && !TokenScope.ADMIN.equals(context.getScope())) {
                TokenContextHolder.set(context);
                tokenService.renew(token);
                return true;
            }
            // Token 无效或 scope 不符：继续回落 Session
        }

        // 2. 回落 Session 认证（现有前端 cookie）
        HttpSession session = request.getSession();
        SessionWebUserDto sessionUser = (SessionWebUserDto) session.getAttribute(SESSION_KEY);
        if (sessionUser != null) {
            return true;
        }

        // 3. 仅 dev profile 下 Bean 存在；生产环境 devWebAutoLoginHandler == null，此分支不可达
        if (devWebAutoLoginHandler != null) {
            return devWebAutoLoginHandler.createDevSession(session);
        }

        throw new BusinessException(ResponseCodeEnum.CODE_901);
    }

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, Exception ex) {
        // 清理请求级上下文，避免线程复用导致的上下文泄漏
        TokenContextHolder.clear();
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
}

package com.nlizzard.horizonhub.controller;

import com.nlizzard.horizonhub.annotation.GlobalInterceptor;
import com.nlizzard.horizonhub.annotation.VerifyParam;
import com.nlizzard.horizonhub.basecontroller.BaseController;
import com.nlizzard.horizonhub.constants.Constants;
import com.nlizzard.horizonhub.entity.config.AdminConfig;
import com.nlizzard.horizonhub.entity.dto.CreateImageCode;
import com.nlizzard.horizonhub.entity.dto.LoginUserContext;
import com.nlizzard.horizonhub.entity.dto.SessionAdminUserDto;
import com.nlizzard.horizonhub.entity.enums.TokenScope;
import com.nlizzard.horizonhub.entity.vo.ResponseVO;
import com.nlizzard.horizonhub.exception.BusinessException;
import com.nlizzard.horizonhub.utils.TokenService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class AccountController extends BaseController {
    @Resource
    private AdminConfig adminConfig;

    @Resource
    private TokenService tokenService;

    /**
     * 生成图片验证码
     *
     */
    @RequestMapping(value = "/checkCode")
    public void checkCode(HttpServletResponse response, HttpSession session) throws IOException {
        // 创建图片验证码对象
        CreateImageCode codeEntity = new CreateImageCode(130, 38, 5, 10);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpeg");
        // 将验证码存入session
        String code = codeEntity.getCode();
        session.setAttribute(Constants.CHECK_CODE_KEY, code);
        codeEntity.write(response.getOutputStream());
    }

    /**
     * 登录接口
     *
     * @param account   账号名
     * @param password  密码
     * @param checkCode 图片验证码
     */
    @PostMapping("/login")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<SessionAdminUserDto> login(HttpSession session,
                                                 @VerifyParam(required = true) String account,
                                                 @VerifyParam(required = true) String password,
                                                 @VerifyParam(required = true) String checkCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("图片验证码不正确");
            }

            // 前端发送明文口令（必须走 HTTPS），与配置中的后台口令直接比对；
            // 生产环境口令由环境变量 ADMIN_PASSWORD 注入。
            if (!adminConfig.getAdminAccount().equals(account) || !adminConfig.getAdminPassword().equals(password)) {
                throw new BusinessException("账号或密码错误");
            }
            // 登录成功，创建session
            SessionAdminUserDto sessionAdminUserDto = new SessionAdminUserDto();
            sessionAdminUserDto.setAccount(account);
            session.setAttribute(Constants.SESSION_KEY, sessionAdminUserDto);
            return getSuccessResponseVO(sessionAdminUserDto);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    /**
     * Token 登录接口（账号 + 密码，免图形验证码，供 AI / 第三方调用后台）。
     * <p>
     * 校验后台口令后签发 ADMIN 作用域 Token；登录态存 Redis，可主动吊销。
     */
    @PostMapping("/tokenLogin")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<Map<String, Object>> tokenLogin(@VerifyParam(required = true) String account,
                                                      @VerifyParam(required = true) String password) {
        if (!adminConfig.getAdminAccount().equals(account) || !adminConfig.getAdminPassword().equals(password)) {
            throw new BusinessException("账号或密码错误");
        }
        LoginUserContext context = new LoginUserContext();
        context.setAccount(account);
        context.setIsAdmin(true);
        context.setScope(TokenScope.ADMIN);
        String token = tokenService.createToken(context);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("account", account);
        return getSuccessResponseVO(result);
    }

    /**
     * Token 登出接口：吊销当前 Token（删除 Redis 登录态，立即失效）。
     */
    @PostMapping("/tokenLogout")
    public ResponseVO<Void> tokenLogout(HttpServletRequest request) {
        String header = request.getHeader(Constants.TOKEN_HEADER);
        if (header != null && header.startsWith(Constants.TOKEN_PREFIX)) {
            tokenService.invalidate(header.substring(Constants.TOKEN_PREFIX.length()).trim());
        }
        return getSuccessResponseVO(null);
    }
}

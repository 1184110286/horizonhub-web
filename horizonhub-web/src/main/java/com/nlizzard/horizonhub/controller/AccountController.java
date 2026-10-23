package com.nlizzard.horizonhub.controller;


import com.nlizzard.horizonhub.annotation.GlobalInterceptor;
import com.nlizzard.horizonhub.annotation.VerifyParam;
import com.nlizzard.horizonhub.basecontroller.BaseController;
import com.nlizzard.horizonhub.constants.Constants;
import com.nlizzard.horizonhub.entity.dto.CreateImageCode;
import com.nlizzard.horizonhub.entity.dto.LoginUserContext;
import com.nlizzard.horizonhub.entity.dto.SessionWebUserDto;
import com.nlizzard.horizonhub.entity.dto.SysSetting4CommentDto;
import com.nlizzard.horizonhub.entity.dto.SysSettingDto;
import com.nlizzard.horizonhub.entity.enums.TokenScope;
import com.nlizzard.horizonhub.entity.enums.VerifyRegexEnum;
import com.nlizzard.horizonhub.entity.vo.ResponseVO;
import com.nlizzard.horizonhub.exception.BusinessException;
import com.nlizzard.horizonhub.service.EmailCodeService;
import com.nlizzard.horizonhub.service.UserInfoService;
import com.nlizzard.horizonhub.utils.SysCacheUtils;
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

/**
 * 账号相关controller
 */
@RestController
public class AccountController extends BaseController {

    @Resource
    private EmailCodeService emailCodeService;

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private SysCacheUtils sysCacheUtils;

    @Resource
    private TokenService tokenService;

    /**
     * 生成图片验证码
     *
     * @param type 0：登录注册图片验证码，1：发送邮件图片验证码
     */
    @RequestMapping(value = "/checkCode")
    public void checkCode(HttpServletResponse response,
                          HttpSession session,
                          Integer type) throws IOException {
        // 创建图片验证码对象
        CreateImageCode codeEntity = new CreateImageCode(130, 38, 5, 10);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpeg");
        // 将验证码存入session
        String code = codeEntity.getCode();
        if (type == null || type == 0) {
            // 注册邮箱验证码
            session.setAttribute(Constants.CHECK_CODE_KEY, code);
        } else {
            // 邮箱验证码
            session.setAttribute(Constants.CHECK_CODE_KEY_EMAIL, code);
        }
        codeEntity.write(response.getOutputStream());
    }

    /**
     * 发送邮箱验证码
     *
     * @param email     接收验证码的邮箱地址
     * @param checkCode 图片验证码
     * @param type      0-注册验证码，1-找回密码验证码
     */
    @PostMapping("/sendEmailCode")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<Void> sendEmailCode(HttpSession session,
                                          @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL) String email,
                                          @VerifyParam(required = true) String checkCode,
                                          @VerifyParam(required = true) Integer type) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL))) {
                throw new BusinessException("图片验证码不正确");
            }
            emailCodeService.sendEmailCode(email, type);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }
    }

    /**
     * 注册账号接口
     *
     * @param email     邮箱
     * @param nickName  昵称
     * @param password  密码
     * @param checkCode 图片验证码
     * @param emailCode 邮箱验证码
     */
    @PostMapping("/register")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<Void> register(HttpSession session,
                                     @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL) String email,
                                     @VerifyParam(required = true, max = 20) String nickName,
                                     @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD) String password,
                                     @VerifyParam(required = true) String checkCode,
                                     @VerifyParam(required = true) String emailCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("图片验证码不正确");
            }
            userInfoService.register(email, nickName, password, emailCode);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    /**
     * 登录接口
     *
     * @param email     邮箱
     * @param password  密码(md5加密后的密码)
     * @param checkCode 图片验证码
     */
    @PostMapping("/login")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<SessionWebUserDto> login(HttpSession session, HttpServletRequest request,
                                               @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL) String email,
                                               @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD) String password,
                                               @VerifyParam(required = true) String checkCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("图片验证码不正确");
            }
            // 登录
            SessionWebUserDto sessionWebUserDto = userInfoService.login(email, password, getIpAddr(request));
            // 登录成功后将用户信息存入 session
            session.setAttribute(Constants.SESSION_KEY, sessionWebUserDto);
            return getSuccessResponseVO(sessionWebUserDto);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    /**
     * 获取当前登录用户信息接口
     */
    @RequestMapping("/getUserInfo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<SessionWebUserDto> getUserInfo(HttpSession session) {
        return getSuccessResponseVO(getUserInfoFromSession(session));

    }


    /**
     * 退出登录接口
     */
    @PostMapping("/logout")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<Void> logout(HttpSession session) {
        session.removeAttribute(Constants.SESSION_KEY);
        return getSuccessResponseVO(null);
    }

    /**
     * 获取系统设置接口
     */
    @RequestMapping("/getSysSetting")
    public ResponseVO<Map<String, Object>> getSysSetting() {
        SysSettingDto sysSettingDto = sysCacheUtils.getSysSetting();
        Map<String, Object> sysSettingMap = new HashMap<>();
        // 评论设置
        SysSetting4CommentDto commentSetting = sysSettingDto.getCommentSetting();
        sysSettingMap.put("commentOpen", commentSetting.getCommentOpen());
        return getSuccessResponseVO(sysSettingMap);
    }

    /**
     * 重置密码接口
     *
     * @param session   session
     * @param email     邮箱
     * @param password  新密码（原始密码）
     * @param checkCode 图片验证码
     * @param emailCode 邮箱验证码
     */
    @PostMapping("/resetPwd")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<Void> resetPwd(HttpSession session,
                                     @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL) String email,
                                     @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD, min = 8, max = 18) String password,
                                     @VerifyParam(required = true) String checkCode,
                                     @VerifyParam(required = true) String emailCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("图片验证码不正确");
            }
            userInfoService.resetPwd(email, password, emailCode);
            return getSuccessResponseVO(null);
        } finally {
            // 重置密码成功后，清除登录注册的图片验证码，避免重复使用
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    /**
     * Token 登录接口（账号 + 密码，免图形验证码，供 AI / 第三方 / 移动端）。
     * <p>
     * 复用 {@code UserInfoService.login} 校验密码并刷新登录信息，签发 WEB 作用域 Token；
     * 登录态存 Redis，可主动吊销。
     *
     * @param email    邮箱
     * @param password 明文密码（必须走 HTTPS）
     */
    @PostMapping("/tokenLogin")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<Map<String, Object>> tokenLogin(HttpServletRequest request,
                                                      @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL) String email,
                                                      @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD) String password) {
        SessionWebUserDto dto = userInfoService.login(email, password, getIpAddr(request));
        LoginUserContext context = new LoginUserContext();
        context.setUserId(dto.getUserId());
        context.setNickName(dto.getNickName());
        context.setIsAdmin(dto.getAdmin());
        context.setScope(TokenScope.WEB);
        String token = tokenService.createToken(context);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", dto);
        return getSuccessResponseVO(result);
    }

    /**
     * Token 登出接口：吊销当前 Token（删除 Redis 登录态，立即失效）。
     */
    @PostMapping("/tokenLogout")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO<Void> tokenLogout(HttpServletRequest request) {
        String header = request.getHeader(Constants.TOKEN_HEADER);
        if (header != null && header.startsWith(Constants.TOKEN_PREFIX)) {
            tokenService.invalidate(header.substring(Constants.TOKEN_PREFIX.length()).trim());
        }
        return getSuccessResponseVO(null);
    }
}

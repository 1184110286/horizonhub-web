package com.nlizzard.horizonhub.basecontroller;

import com.nlizzard.horizonhub.constants.Constants;
import com.nlizzard.horizonhub.entity.dto.LoginUserContext;
import com.nlizzard.horizonhub.entity.dto.SessionWebUserDto;
import com.nlizzard.horizonhub.entity.enums.ResponseCodeEnum;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;
import com.nlizzard.horizonhub.entity.vo.ResponseVO;
import com.nlizzard.horizonhub.utils.CopyTools;
import com.nlizzard.horizonhub.utils.TokenContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class BaseController {

    //成功状态信息
    protected static final String STATUS_SUCCESS = "success";

    //错误状态信息
    protected static final String STATUS_ERROR = "error";

    /**
     *
     * @param t data数据
     * @return 一个成功的ResponseVO对象
     */
    protected <T> ResponseVO<T> getSuccessResponseVO(T t) {
        ResponseVO<T> responseVO = new ResponseVO<>();
        responseVO.setStatus(STATUS_SUCCESS);
        responseVO.setCode(ResponseCodeEnum.CODE_200.getCode());
        responseVO.setInfo(ResponseCodeEnum.CODE_200.getMsg());
        responseVO.setData(t);
        return responseVO;
    }

    /**
     * 获取用户真实 IP 地址
     *
     */
    protected String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个ip值，第一个ip才是真实ip
            if (ip.indexOf(",") != -1) {
                ip = ip.split(",")[0];
            }
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 从 session 中获取用户信息
     *
     * @param session
     * @return
     */
    protected SessionWebUserDto getUserInfoFromSession(HttpSession session) {
        // 双轨：优先 Token 上下文（AI / 第三方 / 移动端），回落 Session（现有前端 cookie）
        LoginUserContext tokenContext = TokenContextHolder.get();
        if (tokenContext != null) {
            SessionWebUserDto dto = new SessionWebUserDto();
            dto.setUserId(tokenContext.getUserId());
            dto.setNickName(tokenContext.getNickName());
            dto.setAdmin(tokenContext.getIsAdmin());
            return dto;
        }
        return (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);
    }

    /**
     * 将分页结果转换为分页结果 VO 对象
     *
     * @param result 分页结果对象
     * @param classz 目标 VO 类
     * @param <S>    源对象类型
     * @param <T>    目标对象类型
     * @return
     */
    protected <S, T> PaginationResultVO<T> convert2PaginationVO(PaginationResultVO<S> result, Class<T> classz) {
        PaginationResultVO<T> resultVO = new PaginationResultVO<>();
        resultVO.setList(CopyTools.copyList(result.getList(), classz));
        resultVO.setPageNo(result.getPageNo());
        resultVO.setPageSize(result.getPageSize());
        resultVO.setPageTotal(result.getPageTotal());
        resultVO.setTotalCount(result.getTotalCount());
        return resultVO;
    }
}

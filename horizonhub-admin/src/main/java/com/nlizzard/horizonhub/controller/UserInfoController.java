package com.nlizzard.horizonhub.controller;

import com.nlizzard.horizonhub.annotation.GlobalInterceptor;
import com.nlizzard.horizonhub.annotation.VerifyParam;
import com.nlizzard.horizonhub.basecontroller.BaseController;
import com.nlizzard.horizonhub.entity.pojo.UserInfo;
import com.nlizzard.horizonhub.entity.query.UserInfoQuery;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;
import com.nlizzard.horizonhub.entity.vo.ResponseVO;
import com.nlizzard.horizonhub.service.UserInfoService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserInfoController extends BaseController {

    @Resource
    private UserInfoService userInfoService;

    /**
     * 分页加载用户列表
     *
     * @param userInfoQuery 查询类
     */
    @RequestMapping("/loadUserList")
    public ResponseVO<PaginationResultVO<UserInfo>> loadUserList(UserInfoQuery userInfoQuery) {
        userInfoQuery.setOrderBy("join_time desc");
        return getSuccessResponseVO(userInfoService.findListByPage(userInfoQuery));
    }


    /**
     * 更新用户状态
     *
     * @param status 状态：0禁用，1启用
     * @param userId 用户Id
     */
    @PostMapping("/updateUserStatus")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<Void> updateUserStatus(@VerifyParam(required = true) Integer status,
                                             @VerifyParam(required = true) String userId) {
        userInfoService.updateUserStatus(status, userId);
        return getSuccessResponseVO(null);
    }

    /**
     * 发送系统消息
     *
     * @param userId   用户Id
     * @param message  消息内容
     * @param integral 积分
     * @return
     */
    @PostMapping("/sendMessage")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<Void> sendMessage(@VerifyParam(required = true) String userId,
                                        @VerifyParam(required = true) String message,
                                        Integer integral) {
        userInfoService.sendMessage(userId, message, integral);
        return getSuccessResponseVO(null);
    }
}


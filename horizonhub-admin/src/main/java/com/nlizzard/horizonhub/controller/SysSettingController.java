package com.nlizzard.horizonhub.controller;

import com.nlizzard.horizonhub.annotation.GlobalInterceptor;
import com.nlizzard.horizonhub.annotation.VerifyParam;
import com.nlizzard.horizonhub.basecontroller.BaseController;
import com.nlizzard.horizonhub.entity.dto.*;
import com.nlizzard.horizonhub.entity.vo.ResponseVO;
import com.nlizzard.horizonhub.service.SysSettingService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/setting")
public class SysSettingController extends BaseController {
    @Resource
    private SysSettingService sysSettingService;


    /**
     * 获取系统设置
     */
    @RequestMapping("getSetting")
    public ResponseVO<SysSettingDto> getSetting() {
        return getSuccessResponseVO(sysSettingService.initSysSettingToCache());
    }

    /**
     * 更新保存系统设置
     *
     * @param auditDto    审核相关
     * @param commentDto  评论相关
     * @param postDto     发帖相关
     * @param likeDto     点赞相关
     * @param registerDto 注册相关
     * @param emailDto    邮件相关
     */
    @PostMapping("saveSetting")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO<Void> saveSetting(@VerifyParam SysSetting4AuditDto auditDto,
                                        @VerifyParam SysSetting4CommentDto commentDto,
                                        @VerifyParam SysSetting4PostDto postDto,
                                        @VerifyParam SysSetting4LikeDto likeDto,
                                        @VerifyParam SysSetting4RegisterDto registerDto,
                                        @VerifyParam SysSetting4EmailDto emailDto) {
        SysSettingDto sysSettingDto = new SysSettingDto();
        sysSettingDto.setAuditSetting(auditDto);
        sysSettingDto.setCommentSetting(commentDto);
        sysSettingDto.setPostSetting(postDto);
        sysSettingDto.setLikeSetting(likeDto);
        sysSettingDto.setEmailSetting(emailDto);
        sysSettingDto.setRegisterSetting(registerDto);
        sysSettingService.updateSetting(sysSettingDto);
        return getSuccessResponseVO(null);
    }
}

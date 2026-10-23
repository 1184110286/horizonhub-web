package com.nlizzard.horizonhub.controller;

import com.nlizzard.horizonhub.basecontroller.BaseFileController;
import com.nlizzard.horizonhub.entity.config.AdminConfig;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理侧文件接口。
 * <p>
 * 头像/图片读取等公共能力继承自 {@link BaseFileController}，本类只提供项目根目录。
 */
@RestController
@RequestMapping("/file")
public class FileController extends BaseFileController {

    @Resource
    private AdminConfig adminConfig;

    @Override
    protected String getProjectFolder() {
        return adminConfig.getProjectFolder();
    }
}

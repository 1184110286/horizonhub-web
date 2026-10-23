package com.nlizzard.horizonhub.entity.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AppConfig {
    /**
     * 是否是开发环境
     */
    @Value("${isDev.open:false}")
    private boolean isDev;

    /**
     * 当前项目目录地址
     */
    @Value("${projectFolder}")
    private String projectFolder;

    /**
     * 开发环境下测试用户邮箱
     */
    @Value("${isDev.testUserEmail}")
    private String DevTestEmail;


    public String getProjectFolder() {
        return projectFolder;
    }

    public boolean getIsDev() {
        return isDev;
    }

    public String getDevTestEmail() {
        return DevTestEmail;
    }

}


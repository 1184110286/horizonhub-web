package com.nlizzard.horizonhub;

import com.nlizzard.horizonhub.service.SysSettingService;
import jakarta.annotation.Resource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class InitRun implements ApplicationRunner {

    @Resource
    private SysSettingService sysSettingService;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        // 系统启动读系统配置到redis中（预热）
        sysSettingService.initSysSettingToCache();
    }
}

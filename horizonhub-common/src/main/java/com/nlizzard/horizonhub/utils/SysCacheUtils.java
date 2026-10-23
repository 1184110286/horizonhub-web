package com.nlizzard.horizonhub.utils;

import cn.hutool.core.util.StrUtil;
import com.nlizzard.horizonhub.entity.dto.SysSettingDto;
import com.nlizzard.horizonhub.service.SysSettingService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

import static com.nlizzard.horizonhub.constants.Constants.SYS_SETTING_KEY;
import static com.nlizzard.horizonhub.constants.Constants.SYS_SETTING_KEY_EXPIRE;

/**
 * 系统设置工具类  用于在内存中设置系统设置
 */
@Component
public class SysCacheUtils {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Lazy
    @Resource
    private SysSettingService sysSettingService;

    // 从redis中拿到系统设置
    public SysSettingDto getSysSetting() {
        SysSettingDto sysSettingDto;
        String sysSettingJson = stringRedisTemplate.opsForValue().get(SYS_SETTING_KEY);
        if (StrUtil.isBlankIfStr(sysSettingJson)) {
            sysSettingDto = sysSettingService.initSysSettingToCache();
        } else {
            sysSettingDto = JsonUtils.json2Object(sysSettingJson, SysSettingDto.class);
        }
        return sysSettingDto;
    }

    // 将系统设置缓存到redis中
    public void setSysSettingMap(SysSettingDto sysSettingDto) {
        stringRedisTemplate.opsForValue().set(SYS_SETTING_KEY, JsonUtils.object2Json(sysSettingDto));
        stringRedisTemplate.expire(SYS_SETTING_KEY, SYS_SETTING_KEY_EXPIRE, TimeUnit.MINUTES);
    }

    // 删除redis中系统设置
    public void deleteSysSettingInRedis() {
        stringRedisTemplate.delete(SYS_SETTING_KEY);
    }
}

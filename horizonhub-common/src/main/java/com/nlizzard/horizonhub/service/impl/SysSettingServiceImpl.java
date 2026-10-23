package com.nlizzard.horizonhub.service.impl;


import com.nlizzard.horizonhub.entity.dto.SysSettingDto;
import com.nlizzard.horizonhub.entity.enums.PageSize;
import com.nlizzard.horizonhub.entity.enums.SysSettingCodeEnum;
import com.nlizzard.horizonhub.entity.pojo.SysSetting;
import com.nlizzard.horizonhub.entity.query.SysSettingQuery;
import com.nlizzard.horizonhub.entity.query.basequery.SimplePage;
import com.nlizzard.horizonhub.entity.vo.PaginationResultVO;
import com.nlizzard.horizonhub.exception.BusinessException;
import com.nlizzard.horizonhub.mappers.SysSettingMapper;
import com.nlizzard.horizonhub.service.SysSettingService;
import com.nlizzard.horizonhub.utils.JsonUtils;
import com.nlizzard.horizonhub.utils.SysCacheUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @Description:系统设置信息ServiceImpl
 * @author:nlizzard
 * @date:2026/03/08
 */
@Service("sysSettingService")
public class SysSettingServiceImpl implements SysSettingService {

    private static final Logger logger = LoggerFactory.getLogger(SysSettingServiceImpl.class);

    @Resource
    private SysSettingMapper<SysSetting, SysSettingQuery> sysSettingMapper;

    @Resource
    private SysCacheUtils sysCacheUtils;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<SysSetting> findListByParam(SysSettingQuery query) {
        return this.sysSettingMapper.selectList(query);
    }

    /**
     * 根据条件查询数量
     */
    @Override
    public Integer findCountByParam(SysSettingQuery query) {
        return this.sysSettingMapper.selectCount(query);
    }

    /**
     * 分页查询
     */
    @Override
    public PaginationResultVO<SysSetting> findListByPage(SysSettingQuery query) {
        Integer count = this.findCountByParam(query);
        Integer pageSize = query.getPageSize() == null ? PageSize.SIZE15.getSize() : query.getPageSize();
        SimplePage page = new SimplePage(query.getPageNo(), count, pageSize);
        query.setSimplePage(page);
        List<SysSetting> list = this.findListByParam(query);
        return new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
    }

    /**
     * 新增
     */
    @Override
    public Integer add(SysSetting bean) {
        return this.sysSettingMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<SysSetting> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.sysSettingMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或修改
     */
    @Override
    public Integer addOrUpdateBatch(List<SysSetting> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.sysSettingMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 根据Code查询
     */
    @Override
    public SysSetting getSysSettingByCode(String code) {
        return this.sysSettingMapper.selectByCode(code);
    }

    /**
     * 根据Code更新
     */
    @Override
    public Integer updateSysSettingByCode(SysSetting bean, String code) {
        return this.sysSettingMapper.updateByCode(bean, code);
    }

    /**
     * 根据Code删除
     */
    @Override
    public Integer deleteSysSettingByCode(String code) {
        return this.sysSettingMapper.deleteByCode(code);
    }

    /**
     * 将系统设置初始化到内存中  在系统启动时调用  将系统设置加载到内存中  以便后续使用  避免每次查询数据库获取系统设置
     */
    @Override
    public SysSettingDto initSysSettingToCache() {
        try {
            SysSettingDto sysSettingDto = new SysSettingDto();
            List<SysSetting> sysSettinglist = this.findListByParam(new SysSettingQuery());
            Class<SysSettingDto> clazz = SysSettingDto.class;
            for (SysSetting sysSetting : sysSettinglist) {
                // 具体配置 json 内容
                String jsonContent = sysSetting.getJsonContent();
                if (StringUtils.isBlank(jsonContent)) {
                    continue;
                }
                // 根据code获取枚举类  以便后续根据枚举类反序列化json内容
                SysSettingCodeEnum sysSettingCodeEnum = SysSettingCodeEnum.getByCode(sysSetting.getCode());
                if (sysSettingCodeEnum == null) {
                    logger.warn("系统设置code未找到对应枚举类，code: {}", sysSetting.getCode());
                    continue;
                }
                PropertyDescriptor pd = new PropertyDescriptor(sysSettingCodeEnum.getPropName(), clazz);
                Method writeMethod = pd.getWriteMethod();
                // 取消Java语言访问检查，提升反射效率
                writeMethod.setAccessible(true);
                Object o = JsonUtils.json2Object(jsonContent, Class.forName(sysSettingCodeEnum.getClassZ()));
                writeMethod.invoke(sysSettingDto, o);
            }
            sysCacheUtils.setSysSettingMap(sysSettingDto);
            logger.info("系统设置写入redis成功");
            return sysSettingDto;
        } catch (Exception e) {
            logger.error("系统设置写入redis失败", e);
            throw new BusinessException("系统设置写入redis失败");
        } finally {
            logger.info("web后端系统准备就绪 :)");
        }
    }

    /**
     * 更新系统设置  将新的系统设置保存到数据库中
     *
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSetting(SysSettingDto sysSettingDto) {
        SysSetting sysSetting = new SysSetting();
        // 遍历字段，拿到read方法，拿到code，read读出来转json字符串
        for (SysSettingCodeEnum settingCodeEnum : SysSettingCodeEnum.values()) {
            try {
                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(settingCodeEnum.getPropName(), SysSettingDto.class);
                Method readMethod = propertyDescriptor.getReadMethod();
                String setting = JsonUtils.object2Json(readMethod.invoke(sysSettingDto));
                String code = settingCodeEnum.getCode();
                //执行更新操作
                sysSetting.setJsonContent(setting);
                sysSettingMapper.updateByCode(sysSetting, code);
            } catch (Exception e) {
                logger.error("更新系统设置报错:", e);
                throw new BusinessException("更新系统设置失败");
            }
        }
        // 删除redis缓存
        sysCacheUtils.deleteSysSettingInRedis();
    }
}
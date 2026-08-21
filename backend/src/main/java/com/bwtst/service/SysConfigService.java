package com.bwtst.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bwtst.common.BusinessException;
import com.bwtst.entity.SysConfig;
import com.bwtst.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

// add by codebuddy 20260817: 系统配置读写，当前用于要货单ERP上传定时任务的间隔配置
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigService {

    public static final String KEY_UPLOAD_INTERVAL_MINUTES = "erp.upload.interval.minutes";

    // add by codebuddy 20260818: 要货单ERP上传定时任务开关（true-启用 / false-停用）
    public static final String KEY_UPLOAD_ENABLED = "erp.upload.enabled";

    private final SysConfigMapper sysConfigMapper;

    /** 默认间隔：3 分钟 */
    private static final int DEFAULT_INTERVAL_MINUTES = 3;

    public int getUploadIntervalMinutes() {
        try {
            SysConfig cfg = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                    .eq(SysConfig::getConfigKey, KEY_UPLOAD_INTERVAL_MINUTES));
            if (cfg == null || !StringUtils.hasText(cfg.getConfigValue())) {
                return DEFAULT_INTERVAL_MINUTES;
            }
            return Math.max(1, Integer.parseInt(cfg.getConfigValue().trim()));
        } catch (DataAccessException e) {
            // add by codebuddy 20260817: 首次启动 sys_config 尚未创建时，使用默认值避免阻塞启动
            log.warn("读取定时任务间隔配置失败(表可能未创建)，使用默认值 {} 分钟: {}", DEFAULT_INTERVAL_MINUTES, e.getMessage());
            return DEFAULT_INTERVAL_MINUTES;
        } catch (NumberFormatException e) {
            return DEFAULT_INTERVAL_MINUTES;
        }
    }

    public void updateUploadIntervalMinutes(int minutes) {
        if (minutes < 1 || minutes > 1440) {
            throw new BusinessException("定时任务间隔需在 1~1440 分钟之间");
        }
        SysConfig cfg = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, KEY_UPLOAD_INTERVAL_MINUTES));
        if (cfg == null) {
            cfg = new SysConfig();
            cfg.setConfigKey(KEY_UPLOAD_INTERVAL_MINUTES);
            cfg.setConfigValue(String.valueOf(minutes));
            cfg.setRemark("要货单ERP上传定时任务间隔（分钟）");
            sysConfigMapper.insert(cfg);
        } else {
            cfg.setConfigValue(String.valueOf(minutes));
            sysConfigMapper.updateById(cfg);
        }
    }

    // add by codebuddy 20260818: 读取定时任务开关状态，配置缺失时默认停用，避免用户关闭后刷新页面又被自动开启
    public boolean isUploadEnabled() {
        try {
            SysConfig cfg = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                    .eq(SysConfig::getConfigKey, KEY_UPLOAD_ENABLED));
            if (cfg == null || !StringUtils.hasText(cfg.getConfigValue())) {
                return false;
            }
            return Boolean.parseBoolean(cfg.getConfigValue().trim());
        } catch (DataAccessException e) {
            // add by codebuddy 20260818: 首次启动 sys_config 尚未创建时，按停用处理
            log.warn("读取定时任务开关配置失败(表可能未创建)，按停用处理: {}", e.getMessage());
            return false;
        }
    }

    // add by codebuddy 20260818: 保存定时任务开关状态
    public void setUploadEnabled(boolean enabled) {
        SysConfig cfg = sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, KEY_UPLOAD_ENABLED));
        if (cfg == null) {
            cfg = new SysConfig();
            cfg.setConfigKey(KEY_UPLOAD_ENABLED);
            cfg.setConfigValue(String.valueOf(enabled));
            cfg.setRemark("要货单ERP上传定时任务开关（true-启用/false-停用）");
            sysConfigMapper.insert(cfg);
        } else {
            cfg.setConfigValue(String.valueOf(enabled));
            sysConfigMapper.updateById(cfg);
        }
    }
}

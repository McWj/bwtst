package com.bwtst.task;

import com.bwtst.service.DeliveryShipmentService;
import com.bwtst.service.DistributionReturnService;
import com.bwtst.service.DistributionService;
import com.bwtst.service.RequisitionService;
import com.bwtst.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.FixedDelayTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

// add by codebuddy 20260817: 定时抓取已提交(TJ)的要货单上传 ERP，间隔可在首页动态配置，默认 3 分钟
@Slf4j
@Component
@RequiredArgsConstructor
public class ErpUploadTask implements SchedulingConfigurer {

    private final RequisitionService requisitionService;
    // add by codebuddy 20260817: 配送发货单上传 ERP（DocnoUpload）共用同一调度
    private final DeliveryShipmentService deliveryShipmentService;
    // add by codebuddy 20260819: 门店配送收货单上传 ERP（return.create）共用同一调度
    private final DistributionService distributionService;
    // add by codebuddy 20260819: 配送退货单上传 ERP（return.create）共用同一调度
    private final DistributionReturnService distributionReturnService;
    private final SysConfigService configService;

    private ScheduledTaskRegistrar registrar;
    private ScheduledTask currentTask;

    /** 上次执行时间 */
    private volatile LocalDateTime lastRunAt;
    /** 下次执行时间 */
    private volatile LocalDateTime nextRunAt;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        this.registrar = taskRegistrar;
        reschedule();
    }

    /**
     * 重新调度：取消当前任务，按配置的间隔重新注册。
     * 首页保存定时任务配置后调用，立即生效。
     * add by codebuddy 20260818: 支持停用/启用，停用时不注册调度
     */
    public synchronized void reschedule() {
        if (currentTask != null) {
            currentTask.cancel();
            currentTask = null;
        }
        if (registrar == null) {
            return;
        }
        if (!configService.isUploadEnabled()) {
            nextRunAt = null;
            log.info("要货单ERP上传定时任务已停用");
            return;
        }
        int intervalMinutes = configService.getUploadIntervalMinutes();
        nextRunAt = LocalDateTime.now().plusMinutes(intervalMinutes);
        currentTask = registrar.scheduleFixedDelayTask(
                new FixedDelayTask(() -> runUpload(), Duration.ofMinutes(intervalMinutes), Duration.ofSeconds(15)));
        log.info("要货单ERP上传定时任务已重调度，间隔 {} 分钟，下次执行 {}", intervalMinutes, nextRunAt);
    }

    private void runUpload() {
        lastRunAt = LocalDateTime.now();
        nextRunAt = LocalDateTime.now().plusMinutes(configService.getUploadIntervalMinutes());
        try {
            int success = requisitionService.uploadSubmittedToErp();
            if (success > 0) {
                log.info("定时上传 ERP 完成，本次成功 {} 张要货单", success);
            }
        } catch (Exception e) {
            log.error("要货单定时上传 ERP 执行异常: {}", e.getMessage());
        }
        // add by codebuddy 20260817: 配送发货单上传 ERP（DocnoUpload）共用同一调度
        try {
            int success = deliveryShipmentService.uploadSubmittedToErp();
            if (success > 0) {
                log.info("定时上传 ERP 完成，本次成功 {} 张配送发货单", success);
            }
        } catch (Exception e) {
            log.error("配送发货单定时上传 ERP 执行异常: {}", e.getMessage());
        }
        // add by codebuddy 20260819: 门店配送收货单上传 ERP（return.create）共用同一调度
        try {
            int success = distributionService.uploadSubmittedToErp();
            if (success > 0) {
                log.info("定时上传 ERP 完成，本次成功 {} 张门店配送收货单", success);
            }
        } catch (Exception e) {
            log.error("门店配送收货单定时上传 ERP 执行异常: {}", e.getMessage());
        }
        // add by codebuddy 20260819: 配送退货单上传 ERP（return.create）共用同一调度
        try {
            distributionReturnService.uploadSubmittedToErp();
            log.info("定时上传 ERP 完成，配送退货单已抓取上传");
        } catch (Exception e) {
            log.error("配送退货单定时上传 ERP 执行异常: {}", e.getMessage());
        }
    }

    public int getIntervalMinutes() {
        return configService.getUploadIntervalMinutes();
    }

    // add by codebuddy 20260818: 当前定时任务是否启用
    public boolean isEnabled() {
        return configService.isUploadEnabled();
    }

    public LocalDateTime getLastRunAt() {
        return lastRunAt;
    }

    public LocalDateTime getNextRunAt() {
        return nextRunAt;
    }
}

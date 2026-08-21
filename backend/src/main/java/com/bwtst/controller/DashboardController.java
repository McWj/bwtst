package com.bwtst.controller;

import com.bwtst.common.Result;
import com.bwtst.service.DashboardService;
import com.bwtst.service.SysConfigService;
import com.bwtst.task.ErpUploadTask;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// add by codebuddy 20260817: 首页大屏数据总览与定时任务配置
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final SysConfigService sysConfigService;
    private final ErpUploadTask erpUploadTask;

    /** 大屏总览数据（view: hq-总部/shop-门店/dc-配送中心，门店视角需带 shopId，配送中心视角需带 dcId） */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(@RequestParam(required = false) String view,
                                                @RequestParam(required = false) Integer shopId,
                                                @RequestParam(required = false) Integer dcId) {
        return Result.ok(dashboardService.overview(view, shopId, dcId));
    }

    // add by codebuddy 20260818: 未上传 ERP 单据明细（含 ERP 报错）
    /** 未上传 ERP 单据明细（含 ERP 报错） */
    @GetMapping("/unuploaded-detail")
    public Result<List<Map<String, Object>>> unuploadedDetail(@RequestParam String type,
                                                              @RequestParam(required = false) String view,
                                                              @RequestParam(required = false) Integer shopId,
                                                              @RequestParam(required = false) Integer dcId) {
        return Result.ok(dashboardService.unuploadedDetail(type, view, shopId, dcId));
    }

    // add by codebuddy 20260818: 终止单据自动上传（终止后不再出现在未上传列表，定时任务跳过）
    /** 终止单据自动上传 */
    @PostMapping("/abort-upload")
    public Result<Void> abortUpload(@RequestParam String type,
                                    @RequestParam Integer id) {
        dashboardService.abortUpload(type, id);
        return Result.ok(null);
    }

    /** 定时任务配置 */
    @GetMapping("/task-config")
    public Result<Map<String, Object>> taskConfig() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("intervalMinutes", erpUploadTask.getIntervalMinutes());
        map.put("lastRunAt", erpUploadTask.getLastRunAt());
        map.put("nextRunAt", erpUploadTask.getNextRunAt());
        // add by codebuddy 20260818: 定时任务开关状态
        map.put("enabled", erpUploadTask.isEnabled());
        return Result.ok(map);
    }

    // add by codebuddy 20260818: 更新定时任务间隔（分钟）与开关状态，保存后立即重新调度
    @PutMapping("/task-config")
    public Result<Map<String, Object>> updateTaskConfig(@RequestBody Map<String, Object> body) {
        Object v = body.get("intervalMinutes");
        int minutes = (v instanceof Number) ? ((Number) v).intValue() : Integer.parseInt(String.valueOf(v));
        sysConfigService.updateUploadIntervalMinutes(minutes);
        Object e = body.get("enabled");
        if (e != null) {
            sysConfigService.setUploadEnabled(Boolean.parseBoolean(String.valueOf(e)));
        }
        erpUploadTask.reschedule();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("intervalMinutes", erpUploadTask.getIntervalMinutes());
        map.put("lastRunAt", erpUploadTask.getLastRunAt());
        map.put("nextRunAt", erpUploadTask.getNextRunAt());
        map.put("enabled", erpUploadTask.isEnabled());
        return Result.ok(map);
    }
}

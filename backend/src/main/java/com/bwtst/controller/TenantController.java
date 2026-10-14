package com.bwtst.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bwtst.common.BusinessException;
import com.bwtst.common.Result;
import com.bwtst.common.TenantContext;
import com.bwtst.common.TenantSaveRequest;
import com.bwtst.entity.SysTenant;
import com.bwtst.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// add by codebuddy 20260923: SaaS 租户管理接口，仅平台超级管理员可访问
@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @GetMapping("/page")
    public Result<IPage<SysTenant>> page(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) String keyword) {
        requirePlatformAdmin();
        return Result.ok(tenantService.page(page, size, keyword));
    }

    /** 新增或编辑租户 */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody TenantSaveRequest req) {
        requirePlatformAdmin();
        if (req.getId() == null) {
            tenantService.create(req);
        } else {
            tenantService.update(req);
        }
        return Result.ok();
    }

    /** 启用/停用租户 */
    @PostMapping("/status")
    public Result<Void> status(@RequestBody Map<String, Object> body) {
        requirePlatformAdmin();
        tenantService.setStatus(toLong(body.get("id")), String.valueOf(body.get("status")));
        return Result.ok();
    }

    /** 重置租户下某账号的密码 */
    @PostMapping("/reset-admin-password")
    public Result<Void> resetAdminPassword(@RequestBody Map<String, Object> body) {
        requirePlatformAdmin();
        tenantService.resetAdminPassword(toLong(body.get("tenantId")),
                body.get("username") == null ? null : String.valueOf(body.get("username")),
                body.get("newPassword") == null ? null : String.valueOf(body.get("newPassword")));
        return Result.ok();
    }

    private Long toLong(Object v) {
        if (v == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(v));
        } catch (NumberFormatException e) {
            throw new BusinessException("参数格式不正确");
        }
    }

    private void requirePlatformAdmin() {
        if (!TenantContext.isPlatformAdmin()) {
            throw new BusinessException("无权限：仅平台超级管理员可管理租户");
        }
    }
}

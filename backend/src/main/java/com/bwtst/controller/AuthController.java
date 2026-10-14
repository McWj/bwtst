package com.bwtst.controller;

import com.bwtst.common.LoginResult;
import com.bwtst.common.Result;
import com.bwtst.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// add by codebuddy 20260819
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 登录。
     * add by codebuddy 20260923: 返回令牌 + 用户信息；tenantCode 为可选的企业编码，留空时在单租户环境下自动使用默认租户
     */
    @PostMapping("/login")
    public Result<LoginResult> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String tenantCode = body.get("tenantCode");
        return Result.ok(userService.login(username, password, tenantCode));
    }

    /**
     * 平台管理端登录。
     * add by codebuddy 20260923: 与业务系统完全分开的入口，仅平台超级管理员可登录
     */
    @PostMapping("/platform-login")
    public Result<LoginResult> platformLogin(@RequestBody Map<String, String> body) {
        return Result.ok(userService.login(body.get("username"), body.get("password"),
                body.get("tenantCode"), true));
    }
}

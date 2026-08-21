package com.bwtst.controller;

import com.bwtst.common.Result;
import com.bwtst.entity.SysUser;
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

    @PostMapping("/login")
    public Result<SysUser> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        SysUser u = userService.login(username, password);
        return Result.ok(u);
    }
}

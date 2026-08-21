package com.bwtst.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bwtst.common.Result;
import java.util.Map;
import com.bwtst.entity.SysUser;
import com.bwtst.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/page")
    public Result<IPage<SysUser>> page(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(required = false) String keyword) {
        return Result.ok(userService.page(page, size, keyword));
    }

    @GetMapping("/{id}")
    public Result<SysUser> get(@PathVariable Integer id) {
        return Result.ok(userService.detail(id));
    }

    @PostMapping("/save")
    public Result<Void> save(@RequestBody SysUser u) {
        userService.save(u);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        userService.delete(id);
        return Result.ok();
    }

    // add by codebuddy 20260819: 批量生成门店用户
    @PostMapping("/generate-shop")
    public Result<String> generateShopUsers() {
        return Result.ok(userService.generateShopUsers());
    }

    // add by codebuddy 20260819: 单独修改密码
    @PostMapping("/reset-password/{id}")
    public Result<Void> resetPassword(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        userService.resetPassword(id, body.get("password"));
        return Result.ok();
    }
}

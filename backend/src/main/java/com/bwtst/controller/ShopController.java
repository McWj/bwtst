package com.bwtst.controller;

import com.bwtst.common.PageResult;
import com.bwtst.common.Result;
import com.bwtst.entity.Shop;
import com.bwtst.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// add by codebuddy 20260817
@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    @GetMapping("/page")
    public Result<PageResult<Shop>> page(@RequestParam(defaultValue = "1") long pageNum,
                                         @RequestParam(defaultValue = "10") long pageSize,
                                         @RequestParam(required = false) String keyword) {
        return Result.ok(shopService.page(pageNum, pageSize, keyword));
    }

    @GetMapping("/list")
    public Result<List<Shop>> list() {
        return Result.ok(shopService.list());
    }

    @GetMapping("/{id}")
    public Result<Shop> getById(@PathVariable Integer id) {
        return Result.ok(shopService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Shop shop) {
        shopService.save(shop);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Shop shop) {
        shopService.update(shop);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        shopService.delete(id);
        return Result.ok();
    }
}

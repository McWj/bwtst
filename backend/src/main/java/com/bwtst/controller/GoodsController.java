package com.bwtst.controller;

import com.bwtst.common.PageResult;
import com.bwtst.common.Result;
import com.bwtst.entity.Goods;
import com.bwtst.service.GoodsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// add by codebuddy 20260817
@RestController
@RequestMapping("/api/goods")
@RequiredArgsConstructor
public class GoodsController {

    private final GoodsService goodsService;

    @GetMapping("/page")
    public Result<PageResult<Goods>> page(@RequestParam(defaultValue = "1") long pageNum,
                                          @RequestParam(defaultValue = "10") long pageSize,
                                          @RequestParam(required = false) String keyword) {
        return Result.ok(goodsService.page(pageNum, pageSize, keyword));
    }

    @GetMapping("/list")
    public Result<List<Goods>> list() {
        return Result.ok(goodsService.list());
    }

    @GetMapping("/{id}")
    public Result<Goods> getById(@PathVariable Integer id) {
        return Result.ok(goodsService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Goods goods) {
        goodsService.save(goods);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Goods goods) {
        goodsService.update(goods);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        goodsService.delete(id);
        return Result.ok();
    }
}

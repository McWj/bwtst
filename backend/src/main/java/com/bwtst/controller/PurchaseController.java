package com.bwtst.controller;

import com.bwtst.common.BillDto;
import com.bwtst.common.PageResult;
import com.bwtst.common.Result;
import com.bwtst.entity.Purchase;
import com.bwtst.entity.PurchaseDetail;
import com.bwtst.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// add by codebuddy 20260817
@RestController
@RequestMapping("/api/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @GetMapping("/page")
    public Result<PageResult<Purchase>> page(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer siteId) {
        return Result.ok(purchaseService.page(pageNum, pageSize, keyword, siteId));
    }

    @GetMapping("/{id}")
    public Result<BillDto<Purchase, PurchaseDetail>> getById(@PathVariable Integer id) {
        return Result.ok(purchaseService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody BillDto<Purchase, PurchaseDetail> dto) {
        purchaseService.save(dto);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody BillDto<Purchase, PurchaseDetail> dto) {
        purchaseService.save(dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        purchaseService.delete(id);
        return Result.ok();
    }

    // add by codebuddy 20260817: 确认采购收货单
    @PostMapping("/confirm/{id}")
    public Result<Void> confirm(@PathVariable Integer id) {
        purchaseService.confirm(id);
        return Result.ok();
    }

    // add by codebuddy 20260817: 提交采购收货单
    @PostMapping("/submit/{id}")
    public Result<Void> submit(@PathVariable Integer id) {
        purchaseService.submit(id);
        return Result.ok();
    }
}

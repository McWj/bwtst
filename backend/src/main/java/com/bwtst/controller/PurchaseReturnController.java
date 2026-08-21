package com.bwtst.controller;

import com.bwtst.common.BillDto;
import com.bwtst.common.PageResult;
import com.bwtst.common.Result;
import com.bwtst.entity.PurchaseReturn;
import com.bwtst.entity.PurchaseReturnDetail;
import com.bwtst.service.PurchaseReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// add by codebuddy 20260817
@RestController
@RequestMapping("/api/purchase-return")
@RequiredArgsConstructor
public class PurchaseReturnController {

    private final PurchaseReturnService purchaseReturnService;

    @GetMapping("/page")
    public Result<PageResult<PurchaseReturn>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                   @RequestParam(defaultValue = "10") long pageSize,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Integer siteId) {
        return Result.ok(purchaseReturnService.page(pageNum, pageSize, keyword, siteId));
    }

    @GetMapping("/{id}")
    public Result<BillDto<PurchaseReturn, PurchaseReturnDetail>> getById(@PathVariable Integer id) {
        return Result.ok(purchaseReturnService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody BillDto<PurchaseReturn, PurchaseReturnDetail> dto) {
        purchaseReturnService.save(dto);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody BillDto<PurchaseReturn, PurchaseReturnDetail> dto) {
        purchaseReturnService.save(dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        purchaseReturnService.delete(id);
        return Result.ok();
    }

    // add by codebuddy 20260817: 确认采购退货单
    @PostMapping("/confirm/{id}")
    public Result<Void> confirm(@PathVariable Integer id) {
        purchaseReturnService.confirm(id);
        return Result.ok();
    }

    // add by codebuddy 20260817: 提交采购退货单
    @PostMapping("/submit/{id}")
    public Result<Void> submit(@PathVariable Integer id) {
        purchaseReturnService.submit(id);
        return Result.ok();
    }
}

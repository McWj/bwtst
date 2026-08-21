package com.bwtst.controller;

import com.bwtst.common.BillDto;
import com.bwtst.common.PageResult;
import com.bwtst.common.Result;
import com.bwtst.entity.PurchaseInbound;
import com.bwtst.entity.PurchaseInboundDetail;
import com.bwtst.service.PurchaseInboundService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// add by codebuddy 20260818
@RestController
@RequestMapping("/api/purchase-inbound")
@RequiredArgsConstructor
public class PurchaseInboundController {

    private final PurchaseInboundService inboundService;

    @GetMapping("/page")
    public Result<PageResult<PurchaseInbound>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                    @RequestParam(defaultValue = "10") long pageSize,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) Integer deliveryCenterId) {
        return Result.ok(inboundService.page(pageNum, pageSize, keyword, deliveryCenterId));
    }

    @GetMapping("/{id}")
    public Result<BillDto<PurchaseInbound, PurchaseInboundDetail>> getById(@PathVariable Integer id) {
        return Result.ok(inboundService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody BillDto<PurchaseInbound, PurchaseInboundDetail> dto) {
        inboundService.save(dto);
        return Result.ok();
    }

    // add by codebuddy 20260818: 确认
    @PostMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Integer id) {
        inboundService.confirm(id);
        return Result.ok();
    }

    // add by codebuddy 20260818: 提交
    @PostMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable Integer id) {
        inboundService.submit(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        inboundService.delete(id);
        return Result.ok();
    }
}

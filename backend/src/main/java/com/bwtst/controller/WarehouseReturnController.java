package com.bwtst.controller;

import com.bwtst.common.BillDto;
import com.bwtst.common.PageResult;
import com.bwtst.common.Result;
import com.bwtst.entity.WarehouseReturn;
import com.bwtst.entity.WarehouseReturnDetail;
import com.bwtst.service.WarehouseReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// add by codebuddy 20260818
@RestController
@RequestMapping("/api/warehouse-return")
@RequiredArgsConstructor
public class WarehouseReturnController {

    private final WarehouseReturnService returnService;

    @GetMapping("/page")
    public Result<PageResult<WarehouseReturn>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                    @RequestParam(defaultValue = "10") long pageSize,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) Integer deliveryCenterId) {
        return Result.ok(returnService.page(pageNum, pageSize, keyword, deliveryCenterId));
    }

    @GetMapping("/{id}")
    public Result<BillDto<WarehouseReturn, WarehouseReturnDetail>> getById(@PathVariable Integer id) {
        return Result.ok(returnService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody BillDto<WarehouseReturn, WarehouseReturnDetail> dto) {
        returnService.save(dto);
        return Result.ok();
    }

    // add by codebuddy 20260818: 确认
    @PostMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Integer id) {
        returnService.confirm(id);
        return Result.ok();
    }

    // add by codebuddy 20260818: 提交
    @PostMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable Integer id) {
        returnService.submit(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        returnService.delete(id);
        return Result.ok();
    }
}

package com.bwtst.controller;

import com.bwtst.common.BillDto;
import com.bwtst.common.PageResult;
import com.bwtst.common.Result;
import com.bwtst.entity.Requisition;
import com.bwtst.entity.RequisitionDetail;
import com.bwtst.service.RequisitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// add by codebuddy 20260817
@RestController
@RequestMapping("/api/requisition")
@RequiredArgsConstructor
public class RequisitionController {

    private final RequisitionService requisitionService;

    @GetMapping("/page")
    public Result<PageResult<Requisition>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                @RequestParam(defaultValue = "10") long pageSize,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Integer siteId) {
        return Result.ok(requisitionService.page(pageNum, pageSize, keyword, siteId));
    }

    @GetMapping("/{id}")
    public Result<BillDto<Requisition, RequisitionDetail>> getById(@PathVariable Integer id) {
        return Result.ok(requisitionService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody BillDto<Requisition, RequisitionDetail> dto) {
        requisitionService.save(dto);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody BillDto<Requisition, RequisitionDetail> dto) {
        requisitionService.save(dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        requisitionService.delete(id);
        return Result.ok();
    }

    // add by codebuddy 20260817: 确认要货单（body 可携带 ERP 返回的预估运费/重量/体积及单位）
    @PostMapping("/confirm/{id}")
    public Result<Void> confirm(@PathVariable Integer id, @RequestBody(required = false) Requisition body) {
        requisitionService.confirm(id, body);
        return Result.ok();
    }

    // add by codebuddy 20260817: 提交要货单（提交后自动生成收货单）
    @PostMapping("/submit/{id}")
    public Result<Void> submit(@PathVariable Integer id) {
        requisitionService.submit(id);
        return Result.ok();
    }
}

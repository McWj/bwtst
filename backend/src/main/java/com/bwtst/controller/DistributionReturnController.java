package com.bwtst.controller;

import com.bwtst.common.BillDto;
import com.bwtst.common.PageResult;
import com.bwtst.common.Result;
import com.bwtst.entity.Distribution;
import com.bwtst.entity.DistributionDetail;
import com.bwtst.entity.DistributionReturn;
import com.bwtst.entity.DistributionReturnDetail;
import com.bwtst.service.DistributionReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// add by codebuddy 20260817
@RestController
@RequestMapping("/api/distribution-return")
@RequiredArgsConstructor
public class DistributionReturnController {

    private final DistributionReturnService distributionReturnService;

    @GetMapping("/page")
    public Result<PageResult<DistributionReturn>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                       @RequestParam(defaultValue = "10") long pageSize,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Integer siteId) {
        return Result.ok(distributionReturnService.page(pageNum, pageSize, keyword, siteId));
    }

    @GetMapping("/{id}")
    public Result<BillDto<DistributionReturn, DistributionReturnDetail>> getById(@PathVariable Integer id) {
        return Result.ok(distributionReturnService.getById(id));
    }

    // add by codebuddy 20260817: 可选来源配送收货单列表（新增配送退货单时选择）
    @GetMapping("/source-distributions")
    public Result<java.util.List<Distribution>> listSourceDistributions(@RequestParam(required = false) Integer siteId) {
        return Result.ok(distributionReturnService.listSourceDistributions(siteId));
    }

    // add by codebuddy 20260817: 来源配送收货单及明细（带出到配送退货单）
    @GetMapping("/source/{id}")
    public Result<BillDto<Distribution, DistributionDetail>> getSourceDistribution(@PathVariable Integer id) {
        return Result.ok(distributionReturnService.getSourceDistribution(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody BillDto<DistributionReturn, DistributionReturnDetail> dto) {
        distributionReturnService.save(dto);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody BillDto<DistributionReturn, DistributionReturnDetail> dto) {
        distributionReturnService.save(dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        distributionReturnService.delete(id);
        return Result.ok();
    }

    // add by codebuddy 20260817: 确认配送退货单
    @PostMapping("/confirm/{id}")
    public Result<Void> confirm(@PathVariable Integer id) {
        distributionReturnService.confirm(id);
        return Result.ok();
    }

    // add by codebuddy 20260817: 提交配送退货单
    @PostMapping("/submit/{id}")
    public Result<Void> submit(@PathVariable Integer id) {
        distributionReturnService.submit(id);
        return Result.ok();
    }

    // add by codebuddy 20260819: 单张配送退货单上传 ERP（return.create）
    @PostMapping("/upload/{id}")
    public Result<Void> upload(@PathVariable Integer id) {
        distributionReturnService.uploadOneToErp(id);
        return Result.ok();
    }
}

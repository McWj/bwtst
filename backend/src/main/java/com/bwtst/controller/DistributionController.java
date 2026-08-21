package com.bwtst.controller;

import com.bwtst.common.BillDto;
import com.bwtst.common.PageResult;
import com.bwtst.common.Result;
import com.bwtst.entity.Distribution;
import com.bwtst.entity.DistributionDetail;
import com.bwtst.service.DistributionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// add by codebuddy 20260817
@RestController
@RequestMapping("/api/distribution")
@RequiredArgsConstructor
public class DistributionController {

    private final DistributionService distributionService;

    @GetMapping("/page")
    public Result<PageResult<Distribution>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                 @RequestParam(defaultValue = "10") long pageSize,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Integer siteId) {
        return Result.ok(distributionService.page(pageNum, pageSize, keyword, siteId));
    }

    @GetMapping("/{id}")
    public Result<BillDto<Distribution, DistributionDetail>> getById(@PathVariable Integer id) {
        return Result.ok(distributionService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody BillDto<Distribution, DistributionDetail> dto) {
        distributionService.save(dto);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody BillDto<Distribution, DistributionDetail> dto) {
        distributionService.save(dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        distributionService.delete(id);
        return Result.ok();
    }

    // add by codebuddy 20260817: 确认配送收货单
    @PostMapping("/confirm/{id}")
    public Result<Void> confirm(@PathVariable Integer id) {
        distributionService.confirm(id);
        return Result.ok();
    }

    // add by codebuddy 20260817: 提交配送收货单
    @PostMapping("/submit/{id}")
    public Result<Void> submit(@PathVariable Integer id) {
        distributionService.submit(id);
        return Result.ok();
    }

    // add by codebuddy 20260819: 手动上传单张配送收货单到 ERP（return.create）
    @PostMapping("/upload/{id}")
    public Result<Void> upload(@PathVariable Integer id) {
        Distribution d = distributionService.getById(id).getHeader();
        if (d == null) {
            return Result.fail("配送收货单不存在");
        }
        distributionService.uploadOneToErp(d);
        return Result.ok();
    }
}

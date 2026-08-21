package com.bwtst.controller;

import com.bwtst.common.PageResult;
import com.bwtst.common.Result;
import com.bwtst.entity.DistributionCenter;
import com.bwtst.service.DistributionCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// add by codebuddy 20260817
@RestController
@RequestMapping("/api/distribution-center")
@RequiredArgsConstructor
public class DistributionCenterController {

    private final DistributionCenterService distributionCenterService;

    @GetMapping("/page")
    public Result<PageResult<DistributionCenter>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                       @RequestParam(defaultValue = "10") long pageSize,
                                                       @RequestParam(required = false) String keyword) {
        return Result.ok(distributionCenterService.page(pageNum, pageSize, keyword));
    }

    @GetMapping("/list")
    public Result<List<DistributionCenter>> list() {
        return Result.ok(distributionCenterService.list());
    }

    @GetMapping("/{id}")
    public Result<DistributionCenter> getById(@PathVariable Integer id) {
        return Result.ok(distributionCenterService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody DistributionCenter dc) {
        distributionCenterService.save(dc);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody DistributionCenter dc) {
        distributionCenterService.save(dc);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        distributionCenterService.delete(id);
        return Result.ok();
    }
}

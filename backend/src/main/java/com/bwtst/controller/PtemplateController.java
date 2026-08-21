package com.bwtst.controller;

import com.bwtst.common.PageResult;
import com.bwtst.common.Result;
import com.bwtst.entity.Ptemplate;
import com.bwtst.entity.PtemplateDto;
import com.bwtst.service.PtemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// add by codebuddy 20260817
@RestController
@RequestMapping("/api/ptemplate")
@RequiredArgsConstructor
public class PtemplateController {

    private final PtemplateService ptemplateService;

    @GetMapping("/page")
    public Result<PageResult<Ptemplate>> page(@RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize,
                                              @RequestParam(required = false) String keyword) {
        return Result.ok(ptemplateService.page(pageNum, pageSize, keyword));
    }

    @GetMapping("/list")
    public Result<List<Ptemplate>> list() {
        return Result.ok(ptemplateService.list());
    }

    @GetMapping("/listByShop")
    public Result<List<Ptemplate>> listByShop(@RequestParam(required = false) Integer shopId) {
        return Result.ok(ptemplateService.listByShop(shopId));
    }

    @GetMapping("/{id}")
    public Result<PtemplateDto> getById(@PathVariable Integer id) {
        return Result.ok(ptemplateService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody PtemplateDto dto) {
        ptemplateService.save(dto);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody PtemplateDto dto) {
        ptemplateService.save(dto);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        ptemplateService.delete(id);
        return Result.ok();
    }
}

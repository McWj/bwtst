package com.bwtst.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bwtst.common.Result;
import com.bwtst.entity.SupplierPrice;
import com.bwtst.service.SupplierPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/supplier-price")
@RequiredArgsConstructor
public class SupplierPriceController {

    private final SupplierPriceService supplierPriceService;

    @GetMapping("/page")
    public Result<IPage<SupplierPrice>> page(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) Integer supplierId,
                                             @RequestParam(required = false) Integer itemId) {
        return Result.ok(supplierPriceService.page(page, size, supplierId, itemId));
    }

    @GetMapping("/{id}")
    public Result<SupplierPrice> get(@PathVariable Integer id) {
        return Result.ok(supplierPriceService.getById(id));
    }

    @PostMapping("/save")
    public Result<Void> save(@RequestBody SupplierPrice p) {
        supplierPriceService.save(p);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        supplierPriceService.delete(id);
        return Result.ok();
    }

    @GetMapping("/all")
    public Result<List<SupplierPrice>> all() {
        return Result.ok(supplierPriceService.all());
    }
}

package com.bwtst.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bwtst.common.Result;
import com.bwtst.entity.Supplier;
import com.bwtst.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/supplier")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping("/page")
    public Result<IPage<Supplier>> page(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "10") int size,
                                        @RequestParam(required = false) String keyword) {
        return Result.ok(supplierService.page(page, size, keyword));
    }

    @GetMapping("/{id}")
    public Result<Supplier> get(@PathVariable Integer id) {
        return Result.ok(supplierService.getById(id));
    }

    @PostMapping("/save")
    public Result<Void> save(@RequestBody Supplier s) {
        supplierService.save(s);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        supplierService.delete(id);
        return Result.ok();
    }

    @GetMapping("/options")
    public Result<List<Supplier>> options() {
        return Result.ok(supplierService.all());
    }
}

package com.bwtst.controller;

import com.bwtst.common.PageResult;
import com.bwtst.common.Result;
import com.bwtst.entity.GoodsShop;
import com.bwtst.service.GoodsShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// add by codebuddy 20260817
@RestController
@RequestMapping("/api/goods-shop")
@RequiredArgsConstructor
public class GoodsShopController {

    private final GoodsShopService goodsShopService;

    /** 根据 CSV 批量更新门店商品价格 */
    @PostMapping("/import-price")
    public Result<Map<String, Object>> importPrice(@RequestParam(required = false) String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            filePath = "D:/CodeBuddyWork/bwtst/门店商品价格.csv";
        }
        return Result.ok(goodsShopService.importPriceFromCsv(filePath));
    }

    // add by codebuddy 20260817: 根据 CSV 将门店商品与主供应商绑定
    /** 根据 CSV 将门店商品与主供应商绑定 */
    @PostMapping("/import-supplier")
    public Result<Map<String, Object>> importSupplier(@RequestParam(required = false) String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            filePath = "D:/CodeBuddyWork/bwtst/主供应商.csv";
        }
        return Result.ok(goodsShopService.importSupplierFromCsv(filePath));
    }

    @GetMapping("/page")
    public Result<PageResult<GoodsShop>> page(@RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize,
                                              @RequestParam(required = false) Integer shopId,
                                              @RequestParam(required = false) Integer itemId) {
        return Result.ok(goodsShopService.page(pageNum, pageSize, shopId, itemId));
    }

    @GetMapping("/list")
    public Result<List<GoodsShop>> list(@RequestParam(required = false) Integer shopId) {
        return Result.ok(goodsShopService.listByShop(shopId));
    }

    @GetMapping("/{id}")
    public Result<GoodsShop> getById(@PathVariable Integer id) {
        return Result.ok(goodsShopService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody GoodsShop goodsShop) {
        goodsShopService.save(goodsShop);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody GoodsShop goodsShop) {
        goodsShopService.save(goodsShop);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        goodsShopService.delete(id);
        return Result.ok();
    }
}

package com.bwtst.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bwtst.common.BusinessException;
import com.bwtst.common.PageResult;
import com.bwtst.entity.DistributionCenter;
import com.bwtst.entity.Goods;
import com.bwtst.entity.GoodsShop;
import com.bwtst.entity.Shop;
import com.bwtst.entity.Supplier;
import com.bwtst.mapper.DistributionCenterMapper;
import com.bwtst.mapper.GoodsMapper;
import com.bwtst.mapper.GoodsShopMapper;
import com.bwtst.mapper.ShopMapper;
import com.bwtst.mapper.SupplierMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// add by codebuddy 20260817
@Service
@RequiredArgsConstructor
public class GoodsShopService {

    private final GoodsShopMapper goodsShopMapper;
    private final ShopMapper shopMapper;
    private final GoodsMapper goodsMapper;
    private final DistributionCenterMapper dcMapper;
    private final SupplierMapper supplierMapper;
    private final JdbcTemplate jdbcTemplate;

    public PageResult<GoodsShop> page(long pageNum, long pageSize, Integer shopId, Integer itemId) {
        LambdaQueryWrapper<GoodsShop> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(shopId != null, GoodsShop::getShopId, shopId)
                .eq(itemId != null, GoodsShop::getItemId, itemId)
                .orderByAsc(GoodsShop::getId);
        Page<GoodsShop> p = goodsShopMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        fillNames(p.getRecords());
        return new PageResult<>(p.getRecords(), p.getTotal(), pageNum, pageSize);
    }

    public List<GoodsShop> listByShop(Integer shopId) {
        LambdaQueryWrapper<GoodsShop> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(shopId != null, GoodsShop::getShopId, shopId).orderByAsc(GoodsShop::getId);
        List<GoodsShop> list = goodsShopMapper.selectList(wrapper);
        fillNames(list);
        return list;
    }

    public GoodsShop getById(Integer id) {
        GoodsShop gs = goodsShopMapper.selectById(id);
        if (gs != null) {
            fillNames(Collections.singletonList(gs));
        }
        return gs;
    }

    public void save(GoodsShop goodsShop) {
        if (goodsShop.getShopId() == null || goodsShop.getItemId() == null) {
            throw new BusinessException("门店和商品不能为空");
        }
        // 同一门店下不允许重复商品
        LambdaQueryWrapper<GoodsShop> w = new LambdaQueryWrapper<>();
        w.eq(GoodsShop::getShopId, goodsShop.getShopId())
                .eq(GoodsShop::getItemId, goodsShop.getItemId())
                .ne(goodsShop.getId() != null, GoodsShop::getId, goodsShop.getId());
        if (goodsShopMapper.selectCount(w) > 0) {
            throw new BusinessException("该门店已存在此商品配置");
        }
        if (goodsShop.getId() == null) {
            goodsShopMapper.insert(goodsShop);
        } else {
            goodsShopMapper.updateById(goodsShop);
        }
    }

    public void delete(Integer id) {
        goodsShopMapper.deleteById(id);
    }

    // add by codebuddy 20260817: 根据 CSV 文件批量更新门店商品价格
    public Map<String, Object> importPriceFromCsv(String filePath) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new BusinessException("文件不存在: " + path.toAbsolutePath());
        }
        // 门店编码 -> id、商品编号 -> id
        Map<String, Integer> shopIdMap = shopMapper.selectList(null).stream()
                .collect(Collectors.toMap(Shop::getShopNo, Shop::getId, (a, b) -> a));
        Map<String, Integer> itemIdMap = goodsMapper.selectList(null).stream()
                .collect(Collectors.toMap(Goods::getItemNo, Goods::getId, (a, b) -> a));

        List<Object[]> batch = new ArrayList<>();
        int total = 0, unmatchedShop = 0, unmatchedItem = 0;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                if (header) {
                    header = false;
                    continue; // 跳过表头
                }
                List<String> cols = parseCsvLine(line);
                if (cols.size() < 5) {
                    continue;
                }
                String shopNo = cols.get(0).trim();
                String itemNo = cols.get(2).trim();
                String priceStr = cols.get(4).trim();
                Integer shopId = shopIdMap.get(shopNo);
                Integer itemId = itemIdMap.get(itemNo);
                if (shopId == null) {
                    unmatchedShop++;
                    continue;
                }
                if (itemId == null) {
                    unmatchedItem++;
                    continue;
                }
                BigDecimal price;
                try {
                    price = new BigDecimal(priceStr);
                } catch (NumberFormatException e) {
                    continue; // 价格格式错误跳过
                }
                batch.add(new Object[]{price, shopId, itemId});
                total++;
            }
        } catch (java.io.IOException e) {
            throw new BusinessException("读取CSV失败: " + e.getMessage());
        }

        // 分批批量更新，避免单条逐次执行过慢
        String sql = "UPDATE goods_shop SET price = ? WHERE shop_id = ? AND item_id = ?";
        int updated = 0;
        for (int i = 0; i < batch.size(); i += 1000) {
            List<Object[]> sub = batch.subList(i, Math.min(i + 1000, batch.size()));
            for (int c : jdbcTemplate.batchUpdate(sql, sub)) {
                updated += c;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("updated", updated);
        result.put("unmatchedShop", unmatchedShop);
        result.put("unmatchedItem", unmatchedItem);
        return result;
    }

    /** 解析 CSV 行，支持双引号包裹字段（内含逗号） */
    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
            }
        }
        fields.add(sb.toString());
        return fields;
    }

    private void fillNames(List<GoodsShop> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Integer> shopIds = list.stream().map(GoodsShop::getShopId).distinct().collect(Collectors.toList());
        List<Integer> itemIds = list.stream().map(GoodsShop::getItemId).distinct().collect(Collectors.toList());
        List<Integer> dcIds = list.stream().map(GoodsShop::getDeliveryCenterId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        // add by codebuddy 20260817: 主供应商id -> 供应商信息
        List<Integer> supplierIds = list.stream().map(GoodsShop::getSupplierId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Integer, Shop> shopMap = shopMapper.selectBatchIds(shopIds).stream()
                .collect(Collectors.toMap(Shop::getId, Function.identity()));
        Map<Integer, Goods> goodsMap = goodsMapper.selectBatchIds(itemIds).stream()
                .collect(Collectors.toMap(Goods::getId, Function.identity()));
        Map<Integer, DistributionCenter> dcMap = dcIds.isEmpty() ? Collections.emptyMap()
                : dcMapper.selectBatchIds(dcIds).stream()
                .collect(Collectors.toMap(DistributionCenter::getId, Function.identity()));
        Map<Integer, Supplier> supplierMap = supplierIds.isEmpty() ? Collections.emptyMap()
                : supplierMapper.selectBatchIds(supplierIds).stream()
                .collect(Collectors.toMap(Supplier::getId, Function.identity()));
        for (GoodsShop gs : list) {
            Shop s = shopMap.get(gs.getShopId());
            Goods g = goodsMap.get(gs.getItemId());
            DistributionCenter dc = dcMap.get(gs.getDeliveryCenterId());
            Supplier supplier = supplierMap.get(gs.getSupplierId());
            if (s != null) {
                gs.setShopName(s.getShopName());
            }
            if (g != null) {
                gs.setItemNo(g.getItemNo());
                gs.setItemName(g.getItemName());
            }
            if (dc != null) {
                gs.setDeliveryCenterNo(dc.getDcNo());
                gs.setDeliveryCenterName(dc.getDcName());
            }
            if (supplier != null) {
                gs.setSupplierNo(supplier.getSupplierCode());
                gs.setSupplierName(supplier.getSupplierName());
            }
        }
    }

    // add by codebuddy 20260817: 根据 CSV 文件将门店商品与主供应商绑定
    // CSV 列：门店编号, 商品编码, 主供应商编号
    public Map<String, Object> importSupplierFromCsv(String filePath) {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new BusinessException("文件不存在: " + path.toAbsolutePath());
        }
        Map<String, Integer> shopIdMap = shopMapper.selectList(null).stream()
                .collect(Collectors.toMap(Shop::getShopNo, Shop::getId, (a, b) -> a));
        Map<String, Integer> itemIdMap = goodsMapper.selectList(null).stream()
                .collect(Collectors.toMap(Goods::getItemNo, Goods::getId, (a, b) -> a));
        Map<String, Integer> supplierIdMap = supplierMapper.selectList(null).stream()
                .collect(Collectors.toMap(Supplier::getSupplierCode, Supplier::getId, (a, b) -> a));

        List<Object[]> batch = new ArrayList<>();
        int total = 0, unmatchedShop = 0, unmatchedItem = 0, unmatchedSupplier = 0;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                if (header) {
                    header = false;
                    continue; // 跳过表头
                }
                List<String> cols = parseCsvLine(line);
                if (cols.size() < 3) {
                    continue;
                }
                String shopNo = cols.get(0).trim();
                String itemNo = cols.get(1).trim();
                String supplierCode = cols.get(2).trim();
                Integer shopId = shopIdMap.get(shopNo);
                Integer itemId = itemIdMap.get(itemNo);
                Integer supplierId = supplierIdMap.get(supplierCode);
                if (shopId == null) {
                    unmatchedShop++;
                    continue;
                }
                if (itemId == null) {
                    unmatchedItem++;
                    continue;
                }
                if (supplierId == null) {
                    unmatchedSupplier++;
                    continue;
                }
                batch.add(new Object[]{supplierId, shopId, itemId});
                total++;
            }
        } catch (java.io.IOException e) {
            throw new BusinessException("读取CSV失败: " + e.getMessage());
        }

        String sql = "UPDATE goods_shop SET supplier_id = ? WHERE shop_id = ? AND item_id = ?";
        int updated = 0;
        for (int i = 0; i < batch.size(); i += 1000) {
            List<Object[]> sub = batch.subList(i, Math.min(i + 1000, batch.size()));
            for (int c : jdbcTemplate.batchUpdate(sql, sub)) {
                updated += c;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("updated", updated);
        result.put("unmatchedShop", unmatchedShop);
        result.put("unmatchedItem", unmatchedItem);
        result.put("unmatchedSupplier", unmatchedSupplier);
        return result;
    }
}

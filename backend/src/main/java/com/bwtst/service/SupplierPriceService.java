package com.bwtst.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bwtst.common.BusinessException;
import com.bwtst.entity.Goods;
import com.bwtst.entity.Supplier;
import com.bwtst.entity.SupplierPrice;
import com.bwtst.mapper.GoodsMapper;
import com.bwtst.mapper.SupplierMapper;
import com.bwtst.mapper.SupplierPriceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import java.util.List;

// add by codebuddy 20260819
@Service
@RequiredArgsConstructor
public class SupplierPriceService {

    private final SupplierPriceMapper priceMapper;
    private final SupplierMapper supplierMapper;
    private final GoodsMapper goodsMapper;

    public IPage<SupplierPrice> page(int page, int size, Integer supplierId, Integer itemId) {
        LambdaQueryWrapper<SupplierPrice> w = new LambdaQueryWrapper<>();
        if (supplierId != null) {
            w.eq(SupplierPrice::getSupplierId, supplierId);
        }
        if (itemId != null) {
            w.eq(SupplierPrice::getItemId, itemId);
        }
        w.orderByDesc(SupplierPrice::getId);
        return priceMapper.selectPage(new Page<>(page, size), w);
    }

    public SupplierPrice getById(Integer id) {
        return priceMapper.selectById(id);
    }

    public void save(SupplierPrice p) {
        if (p.getSupplierId() == null || p.getItemId() == null) {
            throw new BusinessException("供应商和商品不能为空");
        }
        Goods g = goodsMapper.selectById(p.getItemId());
        if (g == null) {
            throw new BusinessException("商品不存在");
        }
        Supplier s = supplierMapper.selectById(p.getSupplierId());
        if (s == null) {
            throw new BusinessException("供应商不存在");
        }
        // 唯一性校验：同一供应商+商品只允许一条价格
        LambdaQueryWrapper<SupplierPrice> w = new LambdaQueryWrapper<>();
        w.eq(SupplierPrice::getSupplierId, p.getSupplierId())
                .eq(SupplierPrice::getItemId, p.getItemId());
        if (p.getId() != null) {
            w.ne(SupplierPrice::getId, p.getId());
        }
        if (priceMapper.selectCount(w) > 0) {
            throw new BusinessException("该供应商下此商品的价格已存在");
        }
        if (p.getId() == null) {
            priceMapper.insert(p);
        } else {
            priceMapper.updateById(p);
        }
    }

    public void delete(Integer id) {
        priceMapper.deleteById(id);
    }

    /** 取某供应商+商品的单价，未配置返回 null */
    public BigDecimal getPrice(Integer supplierId, Integer itemId) {
        LambdaQueryWrapper<SupplierPrice> w = new LambdaQueryWrapper<>();
        w.eq(SupplierPrice::getSupplierId, supplierId)
                .eq(SupplierPrice::getItemId, itemId)
                .eq(SupplierPrice::getStatus, "1")
                .last("LIMIT 1");
        SupplierPrice p = priceMapper.selectOne(w);
        return p == null ? null : p.getPrice();
    }

    public List<SupplierPrice> all() {
        return priceMapper.selectList(new LambdaQueryWrapper<SupplierPrice>()
                .eq(SupplierPrice::getStatus, "1")
                .orderByAsc(SupplierPrice::getId));
    }
}

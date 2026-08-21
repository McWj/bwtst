package com.bwtst.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bwtst.entity.Supplier;
import com.bwtst.mapper.SupplierMapper;
import com.bwtst.mapper.GoodsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// add by codebuddy 20260819
@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierMapper supplierMapper;
    private final GoodsMapper goodsMapper;

    public IPage<Supplier> page(int page, int size, String keyword) {
        LambdaQueryWrapper<Supplier> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.like(Supplier::getSupplierCode, keyword)
                    .or().like(Supplier::getSupplierName, keyword);
        }
        w.orderByDesc(Supplier::getId);
        return supplierMapper.selectPage(new Page<>(page, size), w);
    }

    public Supplier getById(Integer id) {
        return supplierMapper.selectById(id);
    }

    public void save(Supplier s) {
        if (s.getId() == null) {
            supplierMapper.insert(s);
        } else {
            supplierMapper.updateById(s);
        }
    }

    public void delete(Integer id) {
        supplierMapper.deleteById(id);
    }

    public List<Supplier> all() {
        return supplierMapper.selectList(new LambdaQueryWrapper<Supplier>()
                .eq(Supplier::getStatus, "1")
                .orderByAsc(Supplier::getSupplierCode));
    }
}

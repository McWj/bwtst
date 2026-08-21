package com.bwtst.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bwtst.common.BusinessException;
import com.bwtst.common.PageResult;
import com.bwtst.entity.Shop;
import com.bwtst.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

// add by codebuddy 20260817
@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopMapper shopMapper;

    public PageResult<Shop> page(long pageNum, long pageSize, String keyword) {
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(keyword), w -> w
                        .like(Shop::getShopNo, keyword)
                        .or().like(Shop::getShopName, keyword))
                .orderByAsc(Shop::getId);
        Page<Shop> p = shopMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return new PageResult<>(p.getRecords(), p.getTotal(), pageNum, pageSize);
    }

    public List<Shop> list() {
        return shopMapper.selectList(new LambdaQueryWrapper<Shop>().orderByAsc(Shop::getId));
    }

    public Shop getById(Integer id) {
        return shopMapper.selectById(id);
    }

    public void save(Shop shop) {
        if (StringUtils.hasText(shop.getShopNo()) && existsByNo(shop.getShopNo(), null)) {
            throw new BusinessException("门店编号已存在：" + shop.getShopNo());
        }
        shopMapper.insert(shop);
    }

    public void update(Shop shop) {
        if (shop.getId() == null) {
            throw new BusinessException("缺少主键id");
        }
        if (StringUtils.hasText(shop.getShopNo()) && existsByNo(shop.getShopNo(), shop.getId())) {
            throw new BusinessException("门店编号已存在：" + shop.getShopNo());
        }
        shopMapper.updateById(shop);
    }

    public void delete(Integer id) {
        shopMapper.deleteById(id);
    }

    private boolean existsByNo(String no, Integer excludeId) {
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Shop::getShopNo, no)
                .ne(excludeId != null, Shop::getId, excludeId);
        return shopMapper.selectCount(wrapper) > 0;
    }
}

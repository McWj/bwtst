package com.bwtst.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bwtst.common.BusinessException;
import com.bwtst.common.PageResult;
import com.bwtst.entity.Goods;
import com.bwtst.mapper.GoodsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

// add by codebuddy 20260817
@Service
@RequiredArgsConstructor
public class GoodsService {

    private final GoodsMapper goodsMapper;

    public PageResult<Goods> page(long pageNum, long pageSize, String keyword) {
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(keyword), w -> w
                        .like(Goods::getItemNo, keyword)
                        .or().like(Goods::getItemName, keyword))
                .orderByAsc(Goods::getId);
        Page<Goods> p = goodsMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return new PageResult<>(p.getRecords(), p.getTotal(), pageNum, pageSize);
    }

    public List<Goods> list() {
        return goodsMapper.selectList(new LambdaQueryWrapper<Goods>().orderByAsc(Goods::getId));
    }

    public Goods getById(Integer id) {
        return goodsMapper.selectById(id);
    }

    public void save(Goods goods) {
        if (StringUtils.hasText(goods.getItemNo()) && existsByNo(goods.getItemNo(), null)) {
            throw new BusinessException("商品编号已存在：" + goods.getItemNo());
        }
        goodsMapper.insert(goods);
    }

    public void update(Goods goods) {
        if (goods.getId() == null) {
            throw new BusinessException("缺少主键id");
        }
        if (StringUtils.hasText(goods.getItemNo()) && existsByNo(goods.getItemNo(), goods.getId())) {
            throw new BusinessException("商品编号已存在：" + goods.getItemNo());
        }
        goodsMapper.updateById(goods);
    }

    public void delete(Integer id) {
        goodsMapper.deleteById(id);
    }

    private boolean existsByNo(String no, Integer excludeId) {
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Goods::getItemNo, no)
                .ne(excludeId != null, Goods::getId, excludeId);
        return goodsMapper.selectCount(wrapper) > 0;
    }
}

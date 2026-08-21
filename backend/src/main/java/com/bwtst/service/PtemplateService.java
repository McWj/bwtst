package com.bwtst.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bwtst.common.CodeGenerator;
import com.bwtst.common.PageResult;
import com.bwtst.entity.Goods;
import com.bwtst.entity.Ptemplate;
import com.bwtst.entity.PtemplateDto;
import com.bwtst.entity.PtemplateGoods;
import com.bwtst.entity.PtemplateShop;
import com.bwtst.entity.Shop;
import com.bwtst.mapper.GoodsMapper;
import com.bwtst.mapper.PtemplateGoodsMapper;
import com.bwtst.mapper.PtemplateMapper;
import com.bwtst.mapper.PtemplateShopMapper;
import com.bwtst.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// add by codebuddy 20260817
@Service
@RequiredArgsConstructor
public class PtemplateService {

    private final PtemplateMapper ptemplateMapper;
    private final PtemplateGoodsMapper goodsMapper;
    private final PtemplateShopMapper shopMapper;
    private final GoodsMapper goodsMasterMapper;
    private final ShopMapper shopMasterMapper;

    public PageResult<Ptemplate> page(long pageNum, long pageSize, String keyword) {
        LambdaQueryWrapper<Ptemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(keyword), w -> w
                        .like(Ptemplate::getPtemplateNo, keyword)
                        .or().like(Ptemplate::getPtemplateName, keyword))
                .orderByAsc(Ptemplate::getId);
        Page<Ptemplate> p = ptemplateMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return new PageResult<>(p.getRecords(), p.getTotal(), pageNum, pageSize);
    }

    public List<Ptemplate> list() {
        return ptemplateMapper.selectList(new LambdaQueryWrapper<Ptemplate>().orderByAsc(Ptemplate::getId));
    }

    /** 查询适用某门店的模板列表（通过模板-门店明细关联） */
    public List<Ptemplate> listByShop(Integer shopId) {
        if (shopId == null) {
            return java.util.Collections.emptyList();
        }
        List<Integer> templateIds = shopMapper.selectList(new LambdaQueryWrapper<PtemplateShop>()
                        .eq(PtemplateShop::getShopId, shopId))
                .stream().map(PtemplateShop::getPtemplateId).distinct().collect(Collectors.toList());
        if (templateIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return ptemplateMapper.selectList(new LambdaQueryWrapper<Ptemplate>()
                .in(Ptemplate::getId, templateIds)
                .orderByAsc(Ptemplate::getId));
    }

    /** 查询模板及其商品明细、门店明细 */
    public PtemplateDto getById(Integer id) {
        Ptemplate header = ptemplateMapper.selectById(id);
        if (header == null) {
            return null;
        }
        PtemplateDto dto = new PtemplateDto();
        dto.setHeader(header);
        dto.setGoodsList(fillGoodsNames(loadGoods(id)));
        dto.setShopList(fillShopNames(loadShops(id)));
        return dto;
    }

    /** 保存模板（新增或修改），商品与门店明细采用先删后插策略 */
    @Transactional(rollbackFor = Exception.class)
    public void save(PtemplateDto dto) {
        Ptemplate header = dto.getHeader();
        if (header.getId() == null) {
            if (!StringUtils.hasText(header.getPtemplateNo())) {
                header.setPtemplateNo(CodeGenerator.gen("PT"));
            }
            ptemplateMapper.insert(header);
        } else {
            ptemplateMapper.updateById(header);
        }

        // 商品明细：先删后插
        goodsMapper.delete(new LambdaQueryWrapper<PtemplateGoods>()
                .eq(PtemplateGoods::getPtemplateId, header.getId()));
        List<PtemplateGoods> goodsList = dto.getGoodsList();
        if (goodsList != null) {
            int seq = 1;
            for (PtemplateGoods g : goodsList) {
                g.setId(null);
                g.setPtemplateId(header.getId());
                if (g.getSeq() == null) {
                    g.setSeq(seq++);
                }
                goodsMapper.insert(g);
            }
        }

        // 门店明细：先删后插
        shopMapper.delete(new LambdaQueryWrapper<PtemplateShop>()
                .eq(PtemplateShop::getPtemplateId, header.getId()));
        List<PtemplateShop> shopList = dto.getShopList();
        if (shopList != null) {
            int seq = 1;
            for (PtemplateShop s : shopList) {
                s.setId(null);
                s.setPtemplateId(header.getId());
                if (s.getSeq() == null) {
                    s.setSeq(seq++);
                }
                shopMapper.insert(s);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        goodsMapper.delete(new LambdaQueryWrapper<PtemplateGoods>()
                .eq(PtemplateGoods::getPtemplateId, id));
        shopMapper.delete(new LambdaQueryWrapper<PtemplateShop>()
                .eq(PtemplateShop::getPtemplateId, id));
        ptemplateMapper.deleteById(id);
    }

    private List<PtemplateGoods> loadGoods(Integer templateId) {
        return goodsMapper.selectList(new LambdaQueryWrapper<PtemplateGoods>()
                .eq(PtemplateGoods::getPtemplateId, templateId)
                .orderByAsc(PtemplateGoods::getSeq, PtemplateGoods::getId));
    }

    private List<PtemplateShop> loadShops(Integer templateId) {
        return shopMapper.selectList(new LambdaQueryWrapper<PtemplateShop>()
                .eq(PtemplateShop::getPtemplateId, templateId)
                .orderByAsc(PtemplateShop::getSeq, PtemplateShop::getId));
    }

    private List<PtemplateGoods> fillGoodsNames(List<PtemplateGoods> goods) {
        if (goods == null || goods.isEmpty()) {
            return goods;
        }
        List<Integer> ids = goods.stream().map(PtemplateGoods::getItemId).distinct().collect(Collectors.toList());
        Map<Integer, Goods> goodsMap = goodsMasterMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Goods::getId, Function.identity()));
        goods.forEach(g -> {
            Goods m = goodsMap.get(g.getItemId());
            if (m != null) {
                g.setItemNo(m.getItemNo());
                g.setItemName(m.getItemName());
            }
        });
        return goods;
    }

    private List<PtemplateShop> fillShopNames(List<PtemplateShop> shops) {
        if (shops == null || shops.isEmpty()) {
            return shops;
        }
        List<Integer> ids = shops.stream().map(PtemplateShop::getShopId).distinct().collect(Collectors.toList());
        Map<Integer, Shop> shopMap = shopMasterMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Shop::getId, Function.identity()));
        shops.forEach(s -> {
            Shop m = shopMap.get(s.getShopId());
            if (m != null) {
                s.setShopName(m.getShopName());
            }
        });
        return shops;
    }
}

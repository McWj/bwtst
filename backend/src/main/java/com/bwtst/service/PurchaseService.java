package com.bwtst.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bwtst.common.BillDto;
import com.bwtst.common.BusinessException;
import com.bwtst.common.CodeGenerator;
import com.bwtst.common.PageResult;
import com.bwtst.entity.Goods;
import com.bwtst.entity.Purchase;
import com.bwtst.entity.PurchaseDetail;
import com.bwtst.entity.Shop;
import com.bwtst.mapper.GoodsMapper;
import com.bwtst.mapper.PurchaseDetailMapper;
import com.bwtst.mapper.PurchaseMapper;
import com.bwtst.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// add by codebuddy 20260817
@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseMapper purchaseMapper;
    private final PurchaseDetailMapper detailMapper;
    private final ShopMapper shopMapper;
    private final GoodsMapper goodsMapper;

    public PageResult<Purchase> page(long pageNum, long pageSize, String keyword, Integer siteId) {
        LambdaQueryWrapper<Purchase> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(keyword), w -> w
                        .like(Purchase::getPurchaseNo, keyword)
                        .or().like(Purchase::getErpNo, keyword)
                        .or().like(Purchase::getCreator, keyword))
                .eq(siteId != null, Purchase::getSiteId, siteId)
                .orderByDesc(Purchase::getId);
        Page<Purchase> p = purchaseMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        fillShopNames(p.getRecords());
        return new PageResult<>(p.getRecords(), p.getTotal(), pageNum, pageSize);
    }

    public BillDto<Purchase, PurchaseDetail> getById(Integer id) {
        Purchase header = purchaseMapper.selectById(id);
        if (header == null) {
            return null;
        }
        BillDto<Purchase, PurchaseDetail> dto = new BillDto<>();
        dto.setHeader(header);
        fillShopNames(java.util.Collections.singletonList(header));
        dto.setDetails(fillGoodsNames(detailMapper.selectList(new LambdaQueryWrapper<PurchaseDetail>()
                .eq(PurchaseDetail::getPurchaseId, header.getPurchaseNo())
                .orderByAsc(PurchaseDetail::getSeq, PurchaseDetail::getId))));
        return dto;
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(BillDto<Purchase, PurchaseDetail> dto) {
        Purchase header = dto.getHeader();
        if (header.getId() == null) {
            // add by codebuddy 20260817: 创建时间默认为当前时间
            if (header.getCreateDatetime() == null) {
                header.setCreateDatetime(LocalDateTime.now());
            }
            if (!StringUtils.hasText(header.getPurchaseNo())) {
                header.setPurchaseNo(nextNo(header.getSiteId()));
            }
            purchaseMapper.insert(header);
        } else {
            purchaseMapper.updateById(header);
        }

        detailMapper.delete(new LambdaQueryWrapper<PurchaseDetail>()
                .eq(PurchaseDetail::getPurchaseId, header.getPurchaseNo()));
        List<PurchaseDetail> details = dto.getDetails();
        if (details != null) {
            int seq = 1;
            for (PurchaseDetail d : details) {
                d.setId(null);
                d.setPurchaseId(header.getPurchaseNo());
                if (d.getSeq() == null) {
                    d.setSeq(seq++);
                }
                detailMapper.insert(d);
            }
        }
    }

    // add by codebuddy 20260817: 确认采购收货单，状态置为Y
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Integer id) {
        Purchase header = purchaseMapper.selectById(id);
        if (header == null) {
            throw new BusinessException("采购收货单不存在");
        }
        header.setStatus("Y");
        purchaseMapper.updateById(header);
    }

    // add by codebuddy 20260817: 提交采购收货单（需先确认），状态置为TJ
    @Transactional(rollbackFor = Exception.class)
    public void submit(Integer id) {
        Purchase header = purchaseMapper.selectById(id);
        if (header == null) {
            throw new BusinessException("采购收货单不存在");
        }
        if ("TJ".equals(header.getStatus())) {
            throw new BusinessException("采购收货单已提交，不能重复提交");
        }
        if (!"Y".equals(header.getStatus())) {
            throw new BusinessException("请先确认采购收货单后再提交");
        }
        header.setStatus("TJ");
        purchaseMapper.updateById(header);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        Purchase header = purchaseMapper.selectById(id);
        if (header != null) {
            detailMapper.delete(new LambdaQueryWrapper<PurchaseDetail>()
                    .eq(PurchaseDetail::getPurchaseId, header.getPurchaseNo()));
        }
        purchaseMapper.deleteById(id);
    }

    private static final String NO_PREFIX = "CGSH";

    // add by codebuddy 20260817: 单号前缀加入门店编号
    /** 生成采购收货单号：CGSH + 门店编号 + 年月日 + 三位流水号，例如 CGSHSH000120260817001 */
    private String nextNo(Integer siteId) {
        String shopNo = getShopNo(siteId);
        String prefix = NO_PREFIX + shopNo + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long count = purchaseMapper.selectCount(new LambdaQueryWrapper<Purchase>()
                .likeRight(Purchase::getPurchaseNo, prefix));
        return CodeGenerator.genDaily(NO_PREFIX + shopNo, count.intValue());
    }

    private String getShopNo(Integer siteId) {
        if (siteId == null) {
            return "";
        }
        Shop shop = shopMapper.selectById(siteId);
        return shop == null ? "" : shop.getShopNo();
    }

    private void fillShopNames(List<Purchase> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Integer> ids = list.stream().map(Purchase::getSiteId).filter(java.util.Objects::nonNull)
                .distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return;
        }
        Map<Integer, Shop> shopMap = shopMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(Shop::getId, Function.identity()));
        list.forEach(r -> {
            Shop s = shopMap.get(r.getSiteId());
            if (s != null) {
                r.setSiteName(s.getShopName());
            }
        });
    }

    private List<PurchaseDetail> fillGoodsNames(List<PurchaseDetail> details) {
        if (details == null || details.isEmpty()) {
            return details;
        }
        List<Integer> ids = details.stream().map(PurchaseDetail::getItemId).filter(java.util.Objects::nonNull)
                .distinct().collect(Collectors.toList());
        if (!ids.isEmpty()) {
            Map<Integer, Goods> goodsMap = goodsMapper.selectBatchIds(ids).stream()
                    .collect(Collectors.toMap(Goods::getId, Function.identity()));
            details.forEach(d -> {
                Goods g = goodsMap.get(d.getItemId());
                if (g != null) {
                    d.setItemNo(g.getItemNo());
                    d.setItemName(g.getItemName());
                }
            });
        }
        return details;
    }
}

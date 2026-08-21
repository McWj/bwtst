package com.bwtst.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bwtst.common.BillDto;
import com.bwtst.common.BusinessException;
import com.bwtst.common.CodeGenerator;
import com.bwtst.common.PageResult;
import com.bwtst.entity.Goods;
import com.bwtst.entity.PurchaseReturn;
import com.bwtst.entity.PurchaseReturnDetail;
import com.bwtst.entity.Shop;
import com.bwtst.mapper.GoodsMapper;
import com.bwtst.mapper.PurchaseReturnDetailMapper;
import com.bwtst.mapper.PurchaseReturnMapper;
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
public class PurchaseReturnService {

    private final PurchaseReturnMapper returnMapper;
    private final PurchaseReturnDetailMapper detailMapper;
    private final ShopMapper shopMapper;
    private final GoodsMapper goodsMapper;

    public PageResult<PurchaseReturn> page(long pageNum, long pageSize, String keyword, Integer siteId) {
        LambdaQueryWrapper<PurchaseReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(keyword), w -> w
                        .like(PurchaseReturn::getPurchaseReturnNo, keyword)
                        .or().like(PurchaseReturn::getErpNo, keyword)
                        .or().like(PurchaseReturn::getCreator, keyword))
                .eq(siteId != null, PurchaseReturn::getSiteId, siteId)
                .orderByDesc(PurchaseReturn::getId);
        Page<PurchaseReturn> p = returnMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        fillShopNames(p.getRecords());
        return new PageResult<>(p.getRecords(), p.getTotal(), pageNum, pageSize);
    }

    public BillDto<PurchaseReturn, PurchaseReturnDetail> getById(Integer id) {
        PurchaseReturn header = returnMapper.selectById(id);
        if (header == null) {
            return null;
        }
        BillDto<PurchaseReturn, PurchaseReturnDetail> dto = new BillDto<>();
        dto.setHeader(header);
        fillShopNames(java.util.Collections.singletonList(header));
        dto.setDetails(fillGoodsNames(detailMapper.selectList(new LambdaQueryWrapper<PurchaseReturnDetail>()
                .eq(PurchaseReturnDetail::getPurchaseReturnId, header.getPurchaseReturnNo())
                .orderByAsc(PurchaseReturnDetail::getSeq, PurchaseReturnDetail::getId))));
        return dto;
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(BillDto<PurchaseReturn, PurchaseReturnDetail> dto) {
        PurchaseReturn header = dto.getHeader();
        if (header.getId() == null) {
            // add by codebuddy 20260817: 创建时间默认为当前时间
            if (header.getCreateDatetime() == null) {
                header.setCreateDatetime(LocalDateTime.now());
            }
            if (!StringUtils.hasText(header.getPurchaseReturnNo())) {
                header.setPurchaseReturnNo(nextNo(header.getSiteId()));
            }
            returnMapper.insert(header);
        } else {
            returnMapper.updateById(header);
        }

        detailMapper.delete(new LambdaQueryWrapper<PurchaseReturnDetail>()
                .eq(PurchaseReturnDetail::getPurchaseReturnId, header.getPurchaseReturnNo()));
        List<PurchaseReturnDetail> details = dto.getDetails();
        if (details != null) {
            int seq = 1;
            for (PurchaseReturnDetail d : details) {
                d.setId(null);
                d.setPurchaseReturnId(header.getPurchaseReturnNo());
                if (d.getSeq() == null) {
                    d.setSeq(seq++);
                }
                detailMapper.insert(d);
            }
        }
    }

    // add by codebuddy 20260817: 确认采购退货单，状态置为Y
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Integer id) {
        PurchaseReturn header = returnMapper.selectById(id);
        if (header == null) {
            throw new BusinessException("采购退货单不存在");
        }
        header.setStatus("Y");
        returnMapper.updateById(header);
    }

    // add by codebuddy 20260817: 提交采购退货单（需先确认），状态置为TJ
    @Transactional(rollbackFor = Exception.class)
    public void submit(Integer id) {
        PurchaseReturn header = returnMapper.selectById(id);
        if (header == null) {
            throw new BusinessException("采购退货单不存在");
        }
        if ("TJ".equals(header.getStatus())) {
            throw new BusinessException("采购退货单已提交，不能重复提交");
        }
        if (!"Y".equals(header.getStatus())) {
            throw new BusinessException("请先确认采购退货单后再提交");
        }
        header.setStatus("TJ");
        returnMapper.updateById(header);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        PurchaseReturn header = returnMapper.selectById(id);
        if (header != null) {
            detailMapper.delete(new LambdaQueryWrapper<PurchaseReturnDetail>()
                    .eq(PurchaseReturnDetail::getPurchaseReturnId, header.getPurchaseReturnNo()));
        }
        returnMapper.deleteById(id);
    }

    private static final String NO_PREFIX = "CGTH";

    // add by codebuddy 20260817: 单号前缀加入门店编号
    /** 生成采购退货单号：CGTH + 门店编号 + 年月日 + 三位流水号，例如 CGTHSH000120260817001 */
    private String nextNo(Integer siteId) {
        String shopNo = getShopNo(siteId);
        String prefix = NO_PREFIX + shopNo + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long count = returnMapper.selectCount(new LambdaQueryWrapper<PurchaseReturn>()
                .likeRight(PurchaseReturn::getPurchaseReturnNo, prefix));
        return CodeGenerator.genDaily(NO_PREFIX + shopNo, count.intValue());
    }

    private String getShopNo(Integer siteId) {
        if (siteId == null) {
            return "";
        }
        Shop shop = shopMapper.selectById(siteId);
        return shop == null ? "" : shop.getShopNo();
    }

    private void fillShopNames(List<PurchaseReturn> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Integer> ids = list.stream().map(PurchaseReturn::getSiteId).filter(java.util.Objects::nonNull)
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

    private List<PurchaseReturnDetail> fillGoodsNames(List<PurchaseReturnDetail> details) {
        if (details == null || details.isEmpty()) {
            return details;
        }
        List<Integer> ids = details.stream().map(PurchaseReturnDetail::getItemId).filter(java.util.Objects::nonNull)
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

package com.bwtst.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bwtst.common.BillDto;
import com.bwtst.common.BusinessException;
import com.bwtst.common.CodeGenerator;
import com.bwtst.common.PageResult;
import com.bwtst.entity.Distribution;
import com.bwtst.entity.DistributionCenter;
import com.bwtst.entity.DistributionDetail;
import com.bwtst.common.ErpClient;
import com.bwtst.entity.DeliveryShipment;
import com.bwtst.entity.Goods;
import com.bwtst.entity.GoodsShop;
import com.bwtst.entity.Shop;
import com.bwtst.mapper.DeliveryShipmentMapper;
import com.bwtst.mapper.DistributionCenterMapper;
import com.bwtst.mapper.DistributionDetailMapper;
import com.bwtst.mapper.DistributionMapper;
import com.bwtst.mapper.GoodsMapper;
import com.bwtst.mapper.GoodsShopMapper;
import com.bwtst.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// add by codebuddy 20260817
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributionService {

    private final DistributionMapper distributionMapper;
    private final DistributionDetailMapper detailMapper;
    private final ShopMapper shopMapper;
    private final GoodsMapper goodsMapper;
    // add by codebuddy 20260817: 配送中心与门店商品配置
    private final DistributionCenterMapper dcMapper;
    private final GoodsShopMapper goodsShopMapper;
    // add by codebuddy 20260819: 配送收货单上传 ERP（transfer.update）调用 ERP
    private final ErpClient erpClient;
    // add by codebuddy 20260819: 上传时带出发货单的 ERP 配送单号
    private final DeliveryShipmentMapper shipmentMapper;

    public PageResult<Distribution> page(long pageNum, long pageSize, String keyword, Integer siteId) {
        LambdaQueryWrapper<Distribution> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(keyword), w -> w
                        .like(Distribution::getDistributionNo, keyword)
                        .or().like(Distribution::getErpNo, keyword)
                        .or().like(Distribution::getCreator, keyword))
                .eq(siteId != null, Distribution::getSiteId, siteId)
                .orderByDesc(Distribution::getId);
        Page<Distribution> p = distributionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        fillShopNames(p.getRecords());
        fillDcNames(p.getRecords());
        return new PageResult<>(p.getRecords(), p.getTotal(), pageNum, pageSize);
    }

    public BillDto<Distribution, DistributionDetail> getById(Integer id) {
        Distribution header = distributionMapper.selectById(id);
        if (header == null) {
            return null;
        }
        BillDto<Distribution, DistributionDetail> dto = new BillDto<>();
        dto.setHeader(header);
        fillShopNames(java.util.Collections.singletonList(header));
        fillDcNames(java.util.Collections.singletonList(header));
        dto.setDetails(fillGoodsNames(detailMapper.selectList(new LambdaQueryWrapper<DistributionDetail>()
                .eq(DistributionDetail::getDistributionId, header.getDistributionNo())
                .orderByAsc(DistributionDetail::getSeq, DistributionDetail::getId))));
        return dto;
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(BillDto<Distribution, DistributionDetail> dto) {
        Distribution header = dto.getHeader();
        if (header.getId() == null) {
            // add by codebuddy 20260817: 创建时间默认为当前时间
            if (header.getCreateDatetime() == null) {
                header.setCreateDatetime(LocalDateTime.now());
            }
            if (!StringUtils.hasText(header.getDistributionNo())) {
                header.setDistributionNo(nextNo(header.getSiteId()));
            }
            // add by codebuddy 20260817: 新建配送收货单时按门店商品配置表取出配送中心写入主表
            if (header.getDeliveryCenterId() == null) {
                fillDeliveryCenterFromGoodsShop(header, dto.getDetails());
            }
            distributionMapper.insert(header);
        } else {
            distributionMapper.updateById(header);
        }

        detailMapper.delete(new LambdaQueryWrapper<DistributionDetail>()
                .eq(DistributionDetail::getDistributionId, header.getDistributionNo()));
        List<DistributionDetail> details = dto.getDetails();
        if (details != null) {
            int seq = 1;
            for (DistributionDetail d : details) {
                d.setId(null);
                d.setDistributionId(header.getDistributionNo());
                if (d.getSeq() == null) {
                    d.setSeq(seq++);
                }
                detailMapper.insert(d);
            }
        }
    }

    // add by codebuddy 20260817: 确认配送收货单，状态置为Y
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Integer id) {
        Distribution header = distributionMapper.selectById(id);
        if (header == null) {
            throw new BusinessException("配送收货单不存在");
        }
        header.setStatus("Y");
        distributionMapper.updateById(header);
    }

    // add by codebuddy 20260817: 提交配送收货单（需先确认），状态置为TJ
    @Transactional(rollbackFor = Exception.class)
    public void submit(Integer id) {
        Distribution header = distributionMapper.selectById(id);
        if (header == null) {
            throw new BusinessException("配送收货单不存在");
        }
        if ("TJ".equals(header.getStatus())) {
            throw new BusinessException("配送收货单已提交，不能重复提交");
        }
        if (!"Y".equals(header.getStatus())) {
            throw new BusinessException("请先确认配送收货单后再提交");
        }
        header.setStatus("TJ");
        distributionMapper.updateById(header);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        Distribution header = distributionMapper.selectById(id);
        if (header != null) {
            detailMapper.delete(new LambdaQueryWrapper<DistributionDetail>()
                    .eq(DistributionDetail::getDistributionId, header.getDistributionNo()));
        }
        distributionMapper.deleteById(id);
    }

    private static final String NO_PREFIX = "PSSH";

    // add by codebuddy 20260817: 单号前缀加入门店编号
    /** 生成配送收货单号：PSSH + 门店编号 + 年月日 + 三位流水号，例如 PSSHSH000120260817001 */
    private String nextNo(Integer siteId) {
        String shopNo = getShopNo(siteId);
        String prefix = NO_PREFIX + shopNo + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long count = distributionMapper.selectCount(new LambdaQueryWrapper<Distribution>()
                .likeRight(Distribution::getDistributionNo, prefix));
        return CodeGenerator.genDaily(NO_PREFIX + shopNo, count.intValue());
    }

    private String getShopNo(Integer siteId) {
        if (siteId == null) {
            return "";
        }
        Shop shop = shopMapper.selectById(siteId);
        return shop == null ? "" : shop.getShopNo();
    }

    private void fillShopNames(List<Distribution> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Integer> ids = list.stream().map(Distribution::getSiteId).filter(java.util.Objects::nonNull)
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

    // add by codebuddy 20260817: 按门店商品配置表取出配送中心写入配送收货单主表（取第一条明细配置的配送中心）
    private void fillDeliveryCenterFromGoodsShop(Distribution header, List<DistributionDetail> details) {
        if (header.getSiteId() == null || details == null || details.isEmpty()) {
            return;
        }
        for (DistributionDetail d : details) {
            if (d.getItemId() == null) {
                continue;
            }
            GoodsShop gs = goodsShopMapper.selectOne(new LambdaQueryWrapper<GoodsShop>()
                    .eq(GoodsShop::getShopId, header.getSiteId())
                    .eq(GoodsShop::getItemId, d.getItemId())
                    .last("LIMIT 1"));
            if (gs != null && gs.getDeliveryCenterId() != null) {
                header.setDeliveryCenterId(gs.getDeliveryCenterId());
                break;
            }
        }
    }

    // add by codebuddy 20260817: 批量填充配送中心编号与名称
    private void fillDcNames(List<Distribution> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Integer> ids = list.stream().map(Distribution::getDeliveryCenterId).filter(java.util.Objects::nonNull)
                .distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return;
        }
        Map<Integer, DistributionCenter> dcMap = dcMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(DistributionCenter::getId, Function.identity()));
        list.forEach(r -> {
            DistributionCenter dc = dcMap.get(r.getDeliveryCenterId());
            if (dc != null) {
                r.setDeliveryCenterNo(dc.getDcNo());
                r.setDeliveryCenterName(dc.getDcName());
            }
        });
    }

    private List<DistributionDetail> fillGoodsNames(List<DistributionDetail> details) {
        if (details == null || details.isEmpty()) {
            return details;
        }
        List<Integer> ids = details.stream().map(DistributionDetail::getItemId).filter(java.util.Objects::nonNull)
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

    // add by codebuddy 20260819: 配送收货单批量上传 ERP（transfer.update），定时任务调用
    @Transactional(rollbackFor = Exception.class)
    public int uploadSubmittedToErp() {
        List<Distribution> list = distributionMapper.selectList(new LambdaQueryWrapper<Distribution>()
                .eq(Distribution::getStatus, "Y")
                .eq(Distribution::getAbortUpload, 0)
                .orderByAsc(Distribution::getId));
        int success = 0;
        for (Distribution d : list) {
            try {
                uploadOneToErp(d);
                success++;
            } catch (Exception e) {
                log.error("配送收货单 {} 上传 ERP 失败: {}", d.getDistributionNo(), e.getMessage(), e);
            }
        }
        return success;
    }

    // add by codebuddy 20260819: 单张配送收货单上传 ERP（transfer.update）
    @Transactional(rollbackFor = Exception.class)
    public void uploadOneToErp(Distribution d) {
        d.setLastTransmitAt(LocalDateTime.now());
        distributionMapper.updateById(d);
        List<DistributionDetail> details = detailMapper.selectList(new LambdaQueryWrapper<DistributionDetail>()
                .eq(DistributionDetail::getDistributionId, d.getDistributionNo())
                .orderByAsc(DistributionDetail::getSeq, DistributionDetail::getId));
        if (details == null || details.isEmpty()) {
            throw new BusinessException("配送收货单明细为空，无法上传");
        }
        // 取对应配送发货单的 ERP 单号（source_no）
        String sourceNo = d.getDeliveryShipmentNo();
        if (StringUtils.hasText(d.getDeliveryShipmentNo())) {
            DeliveryShipment shipment = shipmentMapper.selectOne(new LambdaQueryWrapper<DeliveryShipment>()
                    .eq(DeliveryShipment::getShipmentNo, d.getDeliveryShipmentNo()));
            if (shipment != null && StringUtils.hasText(shipment.getErpNo())) {
                sourceNo = shipment.getErpNo();
            }
        }
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String siteNo = StringUtils.hasText(d.getSiteNo()) ? d.getSiteNo() : "";
        String creator = StringUtils.hasText(d.getCreator()) ? d.getCreator() : siteNo;
        String transferOutSiteNo = StringUtils.hasText(d.getDeliveryCenterNo()) ? d.getDeliveryCenterNo() : "";
        // 调拨明细
        List<Map<String, Object>> transferDetail = new ArrayList<>();
        for (DistributionDetail dt : details) {
            Map<String, Object> dm = new HashMap<>();
            dm.put("seq", dt.getSeq() != null ? dt.getSeq() : transferDetail.size() + 1);
            dm.put("source_seq", dt.getSeq() != null ? dt.getSeq() : transferDetail.size() + 1);
            dm.put("source_no", StringUtils.hasText(sourceNo) ? sourceNo : "");
            dm.put("po_template_no", "NC001");
            dm.put("item_no", dt.getItemNo());
            dm.put("item_memo", "");
            dm.put("item_batch_no", StringUtils.hasText(dt.getBatchNo()) ? dt.getBatchNo() : "");
            dm.put("prod_date", "");
            dm.put("item_barcode", StringUtils.hasText(dt.getBarcode()) ? dt.getBarcode() : "");
            dm.put("packing_unit", StringUtils.hasText(dt.getUnit()) ? dt.getUnit() : "");
            dm.put("packing_qty", dt.getQty() != null ? dt.getQty().toPlainString() : "0");
            dm.put("base_qty", dt.getQty() != null ? dt.getQty().toPlainString() : "0");
            dm.put("warehouse_no", siteNo);
            dm.put("feature_no", " ");
            dm.put("distri_price", dt.getPrice() != null ? dt.getPrice().toPlainString() : "0");
            dm.put("distri_amount", dt.getAmount() != null ? dt.getAmount().toPlainString() : "0");
            dm.put("price", "0");
            dm.put("amount", dt.getAmount() != null ? dt.getAmount().toPlainString() : "");
            dm.put("item_feature_no", " ");
            dm.put("base_unit", StringUtils.hasText(dt.getUnit()) ? dt.getUnit() : "");
            transferDetail.add(dm);
        }
        // 调拨单头
        Map<String, Object> transfer = new HashMap<>();
        transfer.put("version", "3.0");
        transfer.put("front_no", d.getDistributionNo());
        transfer.put("site_no", siteNo);
        transfer.put("transfer_out_site_no", transferOutSiteNo);
        transfer.put("source_no", StringUtils.hasText(sourceNo) ? sourceNo : "");
        transfer.put("remark", "系统自动确认");
        transfer.put("creator", creator);
        transfer.put("create_datetime", now);
        transfer.put("modify_no", "");
        transfer.put("modify_datetime", "");
        transfer.put("approve_no", creator);
        transfer.put("approve_datetime", now);
        transfer.put("posted_no", creator);
        transfer.put("posted_datetime", now);
        transfer.put("transfer_type", "2");
        transfer.put("is_multiple_receipt", "0");
        transfer.put("isRefund", "");
        transfer.put("transfer_detail", transferDetail);
        // std_data -> parameter -> transfer[]
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("transfer", java.util.Collections.singletonList(transfer));
        Map<String, Object> stdData = new HashMap<>();
        stdData.put("parameter", parameter);
        try {
            erpClient.request("transfer.update", siteNo, stdData);
            d.setErpNo(StringUtils.hasText(sourceNo) ? sourceNo : d.getDistributionNo());
            d.setStatus("SC");
            d.setErrorMessage(null);
            distributionMapper.updateById(d);
            log.info("配送收货单 {} 上传 ERP(transfer.update) 成功", d.getDistributionNo());
        } catch (Exception e) {
            d.setErrorMessage(e.getMessage());
            distributionMapper.updateById(d);
            throw e instanceof BusinessException ? (BusinessException) e : new BusinessException(e.getMessage());
        }
    }
}

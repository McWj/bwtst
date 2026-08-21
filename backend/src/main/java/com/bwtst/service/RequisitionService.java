package com.bwtst.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bwtst.common.BillDto;
import com.bwtst.common.BusinessException;
import com.bwtst.common.CodeGenerator;
import com.bwtst.common.PageResult;
import com.bwtst.common.ErpClient;
import com.bwtst.entity.DeliveryShipment;
import com.bwtst.entity.DeliveryShipmentDetail;
import com.bwtst.entity.DistributionCenter;
import com.bwtst.entity.Goods;
import com.bwtst.entity.GoodsShop;
import com.bwtst.entity.Ptemplate;
import com.bwtst.entity.Purchase;
import com.bwtst.entity.PurchaseDetail;
import com.bwtst.entity.Requisition;
import com.bwtst.entity.RequisitionDetail;
import com.bwtst.entity.Shop;
import com.bwtst.mapper.DistributionCenterMapper;
import com.bwtst.mapper.GoodsMapper;
import com.bwtst.mapper.GoodsShopMapper;
import com.bwtst.mapper.PtemplateMapper;
import com.bwtst.mapper.RequisitionDetailMapper;
import com.bwtst.mapper.RequisitionMapper;
import com.bwtst.mapper.ShopMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// add by codebuddy 20260817
@Slf4j
@Service
@RequiredArgsConstructor
public class RequisitionService {

    private final RequisitionMapper requisitionMapper;
    private final RequisitionDetailMapper detailMapper;
    private final ShopMapper shopMapper;
    private final GoodsMapper goodsMapper;
    private final PurchaseService purchaseService;
    // add by codebuddy 20260817: 配送方式2（统采配送）改为自动生成配送发货单
    private final DeliveryShipmentService deliveryShipmentService;
    private final GoodsShopMapper goodsShopMapper;
    private final PtemplateMapper ptemplateMapper;
    private final ErpClient erpClient;
    // add by codebuddy 20260817: 配送中心名称填充
    private final DistributionCenterMapper dcMapper;

    public PageResult<Requisition> page(long pageNum, long pageSize, String keyword, Integer siteId) {
        LambdaQueryWrapper<Requisition> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(keyword), w -> w
                        .like(Requisition::getRequisitionNo, keyword)
                        .or().like(Requisition::getCreator, keyword))
                .eq(siteId != null, Requisition::getSiteId, siteId)
                .orderByDesc(Requisition::getId);
        Page<Requisition> p = requisitionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        fillShopNames(p.getRecords());
        fillDcNames(p.getRecords());
        return new PageResult<>(p.getRecords(), p.getTotal(), pageNum, pageSize);
    }

    /** 查询单据及其明细 */
    public BillDto<Requisition, RequisitionDetail> getById(Integer id) {
        Requisition header = requisitionMapper.selectById(id);
        if (header == null) {
            return null;
        }
        BillDto<Requisition, RequisitionDetail> dto = new BillDto<>();
        dto.setHeader(header);
        fillShopNames(java.util.Collections.singletonList(header));
        fillDcNames(java.util.Collections.singletonList(header));
        dto.setDetails(fillGoodsNames(detailMapper.selectList(new LambdaQueryWrapper<RequisitionDetail>()
                .eq(RequisitionDetail::getRequisitionId, header.getRequisitionNo())
                .orderByAsc(RequisitionDetail::getSeq, RequisitionDetail::getId))));
        return dto;
    }

    /** 新增或修改（含明细，明细先删后插） */
    @Transactional(rollbackFor = Exception.class)
    public void save(BillDto<Requisition, RequisitionDetail> dto) {
        Requisition header = dto.getHeader();
        if (header.getId() == null) {
            // add by codebuddy 20260817: 创建时间默认为当前时间
            if (header.getCreateDatetime() == null) {
                header.setCreateDatetime(LocalDateTime.now());
            }
            if (!StringUtils.hasText(header.getRequisitionNo())) {
                header.setRequisitionNo(nextNo(header.getSiteId()));
            }
            // add by codebuddy 20260817: 新建要货单时按门店商品配置表取出配送中心写入主表
            if (header.getDeliveryCenterId() == null) {
                fillDeliveryCenterFromGoodsShop(header, dto.getDetails());
            }
            requisitionMapper.insert(header);
        } else {
            requisitionMapper.updateById(header);
        }

        detailMapper.delete(new LambdaQueryWrapper<RequisitionDetail>()
                .eq(RequisitionDetail::getRequisitionId, header.getRequisitionNo()));
        List<RequisitionDetail> details = dto.getDetails();
        if (details != null) {
            int seq = 1;
            for (RequisitionDetail d : details) {
                d.setId(null);
                d.setRequisitionId(header.getRequisitionNo());
                if (d.getSeq() == null) {
                    d.setSeq(seq++);
                }
                detailMapper.insert(d);
            }
        }
    }

    // add by codebuddy 20260817: 确认要货单，更新 ERP 返回的预估运费/重量/体积及单位，状态置为Y
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Integer id, Requisition body) {
        Requisition header = requisitionMapper.selectById(id);
        if (header == null) {
            throw new BusinessException("要货单不存在");
        }
        if ("TJ".equals(header.getStatus())) {
            throw new BusinessException("要货单已提交，不能确认");
        }
        if (body != null) {
            header.setDistributionFee(body.getDistributionFee());
            header.setWeight(body.getWeight());
            header.setWeightUnit(body.getWeightUnit());
            header.setVolume(body.getVolume());
            header.setVolumeUnit(body.getVolumeUnit());
        }
        header.setStatus("Y");
        requisitionMapper.updateById(header);
    }

    // add by codebuddy 20260817: 提交要货单（需先确认），状态置为TJ
    // mark by codebuddy 20260818: 收货单/配送发货单改为上传 ERP 成功后自动生成（见 uploadOneToErp）
    @Transactional(rollbackFor = Exception.class)
    public void submit(Integer id) {
        Requisition header = requisitionMapper.selectById(id);
        if (header == null) {
            throw new BusinessException("要货单不存在");
        }
        if ("TJ".equals(header.getStatus())) {
            throw new BusinessException("要货单已提交，不能重复提交");
        }
        if (!"Y".equals(header.getStatus())) {
            throw new BusinessException("请先确认要货单后再提交");
        }
        header.setStatus("TJ");
        requisitionMapper.updateById(header);
    }

    // add by codebuddy 20260817: 根据门店商品配置的配送方式自动生成收货/发货单
    // 配送方式：1.统采直送 -> 采购收货单；2.统采配送 -> 配送发货单（按配送中心分组，每个配送中心一张）
    private void generateReceipts(Requisition header, List<RequisitionDetail> details) {
        if (header.getSiteId() == null || details == null || details.isEmpty()) {
            return;
        }
        List<PurchaseDetail> purchaseDetails = new ArrayList<>();
        // add by codebuddy 20260817: 配送方式2 -> 按配送中心分组生成配送发货单
        Map<Integer, List<RequisitionDetail>> shipmentGroup = new LinkedHashMap<>();
        for (RequisitionDetail d : details) {
            if (d.getItemId() == null || d.getQty() == null || d.getQty().signum() <= 0) {
                continue;
            }
            GoodsShop gs = goodsShopMapper.selectOne(new LambdaQueryWrapper<GoodsShop>()
                    .eq(GoodsShop::getShopId, header.getSiteId())
                    .eq(GoodsShop::getItemId, d.getItemId())
                    .last("LIMIT 1"));
            if (gs == null || gs.getDeliveryType() == null) {
                continue;
            }
            BigDecimal price = d.getPrice() == null ? BigDecimal.ZERO : d.getPrice();
            BigDecimal amount = price.multiply(d.getQty());
            if (Integer.valueOf(1).equals(gs.getDeliveryType())) {
                PurchaseDetail pd = new PurchaseDetail();
                pd.setItemId(d.getItemId());
                pd.setRequisitionQty(d.getQty());
                pd.setQty(d.getQty());
                pd.setUnit(d.getUnit());
                pd.setPrice(price);
                pd.setAmount(amount);
                purchaseDetails.add(pd);
            } else if (Integer.valueOf(2).equals(gs.getDeliveryType())) {
                // add by codebuddy 20260817: 改为生成配送发货单，按配送中心分组
                if (gs.getDeliveryCenterId() == null) {
                    log.warn("要货单 {} 商品 {} 配送方式为统采配送但未配置配送中心，跳过", header.getRequisitionNo(), d.getItemId());
                    continue;
                }
                shipmentGroup.computeIfAbsent(gs.getDeliveryCenterId(), k -> new ArrayList<>()).add(d);
            }
        }
        if (!purchaseDetails.isEmpty()) {
            Purchase purchase = new Purchase();
            purchase.setPurchaseDate(LocalDate.now());
            purchase.setSiteId(header.getSiteId());
            purchase.setCreator(header.getCreator());
            purchase.setCreateDatetime(LocalDateTime.now());
            BillDto<Purchase, PurchaseDetail> dto = new BillDto<>();
            dto.setHeader(purchase);
            dto.setDetails(purchaseDetails);
            purchaseService.save(dto);
        }
        // add by codebuddy 20260817: 每个配送中心生成一张配送发货单，明细项次从1开始，首个批次项序为1
        shipmentGroup.forEach((dcId, group) -> {
            DeliveryShipment shipment = new DeliveryShipment();
            shipment.setShipmentDate(LocalDate.now());
            shipment.setDeliveryCenterId(dcId);
            // add by codebuddy 20260818: 收货门店必填（要货单的门店即收货门店，save 校验依赖此字段）
            shipment.setSiteId(header.getSiteId());
            shipment.setCreator(header.getCreator());
            shipment.setCreateDatetime(LocalDateTime.now());
            List<DeliveryShipmentDetail> shipmentDetails = new ArrayList<>();
            int seq = 1;
            for (RequisitionDetail d : group) {
                DeliveryShipmentDetail sd = new DeliveryShipmentDetail();
                sd.setSeq(seq++);
                sd.setOrderSeq(1);
                sd.setItemId(d.getItemId());
                sd.setQty(d.getQty());
                shipmentDetails.add(sd);
            }
            BillDto<DeliveryShipment, DeliveryShipmentDetail> dto = new BillDto<>();
            dto.setHeader(shipment);
            dto.setDetails(shipmentDetails);
            deliveryShipmentService.save(dto);
            log.info("要货单 {} 自动生成配送发货单 {}，配送中心: {}", header.getRequisitionNo(), shipment.getShipmentNo(), dcId);
        });
    }

    // add by codebuddy 20260817: 定时任务调用，抓取已提交(TJ)且未上传过(erpNo为空)的要货单上传 ERP
    public int uploadSubmittedToErp() {
        List<Requisition> list = requisitionMapper.selectList(new LambdaQueryWrapper<Requisition>()
                .eq(Requisition::getStatus, "TJ")
                .and(w -> w.isNull(Requisition::getErpNo).or().eq(Requisition::getErpNo, ""))
                // add by codebuddy 20260818: 排除已终止上传的单据
                .and(w -> w.isNull(Requisition::getAbortUpload).or().ne(Requisition::getAbortUpload, "Y")));
        int success = 0;
        for (Requisition header : list) {
            try {
                uploadOneToErp(header);
                success++;
            } catch (Exception e) {
                log.error("要货单 {} 上传 ERP 失败: {}", header.getRequisitionNo(), e.getMessage());
                // add by codebuddy 20260818: 持久化 ERP 报错信息，便于大屏查看未上传详情
                header.setErrorMessage(e.getMessage());
                requisitionMapper.updateById(header);
            }
        }
        return success;
    }

    // add by codebuddy 20260817: 上传单张要货单到 ERP（requisition.create），成功保存 ERP 单号并置状态为已上传(SC)
    private void uploadOneToErp(Requisition header) {
        List<RequisitionDetail> details = fillGoodsNames(detailMapper.selectList(new LambdaQueryWrapper<RequisitionDetail>()
                .eq(RequisitionDetail::getRequisitionId, header.getRequisitionNo())
                .orderByAsc(RequisitionDetail::getSeq, RequisitionDetail::getId)));
        if (details == null || details.isEmpty()) {
            throw new BusinessException("要货单明细为空");
        }
        String shopNo = getShopNo(header.getSiteId());
        if (!StringUtils.hasText(shopNo)) {
            throw new BusinessException("门店编码为空");
        }
        String poTemplateNo = "";
        if (header.getTemplateNo() != null) {
            Ptemplate template = ptemplateMapper.selectById(header.getTemplateNo());
            if (template != null) {
                poTemplateNo = template.getPtemplateNo() == null ? "" : template.getPtemplateNo();
            }
        }
        Map<String, Object> request = buildErpRequest(header, details, shopNo, poTemplateNo);
        Map<String, Object> parameter = new LinkedHashMap<>();
        parameter.put("request", Collections.singletonList(request));
        Map<String, Object> stdData = new LinkedHashMap<>();
        stdData.put("parameter", parameter);

        // add by codebuddy 20260818: 调用 ERP 接口前记录传输时间
        header.setLastTransmitAt(LocalDateTime.now());
        JsonNode result = erpClient.request("requisition.create", shopNo, stdData);
        String docNo = result.path("parameter").path("doc_no").asText("");
        if (!StringUtils.hasText(docNo)) {
            throw new BusinessException("ERP 未返回单号");
        }
        header.setErpNo(docNo);
        header.setStatus("SC");
        requisitionMapper.updateById(header);
        log.info("要货单 {} 上传 ERP 成功，ERP 单号: {}", header.getRequisitionNo(), docNo);
        // add by codebuddy 20260818: 上传成功后才按门店商品配置的配送方式自动生成收货单/配送发货单
        try {
            generateReceipts(header, details);
        } catch (Exception e) {
            log.error("要货单 {} 上传成功后自动生成收货/发货单失败: {}", header.getRequisitionNo(), e.getMessage(), e);
            header.setErrorMessage("上传成功，但自动生成收货/发货单失败: " + e.getMessage());
            requisitionMapper.updateById(header);
        }
    }

    // add by codebuddy 20260817: 按要货单上传 ERP 报文样例构造 request 节点
    private Map<String, Object> buildErpRequest(Requisition h, List<RequisitionDetail> details,
                                                String shopNo, String poTemplateNo) {
        DateTimeFormatter dt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String now = LocalDateTime.now().format(dt);
        String createDatetime = h.getCreateDatetime() == null ? now : h.getCreateDatetime().format(dt);
        String createDate = h.getCreateDatetime() == null ? LocalDate.now().toString()
                : h.getCreateDatetime().toLocalDate().toString();
        String requisitionDate = h.getRequisitionDate() == null ? LocalDate.now().toString()
                : h.getRequisitionDate().toString();
        String demandDate = h.getDemandDate() == null ? requisitionDate : h.getDemandDate().toString();
        String creator = StringUtils.hasText(h.getCreator()) ? h.getCreator() : shopNo;
        String fee = h.getDistributionFee() == null ? "0" : h.getDistributionFee().toPlainString();

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("version", "3.0");
        request.put("creator", creator);
        request.put("create_datetime", createDatetime);
        request.put("modify_no", creator);
        request.put("modify_datetime", now);
        request.put("approve_no", creator);
        request.put("approve_datetime", now);
        request.put("status", "Y");
        request.put("site_no", shopNo);
        request.put("requisition_date", requisitionDate);
        request.put("requisition_time", "00:00:00");
        request.put("create_date", createDate);
        request.put("account_date", createDate);
        request.put("po_template_no", poTemplateNo);
        request.put("is_add_po", h.getIsAddPo() == null ? "N" : h.getIsAddPo());
        request.put("is_urgent_order", h.getIsUrgentOrder() == null ? "N" : h.getIsUrgentOrder());
        request.put("remark", "");
        request.put("load_doc_no", "");
        request.put("load_type", "0");
        request.put("load_shop", "");
        request.put("front_no", h.getRequisitionNo() == null ? "" : h.getRequisitionNo());
        request.put("receipt_org", "BWCS");
        request.put("shipType", "0");
        request.put("distributionFee", fee);
        request.put("demand_date", demandDate);

        List<Map<String, Object>> detailList = new ArrayList<>();
        int seq = 1;
        for (RequisitionDetail d : details) {
            if (d.getItemId() == null || d.getQty() == null || d.getQty().signum() <= 0) {
                continue;
            }
            BigDecimal price = d.getPrice() == null ? BigDecimal.ZERO : d.getPrice();
            BigDecimal qty = d.getQty();
            BigDecimal amount = price.multiply(qty);
            String unit = StringUtils.hasText(d.getUnit()) ? d.getUnit() : "";
            Map<String, Object> dd = new LinkedHashMap<>();
            dd.put("seq", String.valueOf(seq++));
            dd.put("item_no", d.getItemNo() == null ? "" : d.getItemNo());
            dd.put("so_qty", "0");
            dd.put("price", price.toPlainString());
            dd.put("amount", amount.toPlainString());
            dd.put("packing_unit", unit);
            dd.put("packing_qty", qty.toPlainString());
            dd.put("base_unit", unit);
            dd.put("base_qty", qty.toPlainString());
            dd.put("warehouse_no", "");
            dd.put("feature_no", " ");
            dd.put("remark", "");
            dd.put("distri_price", price.toPlainString());
            dd.put("distri_amount", amount.toPlainString());
            detailList.add(dd);
        }
        if (detailList.isEmpty()) {
            throw new BusinessException("要货单无可上传明细");
        }
        request.put("request_detail", detailList);
        return request;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        Requisition header = requisitionMapper.selectById(id);
        if (header != null) {
            detailMapper.delete(new LambdaQueryWrapper<RequisitionDetail>()
                    .eq(RequisitionDetail::getRequisitionId, header.getRequisitionNo()));
        }
        requisitionMapper.deleteById(id);
    }

    private static final String NO_PREFIX = "YHSQ";

    // add by codebuddy 20260817: 单号前缀加入门店编号
    /** 生成要货单号：YHSQ + 门店编号 + 年月日 + 三位流水号，例如 YHSQSH000120260817001 */
    private String nextNo(Integer siteId) {
        String shopNo = getShopNo(siteId);
        String prefix = NO_PREFIX + shopNo + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long count = requisitionMapper.selectCount(new LambdaQueryWrapper<Requisition>()
                .likeRight(Requisition::getRequisitionNo, prefix));
        return CodeGenerator.genDaily(NO_PREFIX + shopNo, count.intValue());
    }

    private String getShopNo(Integer siteId) {
        if (siteId == null) {
            return "";
        }
        Shop shop = shopMapper.selectById(siteId);
        return shop == null ? "" : shop.getShopNo();
    }

    private void fillShopNames(List<Requisition> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Integer> ids = list.stream().map(Requisition::getSiteId).filter(java.util.Objects::nonNull)
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

    // add by codebuddy 20260817: 按门店商品配置表取出配送中心写入要货单主表（取第一条明细配置的配送中心）
    private void fillDeliveryCenterFromGoodsShop(Requisition header, List<RequisitionDetail> details) {
        if (header.getSiteId() == null || details == null || details.isEmpty()) {
            return;
        }
        for (RequisitionDetail d : details) {
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
    private void fillDcNames(List<Requisition> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Integer> ids = list.stream().map(Requisition::getDeliveryCenterId).filter(java.util.Objects::nonNull)
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

    private List<RequisitionDetail> fillGoodsNames(List<RequisitionDetail> details) {
        if (details == null || details.isEmpty()) {
            return details;
        }
        List<Integer> ids = details.stream().map(RequisitionDetail::getItemId).filter(java.util.Objects::nonNull)
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

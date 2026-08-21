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
import com.bwtst.entity.Distribution;
import com.bwtst.entity.DistributionCenter;
import com.bwtst.entity.DistributionDetail;
import com.bwtst.entity.Goods;
import com.bwtst.entity.GoodsShop;
import com.bwtst.entity.Shop;
import com.bwtst.mapper.DeliveryShipmentDetailMapper;
import com.bwtst.mapper.DeliveryShipmentMapper;
import com.bwtst.mapper.DistributionCenterMapper;
import com.bwtst.mapper.DistributionDetailMapper;
import com.bwtst.mapper.DistributionMapper;
import com.bwtst.mapper.GoodsMapper;
import com.bwtst.mapper.GoodsShopMapper;
import com.bwtst.mapper.ShopMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

// add by codebuddy 20260817
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryShipmentService {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DeliveryShipmentMapper shipmentMapper;
    private final DeliveryShipmentDetailMapper detailMapper;
    private final DistributionCenterMapper dcMapper;
    private final GoodsMapper goodsMapper;
    // add by codebuddy 20260817: 配送发货上传 ERP（DocnoUpload）
    private final ErpClient erpClient;
    // add by codebuddy 20260818: 上传成功后按门店生成配送收货单
    private final ShopMapper shopMapper;
    private final DistributionMapper distributionMapper;
    private final DistributionDetailMapper distributionDetailMapper;
    private final GoodsShopMapper goodsShopMapper;

    public PageResult<DeliveryShipment> page(long pageNum, long pageSize, String keyword, Integer deliveryCenterId) {
        LambdaQueryWrapper<DeliveryShipment> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(keyword), w -> w
                        .like(DeliveryShipment::getShipmentNo, keyword)
                        .or().like(DeliveryShipment::getErpNo, keyword)
                        .or().like(DeliveryShipment::getCreator, keyword))
                .eq(deliveryCenterId != null, DeliveryShipment::getDeliveryCenterId, deliveryCenterId)
                .orderByDesc(DeliveryShipment::getId);
        Page<DeliveryShipment> p = shipmentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        fillDcNames(p.getRecords());
        // add by codebuddy 20260818: 填充收货门店名称
        fillSiteNames(p.getRecords());
        return new PageResult<>(p.getRecords(), p.getTotal(), pageNum, pageSize);
    }

    public BillDto<DeliveryShipment, DeliveryShipmentDetail> getById(Integer id) {
        DeliveryShipment header = shipmentMapper.selectById(id);
        if (header == null) {
            return null;
        }
        fillDcNames(Collections.singletonList(header));
        // add by codebuddy 20260818: 填充收货门店名称
        fillSiteNames(Collections.singletonList(header));
        BillDto<DeliveryShipment, DeliveryShipmentDetail> dto = new BillDto<>();
        dto.setHeader(header);
        dto.setDetails(fillGoodsNames(detailMapper.selectList(new LambdaQueryWrapper<DeliveryShipmentDetail>()
                .eq(DeliveryShipmentDetail::getShipmentId, header.getShipmentNo())
                .orderByAsc(DeliveryShipmentDetail::getSeq, DeliveryShipmentDetail::getOrderSeq,
                        DeliveryShipmentDetail::getId))));
        return dto;
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(BillDto<DeliveryShipment, DeliveryShipmentDetail> dto) {
        DeliveryShipment header = dto.getHeader();
        // add by codebuddy 20260818: 收货门店必填（上传 ERP 成功后据此生成对应门店的配送收货单）
        if (header.getSiteId() == null) {
            throw new BusinessException("请选择收货门店");
        }
        // add by codebuddy 20260817: 已发货(Y)或已上传(SC)的单据不允许修改
        if (header.getId() != null) {
            DeliveryShipment exist = shipmentMapper.selectById(header.getId());
            if (exist != null && StringUtils.hasText(exist.getStatus())) {
                throw new BusinessException("单据已发货或已上传，不能修改");
            }
        }
        if (header.getId() == null) {
            // add by codebuddy 20260817: 创建时间默认为当前时间
            if (header.getCreateDatetime() == null) {
                header.setCreateDatetime(LocalDateTime.now());
            }
            // 未传单号时自动生成：配送中心编号+FH+年月日+三位流水号
            if (!StringUtils.hasText(header.getShipmentNo())) {
                header.setShipmentNo(nextNo(header.getDeliveryCenterId()));
            }
            shipmentMapper.insert(header);
        } else {
            shipmentMapper.updateById(header);
        }

        // 明细先删后插
        detailMapper.delete(new LambdaQueryWrapper<DeliveryShipmentDetail>()
                .eq(DeliveryShipmentDetail::getShipmentId, header.getShipmentNo()));
        List<DeliveryShipmentDetail> details = dto.getDetails();
        if (details != null) {
            int seq = 1;
            for (DeliveryShipmentDetail d : details) {
                d.setId(null);
                d.setShipmentId(header.getShipmentNo());
                if (d.getSeq() == null) {
                    d.setSeq(seq);
                }
                seq++;
                detailMapper.insert(d);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        DeliveryShipment header = shipmentMapper.selectById(id);
        if (header == null) {
            return;
        }
        // add by codebuddy 20260817: 已发货(Y)或已上传(SC)的单据不允许删除
        if (StringUtils.hasText(header.getStatus())) {
            throw new BusinessException("单据已发货或已上传，不能删除");
        }
        detailMapper.delete(new LambdaQueryWrapper<DeliveryShipmentDetail>()
                .eq(DeliveryShipmentDetail::getShipmentId, header.getShipmentNo()));
        shipmentMapper.deleteById(id);
    }

    // add by codebuddy 20260817: 配送发货，必须已录入 ERP 配送单号，发货后状态置为Y（已发货待上传）
    @Transactional(rollbackFor = Exception.class)
    public void deliver(Integer id, DeliveryShipment body) {
        DeliveryShipment header = shipmentMapper.selectById(id);
        if (header == null) {
            throw new BusinessException("配送发货单不存在");
        }
        if (StringUtils.hasText(header.getStatus())) {
            throw new BusinessException("单据已发货或已上传，不能重复发货");
        }
        Long detailCnt = detailMapper.selectCount(new LambdaQueryWrapper<DeliveryShipmentDetail>()
                .eq(DeliveryShipmentDetail::getShipmentId, header.getShipmentNo()));
        if (detailCnt == null || detailCnt == 0) {
            throw new BusinessException("配送发货单明细为空，不能发货");
        }
        String erpShipmentNo = header.getErpShipmentNo();
        if (body != null && StringUtils.hasText(body.getErpShipmentNo())) {
            erpShipmentNo = body.getErpShipmentNo();
        }
        if (!StringUtils.hasText(erpShipmentNo)) {
            throw new BusinessException("请先录入 ERP 配送单号才能发货");
        }
        header.setErpShipmentNo(erpShipmentNo);
        if (body != null) {
            if (body.getVolume() != null) {
                header.setVolume(body.getVolume());
            }
            if (body.getWeight() != null) {
                header.setWeight(body.getWeight());
            }
        }
        if (header.getShipmentDate() == null) {
            header.setShipmentDate(LocalDate.now());
        }
        header.setStatus("Y");
        shipmentMapper.updateById(header);
        log.info("配送发货单 {} 已发货，ERP 配送单号: {}", header.getShipmentNo(), erpShipmentNo);
    }

    // add by codebuddy 20260817: 定时任务调用，抓取已发货(Y)且未上传过(erpNo为空)的配送发货单上传 ERP(DocnoUpload)
    public int uploadSubmittedToErp() {
        List<DeliveryShipment> list = shipmentMapper.selectList(new LambdaQueryWrapper<DeliveryShipment>()
                .eq(DeliveryShipment::getStatus, "Y")
                .and(w -> w.isNull(DeliveryShipment::getErpNo).or().eq(DeliveryShipment::getErpNo, ""))
                // add by codebuddy 20260818: 排除已终止上传的单据
                .and(w -> w.isNull(DeliveryShipment::getAbortUpload).or().ne(DeliveryShipment::getAbortUpload, "Y")));
        int success = 0;
        for (DeliveryShipment header : list) {
            try {
                uploadOneToErp(header);
                success++;
            } catch (Exception e) {
                log.error("配送发货单 {} 上传 ERP 失败: {}", header.getShipmentNo(), e.getMessage());
                // add by codebuddy 20260818: 持久化 ERP 报错信息，便于大屏查看未上传详情
                header.setErrorMessage(e.getMessage());
                shipmentMapper.updateById(header);
            }
        }
        return success;
    }

    // add by codebuddy 20260817: 上传单张配送发货单到 ERP（DocnoUpload），成功保存 ERP 发货单号并置状态为已上传(SC)
    private void uploadOneToErp(DeliveryShipment header) {
        List<DeliveryShipmentDetail> details = fillGoodsNames(detailMapper.selectList(new LambdaQueryWrapper<DeliveryShipmentDetail>()
                .eq(DeliveryShipmentDetail::getShipmentId, header.getShipmentNo())
                .orderByAsc(DeliveryShipmentDetail::getSeq, DeliveryShipmentDetail::getOrderSeq,
                        DeliveryShipmentDetail::getId)));
        if (details == null || details.isEmpty()) {
            throw new BusinessException("配送发货单明细为空");
        }
        if (!StringUtils.hasText(header.getErpShipmentNo())) {
            throw new BusinessException("ERP 配送单号为空，不能上传");
        }
        String dcNo = getDcNo(header.getDeliveryCenterId());
        Map<String, Object> request = buildErpRequest(header, details, dcNo);
        Map<String, Object> parameter = new LinkedHashMap<>();
        parameter.put("request", Collections.singletonList(request));
        Map<String, Object> stdData = new LinkedHashMap<>();
        stdData.put("parameter", parameter);

        // add by codebuddy 20260818: 调用 ERP 接口前记录传输时间
        header.setLastTransmitAt(LocalDateTime.now());
        JsonNode result = erpClient.request("DocnoUpload", dcNo, stdData);
        String erpNo = result.path("parameter").path("ERP_no").asText("");
        if (!StringUtils.hasText(erpNo)) {
            throw new BusinessException("ERP 未返回发货单号");
        }
        header.setErpNo(erpNo);
        header.setStatus("SC");
        shipmentMapper.updateById(header);
        log.info("配送发货单 {} 上传 ERP 成功，ERP 发货单号: {}", header.getShipmentNo(), erpNo);
        // add by codebuddy 20260818: 上传成功后，根据门店id生成对应门店的配送收货单
        createDistributionFromShipment(header, details);
    }

    // add by codebuddy 20260818: 配送发货上传 ERP 成功后，根据门店id自动生成对应门店的配送收货单（未确认状态）
    private void createDistributionFromShipment(DeliveryShipment h, List<DeliveryShipmentDetail> details) {
        try {
            if (h.getSiteId() == null) {
                log.warn("配送发货单 {} 未选择收货门店，跳过生成配送收货单", h.getShipmentNo());
                return;
            }
            Distribution dist = new Distribution();
            dist.setDistributionNo(nextDistributionNo(h.getSiteId()));
            dist.setDistributionDate(LocalDate.now());
            dist.setSiteId(h.getSiteId());
            dist.setDeliveryCenterId(h.getDeliveryCenterId());
            dist.setCreator(h.getCreator());
            dist.setCreateDatetime(LocalDateTime.now());
            distributionMapper.insert(dist);

            int seq = 1;
            for (DeliveryShipmentDetail d : details) {
                if (d.getItemId() == null || d.getQty() == null || d.getQty().signum() <= 0) {
                    continue;
                }
                DistributionDetail dd = new DistributionDetail();
                dd.setDistributionId(dist.getDistributionNo());
                dd.setSeq(seq++);
                dd.setItemId(d.getItemId());
                dd.setQty(d.getQty());
                // 按门店商品配置表取单价，计算金额
                GoodsShop gs = goodsShopMapper.selectOne(new LambdaQueryWrapper<GoodsShop>()
                        .eq(GoodsShop::getShopId, h.getSiteId())
                        .eq(GoodsShop::getItemId, d.getItemId())
                        .last("LIMIT 1"));
                if (gs != null) {
                    if (gs.getPrice() != null) {
                        dd.setPrice(gs.getPrice());
                        dd.setAmount(gs.getPrice().multiply(d.getQty()));
                    }
                    // 发货单未指定配送中心时，从门店商品配置带出
                    if (dist.getDeliveryCenterId() == null && gs.getDeliveryCenterId() != null) {
                        dist.setDeliveryCenterId(gs.getDeliveryCenterId());
                    }
                }
                distributionDetailMapper.insert(dd);
            }
            if (dist.getDeliveryCenterId() != null) {
                distributionMapper.updateById(dist);
            }
            log.info("配送发货单 {} 已自动生成门店配送收货单 {}（门店id={}）",
                    h.getShipmentNo(), dist.getDistributionNo(), h.getSiteId());
        } catch (Exception e) {
            log.error("配送发货单 {} 生成配送收货单失败: {}", h.getShipmentNo(), e.getMessage(), e);
        }
    }

    // add by codebuddy 20260818: 生成配送收货单号：PSSH + 门店编号 + 年月日 + 三位流水号
    private String nextDistributionNo(Integer siteId) {
        String shopNo = getShopNo(siteId);
        String prefix = "PSSH" + shopNo + LocalDate.now().format(DAY_FMT);
        Long count = distributionMapper.selectCount(new LambdaQueryWrapper<Distribution>()
                .likeRight(Distribution::getDistributionNo, prefix));
        return CodeGenerator.genDaily("PSSH" + shopNo, count.intValue());
    }

    private String getShopNo(Integer siteId) {
        if (siteId == null) {
            return "";
        }
        Shop shop = shopMapper.selectById(siteId);
        return shop == null ? "" : shop.getShopNo();
    }

    // add by codebuddy 20260817: 按配送发货上传 ERP 报文样例（DocnoUpload）构造 request 节点
    private Map<String, Object> buildErpRequest(DeliveryShipment h, List<DeliveryShipmentDetail> details, String dcNo) {
        DateTimeFormatter dt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String operateTime = LocalDateTime.now().format(dt);
        String operator = StringUtils.hasText(h.getCreator()) ? h.getCreator() : "admin";
        String waveno = (h.getShipmentDate() == null ? LocalDate.now() : h.getShipmentDate()) + " ";
        String volumn = h.getVolume() == null ? "0" : h.getVolume().stripTrailingZeros().toPlainString();

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("sourceNo", h.getErpShipmentNo() == null ? "" : h.getErpShipmentNo());
        request.put("docType", "1");
        request.put("frontNo", h.getShipmentNo() == null ? " " : h.getShipmentNo());
        request.put("cost", "0");
        request.put("operator", operator);
        request.put("operateTime", operateTime);
        request.put("supplierNo", " ");
        request.put("waveno", waveno);
        request.put("volumn", volumn);
        request.put("pickup", "2");

        List<Map<String, Object>> itemList = new ArrayList<>();
        for (DeliveryShipmentDetail d : details) {
            if (d.getItemId() == null || d.getQty() == null || d.getQty().signum() <= 0) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sourceSeq", d.getSeq() == null ? "1" : String.valueOf(d.getSeq()));
            item.put("sourceSeq1", d.getOrderSeq() == null ? "1" : String.valueOf(d.getOrderSeq()));
            item.put("itemNo", d.getItemNo() == null ? "" : d.getItemNo());
            item.put("featureNo", " ");
            item.put("itemBarcode", " ");
            item.put("qty", d.getQty().stripTrailingZeros().toPlainString());
            item.put("unit", " ");
            item.put("warehouseNo", " ");
            item.put("batchNo", d.getLotNo() == null ? " " : d.getLotNo());
            item.put("productionDate", " ");
            item.put("effectiveDate", " ");
            item.put("boxCode", " ");
            item.put("remark", " ");
            itemList.add(item);
        }
        if (itemList.isEmpty()) {
            throw new BusinessException("配送发货单无可上传明细");
        }
        request.put("items", itemList);
        return request;
    }

    /** 单号：配送中心编号+FH+年月日+三位流水号，例如 BWCSFH20260817001 */
    private String nextNo(Integer deliveryCenterId) {
        String dcNo = getDcNo(deliveryCenterId);
        String prefix = dcNo + "FH" + LocalDate.now().format(DAY_FMT);
        Long count = shipmentMapper.selectCount(new LambdaQueryWrapper<DeliveryShipment>()
                .likeRight(DeliveryShipment::getShipmentNo, prefix));
        return CodeGenerator.genDaily(dcNo + "FH", count.intValue());
    }

    private String getDcNo(Integer deliveryCenterId) {
        if (deliveryCenterId == null) {
            throw new BusinessException("请选择配送中心");
        }
        DistributionCenter dc = dcMapper.selectById(deliveryCenterId);
        if (dc == null) {
            throw new BusinessException("配送中心不存在");
        }
        return dc.getDcNo();
    }

    private void fillDcNames(List<DeliveryShipment> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Integer> ids = list.stream().map(DeliveryShipment::getDeliveryCenterId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
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

    // add by codebuddy 20260818: 填充收货门店名称
    private void fillSiteNames(List<DeliveryShipment> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Integer> ids = list.stream().map(DeliveryShipment::getSiteId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
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

    private List<DeliveryShipmentDetail> fillGoodsNames(List<DeliveryShipmentDetail> details) {
        if (details == null || details.isEmpty()) {
            return details;
        }
        List<Integer> ids = details.stream().map(DeliveryShipmentDetail::getItemId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
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

package com.bwtst.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bwtst.common.BillDto;
import com.bwtst.common.BusinessException;
import com.bwtst.common.CodeGenerator;
import com.bwtst.common.PageResult;
import com.bwtst.common.ErpClient;
import com.bwtst.entity.Distribution;
import com.fasterxml.jackson.databind.JsonNode;
import com.bwtst.entity.DistributionCenter;
import com.bwtst.entity.DistributionDetail;
import com.bwtst.entity.DistributionReturn;
import com.bwtst.entity.DistributionReturnDetail;
import com.bwtst.entity.Goods;
import com.bwtst.entity.GoodsShop;
import com.bwtst.entity.Shop;
import com.bwtst.mapper.DistributionCenterMapper;
import com.bwtst.mapper.DistributionDetailMapper;
import com.bwtst.mapper.DistributionMapper;
import com.bwtst.mapper.DistributionReturnDetailMapper;
import com.bwtst.mapper.DistributionReturnMapper;
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
public class DistributionReturnService {

    private final DistributionReturnMapper returnMapper;
    private final DistributionReturnDetailMapper detailMapper;
    private final ShopMapper shopMapper;
    private final GoodsMapper goodsMapper;
    // add by codebuddy 20260817: 配送中心与门店商品配置
    private final DistributionCenterMapper dcMapper;
    private final GoodsShopMapper goodsShopMapper;
    // add by codebuddy 20260817: 来源配送收货单
    private final DistributionMapper distributionMapper;
    private final DistributionDetailMapper distributionDetailMapper;
    // add by codebuddy 20260819: 配送退货单上传 ERP（return.create）调用 ERP
    private final ErpClient erpClient;

    public PageResult<DistributionReturn> page(long pageNum, long pageSize, String keyword, Integer siteId) {
        LambdaQueryWrapper<DistributionReturn> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(keyword), w -> w
                        .like(DistributionReturn::getDistributionReturnNo, keyword)
                        .or().like(DistributionReturn::getErpNo, keyword)
                        .or().like(DistributionReturn::getCreator, keyword))
                .eq(siteId != null, DistributionReturn::getSiteId, siteId)
                .orderByDesc(DistributionReturn::getId);
        Page<DistributionReturn> p = returnMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        fillShopNames(p.getRecords());
        fillDcNames(p.getRecords());
        return new PageResult<>(p.getRecords(), p.getTotal(), pageNum, pageSize);
    }

    public BillDto<DistributionReturn, DistributionReturnDetail> getById(Integer id) {
        DistributionReturn header = returnMapper.selectById(id);
        if (header == null) {
            return null;
        }
        BillDto<DistributionReturn, DistributionReturnDetail> dto = new BillDto<>();
        dto.setHeader(header);
        fillShopNames(java.util.Collections.singletonList(header));
        fillDcNames(java.util.Collections.singletonList(header));
        dto.setDetails(fillGoodsNames(detailMapper.selectList(new LambdaQueryWrapper<DistributionReturnDetail>()
                .eq(DistributionReturnDetail::getDistributionReturnId, header.getDistributionReturnNo())
                .orderByAsc(DistributionReturnDetail::getSeq, DistributionReturnDetail::getId))));
        return dto;
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(BillDto<DistributionReturn, DistributionReturnDetail> dto) {
        DistributionReturn header = dto.getHeader();
        if (header.getId() == null) {
            // add by codebuddy 20260817: 创建时间默认为当前时间
            if (header.getCreateDatetime() == null) {
                header.setCreateDatetime(LocalDateTime.now());
            }
            if (!StringUtils.hasText(header.getDistributionReturnNo())) {
                header.setDistributionReturnNo(nextNo(header.getSiteId()));
            }
            // add by codebuddy 20260817: 新建配送退货单时按门店商品配置表取出配送中心写入主表
            if (header.getDeliveryCenterId() == null) {
                fillDeliveryCenterFromGoodsShop(header, dto.getDetails());
            }
            returnMapper.insert(header);
        } else {
            returnMapper.updateById(header);
        }

        detailMapper.delete(new LambdaQueryWrapper<DistributionReturnDetail>()
                .eq(DistributionReturnDetail::getDistributionReturnId, header.getDistributionReturnNo()));
        List<DistributionReturnDetail> details = dto.getDetails();
        if (details != null) {
            int seq = 1;
            for (DistributionReturnDetail d : details) {
                d.setId(null);
                d.setDistributionReturnId(header.getDistributionReturnNo());
                if (d.getSeq() == null) {
                    d.setSeq(seq++);
                }
                detailMapper.insert(d);
            }
        }
    }

    // add by codebuddy 20260817: 确认配送退货单，状态置为Y
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Integer id) {
        DistributionReturn header = returnMapper.selectById(id);
        if (header == null) {
            throw new BusinessException("配送退货单不存在");
        }
        header.setStatus("Y");
        returnMapper.updateById(header);
    }

    // add by codebuddy 20260817: 提交配送退货单（需先确认），状态置为TJ
    @Transactional(rollbackFor = Exception.class)
    public void submit(Integer id) {
        DistributionReturn header = returnMapper.selectById(id);
        if (header == null) {
            throw new BusinessException("配送退货单不存在");
        }
        if ("TJ".equals(header.getStatus())) {
            throw new BusinessException("配送退货单已提交，不能重复提交");
        }
        if (!"Y".equals(header.getStatus())) {
            throw new BusinessException("请先确认配送退货单后再提交");
        }
        header.setStatus("TJ");
        returnMapper.updateById(header);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        DistributionReturn header = returnMapper.selectById(id);
        if (header != null) {
            detailMapper.delete(new LambdaQueryWrapper<DistributionReturnDetail>()
                    .eq(DistributionReturnDetail::getDistributionReturnId, header.getDistributionReturnNo()));
        }
        returnMapper.deleteById(id);
    }

    // add by codebuddy 20260817: 查询可选来源的配送收货单（已确认/已提交/已上传），供配送退货单新增时选择
    public List<Distribution> listSourceDistributions(Integer siteId) {
        LambdaQueryWrapper<Distribution> wrapper = new LambdaQueryWrapper<Distribution>()
                .eq(siteId != null, Distribution::getSiteId, siteId)
                .in(Distribution::getStatus, "Y", "TJ", "SC")
                .orderByDesc(Distribution::getId);
        List<Distribution> list = distributionMapper.selectList(wrapper);
        fillDistDcNames(list);
        return list;
    }

    // add by codebuddy 20260817: 获取来源配送收货单及明细（带出商品编号/名称），供配送退货单新增时带出
    public BillDto<Distribution, DistributionDetail> getSourceDistribution(Integer id) {
        Distribution header = distributionMapper.selectById(id);
        if (header == null) {
            return null;
        }
        BillDto<Distribution, DistributionDetail> dto = new BillDto<>();
        dto.setHeader(header);
        fillDistDcNames(java.util.Collections.singletonList(header));
        List<DistributionDetail> details = distributionDetailMapper.selectList(
                new LambdaQueryWrapper<DistributionDetail>()
                        .eq(DistributionDetail::getDistributionId, header.getDistributionNo())
                        .orderByAsc(DistributionDetail::getSeq, DistributionDetail::getId));
        if (!details.isEmpty()) {
            List<Integer> ids = details.stream().map(DistributionDetail::getItemId)
                    .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
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
        dto.setDetails(details);
        return dto;
    }

    // add by codebuddy 20260817: 批量填充配送收货单的配送中心编号与名称
    private void fillDistDcNames(List<Distribution> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Integer> ids = list.stream().map(Distribution::getDeliveryCenterId)
                .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
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

    private static final String NO_PREFIX = "PSTH";

    // add by codebuddy 20260817: 单号前缀加入门店编号
    /** 生成配送退货单号：PSTH + 门店编号 + 年月日 + 三位流水号，例如 PSTHSH000120260817001 */
    private String nextNo(Integer siteId) {
        String shopNo = getShopNo(siteId);
        String prefix = NO_PREFIX + shopNo + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long count = returnMapper.selectCount(new LambdaQueryWrapper<DistributionReturn>()
                .likeRight(DistributionReturn::getDistributionReturnNo, prefix));
        return CodeGenerator.genDaily(NO_PREFIX + shopNo, count.intValue());
    }

    private String getShopNo(Integer siteId) {
        if (siteId == null) {
            return "";
        }
        Shop shop = shopMapper.selectById(siteId);
        return shop == null ? "" : shop.getShopNo();
    }

    private void fillShopNames(List<DistributionReturn> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Integer> ids = list.stream().map(DistributionReturn::getSiteId).filter(java.util.Objects::nonNull)
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

    // add by codebuddy 20260817: 按门店商品配置表取出配送中心写入配送退货单主表（取第一条明细配置的配送中心）
    private void fillDeliveryCenterFromGoodsShop(DistributionReturn header, List<DistributionReturnDetail> details) {
        if (header.getSiteId() == null || details == null || details.isEmpty()) {
            return;
        }
        for (DistributionReturnDetail d : details) {
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
    private void fillDcNames(List<DistributionReturn> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Integer> ids = list.stream().map(DistributionReturn::getDeliveryCenterId).filter(java.util.Objects::nonNull)
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

    private List<DistributionReturnDetail> fillGoodsNames(List<DistributionReturnDetail> details) {
        if (details == null || details.isEmpty()) {
            return details;
        }
        List<Integer> ids = details.stream().map(DistributionReturnDetail::getItemId).filter(java.util.Objects::nonNull)
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

    // add by codebuddy 20260819: 配送退货单上传 ERP（return.create），供定时任务批量调用
    public void uploadSubmittedToErp() {
        List<DistributionReturn> list = returnMapper.selectList(new LambdaQueryWrapper<DistributionReturn>()
                .in(DistributionReturn::getStatus, "Y", "TJ")
                .ne(DistributionReturn::getAbortUpload, "Y"));
        for (DistributionReturn r : list) {
            try {
                uploadOneToErp(r.getId());
            } catch (Exception e) {
                log.warn("配送退货单[{}]上传ERP失败: {}", r.getDistributionReturnNo(), e.getMessage());
            }
        }
    }

    // add by codebuddy 20260819: 单张配送退货单上传 ERP（return.create）
    @Transactional(rollbackFor = Exception.class)
    public void uploadOneToErp(Integer id) {
        BillDto<DistributionReturn, DistributionReturnDetail> dto = getById(id);
        if (dto == null || dto.getHeader() == null) {
            throw new BusinessException("配送退货单不存在");
        }
        DistributionReturn header = dto.getHeader();
        if (!"Y".equals(header.getStatus()) && !"TJ".equals(header.getStatus())) {
            throw new BusinessException("仅已确认/已提交的配送退货单可上传ERP");
        }
        if ("Y".equals(header.getAbortUpload())) {
            throw new BusinessException("该配送退货单已终止上传");
        }
        List<DistributionReturnDetail> details = dto.getDetails();
        if (details == null || details.isEmpty()) {
            throw new BusinessException("配送退货单无明细，无法上传");
        }

        String siteNo = header.getSiteNo();
        if (!StringUtils.hasText(siteNo)) {
            throw new BusinessException("配送退货单缺少门店编码(site_no)，无法上传");
        }
        String dcNo = header.getDeliveryCenterNo();
        if (!StringUtils.hasText(dcNo)) {
            throw new BusinessException("配送退货单缺少配送中心编号，无法上传");
        }

        // 来源配送收货单的 ERP 单号，作为退货单的 source_no
        String sourceNo = null;
        if (StringUtils.hasText(header.getSourceDistributionNo())) {
            Distribution src = distributionMapper.selectOne(new LambdaQueryWrapper<Distribution>()
                    .eq(Distribution::getDistributionNo, header.getSourceDistributionNo())
                    .last("LIMIT 1"));
            if (src != null && StringUtils.hasText(src.getErpNo())) {
                sourceNo = src.getErpNo();
            }
        }
        if (!StringUtils.hasText(sourceNo)) {
            throw new BusinessException("来源配送收货单尚未上传ERP或未取得ERP单号，无法上传退货单");
        }

        List<Map<String, Object>> returnDetail = new ArrayList<>();
        for (DistributionReturnDetail dt : details) {
            Map<String, Object> d = new HashMap<>();
            d.put("seq", dt.getSeq());
            d.put("source_no", sourceNo);
            d.put("source_seq", dt.getSeq());
            d.put("po_template_no", "NC001");
            d.put("item_no", dt.getItemNo());
            d.put("item_batch_no", dt.getItemBatchNo());
            d.put("item_barcode", dt.getItemBarcode());
            d.put("packing_unit", StringUtils.hasText(dt.getUnit()) ? dt.getUnit() : "");
            d.put("base_unit", StringUtils.hasText(dt.getUnit()) ? dt.getUnit() : "");
            d.put("packing_qty", dt.getQty() != null ? dt.getQty().toPlainString() : "0");
            d.put("base_qty", dt.getQty() != null ? dt.getQty().toPlainString() : "0");
            d.put("warehouse_no", siteNo);
            d.put("distri_price", dt.getPrice() != null ? dt.getPrice().toPlainString() : "0");
            d.put("distri_amount", dt.getAmount() != null ? dt.getAmount().toPlainString() : "0");
            d.put("amount", dt.getAmount() != null ? dt.getAmount().toPlainString() : "");
            returnDetail.add(d);
        }

        Map<String, Object> ret = new HashMap<>();
        ret.put("version", "3.0");
        ret.put("front_no", header.getDistributionReturnNo());
        ret.put("site_no", siteNo);
        ret.put("transfer_out_site_no", dcNo);
        ret.put("source_no", sourceNo);
        ret.put("remark", "系统自动退货");
        ret.put("creator", siteNo);
        ret.put("approve_no", siteNo);
        ret.put("posted_no", siteNo);
        ret.put("transfer_type", "2");
        ret.put("is_multiple_receipt", "0");
        ret.put("return_detail", returnDetail);

        Map<String, Object> parameter = new HashMap<>();
        parameter.put("return", ret);
        Map<String, Object> stdData = new HashMap<>();
        stdData.put("parameter", parameter);

        JsonNode result;
        try {
            result = erpClient.request("return.create", siteNo, stdData);
        } catch (Exception e) {
            String msg = e.getMessage();
            header.setErrorMessage(msg);
            header.setLastTransmitAt(LocalDateTime.now());
            returnMapper.updateById(header);
            throw new BusinessException("上传ERP失败: " + msg);
        }
        String erpNo = result.path("parameter").path("return").path("ERP_no").asText(null);
        header.setErpNo(StringUtils.hasText(erpNo) ? erpNo : sourceNo);
        header.setStatus("SC");
        header.setErrorMessage(null);
        header.setLastTransmitAt(LocalDateTime.now());
        returnMapper.updateById(header);
    }
}

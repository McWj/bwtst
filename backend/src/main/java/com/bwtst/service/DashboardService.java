package com.bwtst.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.bwtst.common.BusinessException;
import com.bwtst.entity.DeliveryShipment;
import com.bwtst.entity.Distribution;
import com.bwtst.entity.DistributionCenter;
import com.bwtst.entity.DistributionReturn;
import com.bwtst.entity.Purchase;
import com.bwtst.entity.PurchaseInbound;
import com.bwtst.entity.PurchaseReturn;
import com.bwtst.entity.Requisition;
import com.bwtst.entity.Shop;
import com.bwtst.entity.WarehouseReturn;
import com.bwtst.mapper.DeliveryShipmentMapper;
import com.bwtst.mapper.DistributionCenterMapper;
import com.bwtst.mapper.DistributionMapper;
import com.bwtst.mapper.DistributionReturnMapper;
import com.bwtst.mapper.PurchaseInboundMapper;
import com.bwtst.mapper.PurchaseMapper;
import com.bwtst.mapper.PurchaseReturnMapper;
import com.bwtst.mapper.RequisitionMapper;
import com.bwtst.mapper.ShopMapper;
import com.bwtst.mapper.WarehouseReturnMapper;
import com.bwtst.task.ErpUploadTask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

// add by codebuddy 20260817: 首页大屏统计数据汇总
// add by codebuddy 20260818: 支持 总部(hq)/门店(shop)/配送中心(dc) 三种视角，区分门店数据与配送中心数据
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RequisitionMapper requisitionMapper;
    private final DeliveryShipmentMapper deliveryShipmentMapper;
    private final DistributionMapper distributionMapper;
    private final PurchaseMapper purchaseMapper;
    private final DistributionReturnMapper distributionReturnMapper;
    private final PurchaseReturnMapper purchaseReturnMapper;
    private final PurchaseInboundMapper purchaseInboundMapper;
    private final WarehouseReturnMapper warehouseReturnMapper;
    private final ShopMapper shopMapper;
    private final DistributionCenterMapper distributionCenterMapper;
    private final ErpUploadTask erpUploadTask;

    /**
     * 数据总览
     * @param view   hq-总部(所有数据)，shop-门店(仅本门店数据)，dc-配送中心(仅本配送中心数据)
     * @param shopId 门店视角下的当前门店id
     * @param dcId   配送中心视角下的当前配送中心id
     */
    public Map<String, Object> overview(String view, Integer shopId, Integer dcId) {
        String v = normalize(view);
        boolean isShop = "shop".equals(v);
        boolean isDc = "dc".equals(v);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("view", v);
        // add by codebuddy 20260818: 数据总览只统计单据主表，按单据自带归属 id（门店 siteId / 配送中心 deliveryCenterId）过滤，
        // 不关联 goods_shop / ptemplate_shop 等基础资料表，避免全表扫描超时

        // add by codebuddy 20260818: 门店数据（门店视角按 siteId 过滤）
        if (!isDc) {
            Map<String, Object> store = new LinkedHashMap<>();
            store.put("requisition", requisitionStats(isShop, shopId));
            store.put("distribution", simpleScopedStats(distributionMapper, Distribution::getSiteId, Distribution::getStatus, Distribution::getErpNo, Distribution::getAbortUpload, shopId, isShop));
            store.put("purchase", simpleScopedStats(purchaseMapper, Purchase::getSiteId, Purchase::getStatus, Purchase::getErpNo, Purchase::getAbortUpload, shopId, isShop));
            store.put("distributionReturn", simpleScopedStats(distributionReturnMapper, DistributionReturn::getSiteId, DistributionReturn::getStatus, DistributionReturn::getErpNo, DistributionReturn::getAbortUpload, shopId, isShop));
            store.put("purchaseReturn", simpleScopedStats(purchaseReturnMapper, PurchaseReturn::getSiteId, PurchaseReturn::getStatus, PurchaseReturn::getErpNo, PurchaseReturn::getAbortUpload, shopId, isShop));
            data.put("storeData", store);
        }

        // add by codebuddy 20260818: 配送中心数据（配送中心视角按 deliveryCenterId 过滤）
        if (!isShop) {
            Map<String, Object> dc = new LinkedHashMap<>();
            dc.put("deliveryShipment", deliveryShipmentStats(isDc, dcId));
            dc.put("purchaseInbound", simpleScopedStats(purchaseInboundMapper, PurchaseInbound::getDeliveryCenterId, PurchaseInbound::getStatus, PurchaseInbound::getErpNo, PurchaseInbound::getAbortUpload, dcId, isDc));
            dc.put("warehouseReturn", simpleScopedStats(warehouseReturnMapper, WarehouseReturn::getDeliveryCenterId, WarehouseReturn::getStatus, WarehouseReturn::getErpNo, WarehouseReturn::getAbortUpload, dcId, isDc));
            data.put("dcData", dc);
        }

        data.put("taskConfig", taskConfig());
        return data;
    }

    private String normalize(String view) {
        if ("shop".equals(view) || "dc".equals(view) || "hq".equals(view)) {
            return view;
        }
        return "hq";
    }

    /** 要货单：按状态分布 + 已提交未上传数量 */
    private Map<String, Object> requisitionStats(boolean scoped, Integer shopId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total", requisitionMapper.selectCount(scoped(Requisition::getSiteId, shopId, scoped)));
        map.put("unConfirmed", requisitionMapper.selectCount(scoped(Requisition::getSiteId, shopId, scoped)
                .and(w -> w.isNull(Requisition::getStatus).or().eq(Requisition::getStatus, ""))));
        map.put("confirmed", requisitionMapper.selectCount(scoped(Requisition::getSiteId, shopId, scoped)
                .eq(Requisition::getStatus, "Y")));
        map.put("submitted", requisitionMapper.selectCount(scoped(Requisition::getSiteId, shopId, scoped)
                .eq(Requisition::getStatus, "TJ")));
        map.put("uploaded", requisitionMapper.selectCount(scoped(Requisition::getSiteId, shopId, scoped)
                .eq(Requisition::getStatus, "SC")));
        // 已提交但未上传（erp_no 为空，排除已终止上传的单据）
        map.put("unUploaded", requisitionMapper.selectCount(scoped(Requisition::getSiteId, shopId, scoped)
                .eq(Requisition::getStatus, "TJ")
                .and(w -> w.isNull(Requisition::getErpNo).or().eq(Requisition::getErpNo, ""))
                .and(w -> w.isNull(Requisition::getAbortUpload).or().ne(Requisition::getAbortUpload, "Y"))));
        return map;
    }

    // add by codebuddy 20260817: 配送发货单：未上传 = 已发货(Y)且 erp_no 为空
    private Map<String, Object> deliveryShipmentStats(boolean scoped, Integer dcId) {
        long total = deliveryShipmentMapper.selectCount(scoped(DeliveryShipment::getDeliveryCenterId, dcId, scoped));
        LambdaQueryWrapper<DeliveryShipment> unW = scoped(DeliveryShipment::getDeliveryCenterId, dcId, scoped);
        unW.eq(DeliveryShipment::getStatus, "Y")
                .and(w -> w.isNull(DeliveryShipment::getErpNo).or().eq(DeliveryShipment::getErpNo, ""))
                // add by codebuddy 20260818: 排除已终止上传的单据
                .and(w -> w.isNull(DeliveryShipment::getAbortUpload).or().ne(DeliveryShipment::getAbortUpload, "Y"));
        return simpleStats(total, deliveryShipmentMapper.selectCount(unW));
    }

    /** 通用：按状态分布 + 未上传数量；状态语义：空-未确认 Y-已确认 TJ-已提交 SC-已上传 */
    private <T> Map<String, Object> simpleScopedStats(BaseMapper<T> mapper, SFunction<T, ?> scopeCol,
                                                       SFunction<T, String> statusCol, SFunction<T, ?> erpCol,
                                                       SFunction<T, String> abortCol,
                                                       Integer id, boolean scoped) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total", mapper.selectCount(scoped(scopeCol, id, scoped)));
        map.put("unConfirmed", mapper.selectCount(scoped(scopeCol, id, scoped)
                .and(w -> w.isNull(statusCol).or().eq(statusCol, ""))));
        map.put("confirmed", mapper.selectCount(scoped(scopeCol, id, scoped).eq(statusCol, "Y")));
        map.put("submitted", mapper.selectCount(scoped(scopeCol, id, scoped).eq(statusCol, "TJ")));
        map.put("uploaded", mapper.selectCount(scoped(scopeCol, id, scoped).eq(statusCol, "SC")));
        // 已提交但未上传（erp_no 为空，排除已终止上传的单据）
        map.put("unUploaded", mapper.selectCount(scoped(scopeCol, id, scoped)
                .eq(statusCol, "TJ")
                .and(w -> w.isNull(erpCol).or().eq(erpCol, ""))
                .and(w -> w.isNull(abortCol).or().ne(abortCol, "Y"))));
        return map;
    }

    /** 按视角作用域构建查询条件（scoped=true 时按归属id过滤，id为空则取 -1 保证结果为0） */
    private <T> LambdaQueryWrapper<T> scoped(SFunction<T, ?> col, Integer id, boolean scoped) {
        LambdaQueryWrapper<T> w = new LambdaQueryWrapper<>();
        if (scoped) {
            w.eq(col, id != null ? id : -1);
        }
        return w;
    }

    // add by codebuddy 20260818: 统计某归属维度下 selectCol 的去重数量（selectCount+groupBy/distinct 在该版本不可用，改用 selectObjs 后内存去重）
    // add by codebuddy 20260818: 该方法当前已不被 overview 调用，保留供其他场景使用
    private <T> long distinctCount(BaseMapper<T> mapper, SFunction<T, ?> selectCol, SFunction<T, ?> whereCol, Integer id, boolean scoped) {
        LambdaQueryWrapper<T> w = new LambdaQueryWrapper<>();
        w.select(selectCol);
        if (scoped) {
            w.eq(whereCol, id != null ? id : -1);
        }
        return mapper.selectObjs(w).stream().filter(Objects::nonNull).distinct().count();
    }

    private Map<String, Object> simpleStats(long total, long unUploaded) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total", total);
        map.put("unUploaded", unUploaded);
        return map;
    }

    // add by codebuddy 20260818: 查询某单据类型的已提交但未上传 ERP 明细（含 ERP 报错信息、传输时间、终止状态）
    public List<Map<String, Object>> unuploadedDetail(String type, String view, Integer shopId, Integer dcId) {
        String v = normalize(view);
        boolean isShop = "shop".equals(v);
        boolean isDc = "dc".equals(v);
        switch (type == null ? "" : type) {
            case "requisition":
                return unuploadedList(requisitionMapper, Requisition::getId, Requisition::getSiteId,
                        Requisition::getStatus, Requisition::getErpNo, Requisition::getRequisitionNo,
                        Requisition::getCreateDatetime, Requisition::getErrorMessage,
                        Requisition::getLastTransmitAt, Requisition::getAbortUpload,
                        shopId, isShop, "TJ", this::shopNo, this::shopName);
            case "distribution":
                return unuploadedList(distributionMapper, Distribution::getId, Distribution::getSiteId,
                        Distribution::getStatus, Distribution::getErpNo, Distribution::getDistributionNo,
                        Distribution::getCreateDatetime, Distribution::getErrorMessage,
                        Distribution::getLastTransmitAt, Distribution::getAbortUpload,
                        shopId, isShop, "TJ", this::shopNo, this::shopName);
            case "purchase":
                return unuploadedList(purchaseMapper, Purchase::getId, Purchase::getSiteId,
                        Purchase::getStatus, Purchase::getErpNo, Purchase::getPurchaseNo,
                        Purchase::getCreateDatetime, Purchase::getErrorMessage,
                        Purchase::getLastTransmitAt, Purchase::getAbortUpload,
                        shopId, isShop, "TJ", this::shopNo, this::shopName);
            case "distributionReturn":
                return unuploadedList(distributionReturnMapper, DistributionReturn::getId, DistributionReturn::getSiteId,
                        DistributionReturn::getStatus, DistributionReturn::getErpNo, DistributionReturn::getDistributionReturnNo,
                        DistributionReturn::getCreateDatetime, DistributionReturn::getErrorMessage,
                        DistributionReturn::getLastTransmitAt, DistributionReturn::getAbortUpload,
                        shopId, isShop, "TJ", this::shopNo, this::shopName);
            case "purchaseReturn":
                return unuploadedList(purchaseReturnMapper, PurchaseReturn::getId, PurchaseReturn::getSiteId,
                        PurchaseReturn::getStatus, PurchaseReturn::getErpNo, PurchaseReturn::getPurchaseReturnNo,
                        PurchaseReturn::getCreateDatetime, PurchaseReturn::getErrorMessage,
                        PurchaseReturn::getLastTransmitAt, PurchaseReturn::getAbortUpload,
                        shopId, isShop, "TJ", this::shopNo, this::shopName);
            case "deliveryShipment":
                return unuploadedList(deliveryShipmentMapper, DeliveryShipment::getId, DeliveryShipment::getDeliveryCenterId,
                        DeliveryShipment::getStatus, DeliveryShipment::getErpNo, DeliveryShipment::getShipmentNo,
                        DeliveryShipment::getCreateDatetime, DeliveryShipment::getErrorMessage,
                        DeliveryShipment::getLastTransmitAt, DeliveryShipment::getAbortUpload,
                        dcId, isDc, "Y", this::dcNo, this::dcName);
            case "purchaseInbound":
                return unuploadedList(purchaseInboundMapper, PurchaseInbound::getId, PurchaseInbound::getDeliveryCenterId,
                        PurchaseInbound::getStatus, PurchaseInbound::getErpNo, PurchaseInbound::getPurchaseNo,
                        PurchaseInbound::getCreateDatetime, PurchaseInbound::getErrorMessage,
                        PurchaseInbound::getLastTransmitAt, PurchaseInbound::getAbortUpload,
                        dcId, isDc, "TJ", this::dcNo, this::dcName);
            case "warehouseReturn":
                return unuploadedList(warehouseReturnMapper, WarehouseReturn::getId, WarehouseReturn::getDeliveryCenterId,
                        WarehouseReturn::getStatus, WarehouseReturn::getErpNo, WarehouseReturn::getReturnNo,
                        WarehouseReturn::getCreateDatetime, WarehouseReturn::getErrorMessage,
                        WarehouseReturn::getLastTransmitAt, WarehouseReturn::getAbortUpload,
                        dcId, isDc, "TJ", this::dcNo, this::dcName);
            default:
                return Collections.emptyList();
        }
    }

    // add by codebuddy 20260818: 终止单据自动上传（终止后不再出现在未上传列表，定时任务也会跳过）
    public void abortUpload(String type, Integer id) {
        switch (type == null ? "" : type) {
            case "requisition":
                setAbort(requisitionMapper, Requisition::getId, Requisition::getAbortUpload, id);
                break;
            case "distribution":
                setAbort(distributionMapper, Distribution::getId, Distribution::getAbortUpload, id);
                break;
            case "purchase":
                setAbort(purchaseMapper, Purchase::getId, Purchase::getAbortUpload, id);
                break;
            case "distributionReturn":
                setAbort(distributionReturnMapper, DistributionReturn::getId, DistributionReturn::getAbortUpload, id);
                break;
            case "purchaseReturn":
                setAbort(purchaseReturnMapper, PurchaseReturn::getId, PurchaseReturn::getAbortUpload, id);
                break;
            case "deliveryShipment":
                setAbort(deliveryShipmentMapper, DeliveryShipment::getId, DeliveryShipment::getAbortUpload, id);
                break;
            case "purchaseInbound":
                setAbort(purchaseInboundMapper, PurchaseInbound::getId, PurchaseInbound::getAbortUpload, id);
                break;
            case "warehouseReturn":
                setAbort(warehouseReturnMapper, WarehouseReturn::getId, WarehouseReturn::getAbortUpload, id);
                break;
            default:
                throw new BusinessException("未知单据类型: " + type);
        }
    }

    // add by codebuddy 20260818: 通用设置单据终止上传标记（abort_upload = 'Y'）
    private <T> void setAbort(BaseMapper<T> mapper, SFunction<T, ?> idCol, SFunction<T, String> abortCol,
                              Integer id) {
        T entity = mapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("单据不存在: id=" + id);
        }
        try {
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<T> uw =
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
            uw.eq(idCol, id).set(abortCol, "Y");
            mapper.update(null, uw);
        } catch (Exception e) {
            throw new BusinessException("更新单据终止状态失败: " + e.getMessage());
        }
    }

    private <T> List<Map<String, Object>> unuploadedList(BaseMapper<T> mapper,
                                                         SFunction<T, ?> idCol, SFunction<T, Integer> scopeCol,
                                                         SFunction<T, String> statusCol, SFunction<T, String> erpNoCol,
                                                         SFunction<T, String> billNoCol, SFunction<T, LocalDateTime> createTimeCol,
                                                         SFunction<T, String> errorCol,
                                                         SFunction<T, LocalDateTime> lastTransmitCol, SFunction<T, String> abortCol,
                                                         Integer orgId, boolean scoped, String status,
                                                         Function<Integer, String> orgNoResolver,
                                                         Function<Integer, String> orgNameResolver) {
        LambdaQueryWrapper<T> wrapper = scoped(scopeCol, orgId, scoped)
                .eq(statusCol, status)
                .and(w -> w.isNull(erpNoCol).or().eq(erpNoCol, ""))
                // add by codebuddy 20260818: 已终止上传的单据不再显示到未上传列表
                .and(w -> w.isNull(abortCol).or().ne(abortCol, "Y"))
                .orderByDesc(createTimeCol);
        return mapper.selectList(wrapper).stream().map(item -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", idCol.apply(item));
            map.put("billNo", billNoCol.apply(item));
            Integer scopeId = scopeCol.apply(item);
            map.put("orgId", scopeId);
            map.put("orgNo", orgNoResolver.apply(scopeId));
            map.put("orgName", orgNameResolver.apply(scopeId));
            map.put("createDatetime", formatTime(createTimeCol.apply(item)));
            map.put("lastTransmitAt", formatTime(lastTransmitCol.apply(item)));
            map.put("abortUpload", abortCol.apply(item));
            map.put("errorMessage", errorCol.apply(item));
            return map;
        }).collect(Collectors.toList());
    }

    private String shopName(Integer shopId) {
        if (shopId == null) return "";
        Shop shop = shopMapper.selectById(shopId);
        return shop == null ? "" : shop.getShopName();
    }

    // add by codebuddy 20260818: 门店/配送中心编号也返回给前端展示
    private String shopNo(Integer shopId) {
        if (shopId == null) return "";
        Shop shop = shopMapper.selectById(shopId);
        return shop == null ? "" : shop.getShopNo();
    }

    private String dcName(Integer dcId) {
        if (dcId == null) return "";
        DistributionCenter dc = distributionCenterMapper.selectById(dcId);
        return dc == null ? "" : dc.getDcName();
    }

    private String dcNo(Integer dcId) {
        if (dcId == null) return "";
        DistributionCenter dc = distributionCenterMapper.selectById(dcId);
        return dc == null ? "" : dc.getDcNo();
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private Map<String, Object> taskConfig() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("intervalMinutes", erpUploadTask.getIntervalMinutes());
        map.put("lastRunAt", erpUploadTask.getLastRunAt());
        map.put("nextRunAt", erpUploadTask.getNextRunAt());
        // add by codebuddy 20260818: 定时任务开关状态
        map.put("enabled", erpUploadTask.isEnabled());
        return map;
    }
}

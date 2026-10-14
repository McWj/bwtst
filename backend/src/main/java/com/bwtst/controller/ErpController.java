package com.bwtst.controller;

import com.bwtst.common.BusinessException;
import com.bwtst.common.ErpClient;
import com.bwtst.common.Result;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

// add by codebuddy 20260923: ERP 代理接口。
// 前端不再直连 ERP（浏览器无法按租户选择账套），统一由后端按当前租户的账套配置转发。
@RestController
@RequestMapping("/api/erp")
@RequiredArgsConstructor
public class ErpController {

    private final ErpClient erpClient;

    /** 门店可用额度查询（ERP: credit.query） */
    @PostMapping("/credit-query")
    public Result<JsonNode> creditQuery(@RequestBody Map<String, Object> body) {
        String shopNo = requireShopNo(body.get("shopNo"));
        Map<String, Object> transfer = new LinkedHashMap<>();
        transfer.put("customer", shopNo);
        Map<String, Object> parameter = new LinkedHashMap<>();
        parameter.put("transfer", transfer);
        return Result.ok(erpClient.request("credit.query", shopNo, Map.of("parameter", parameter)));
    }

    /** 要货单确认/提交（ERP: requisition.credit.deduct） */
    @PostMapping("/requisition-credit-deduct")
    public Result<JsonNode> requisitionCreditDeduct(@RequestBody Map<String, Object> body) {
        String shopNo = requireShopNo(body.get("shopNo"));
        Object requisition = body.get("requisition");
        if (requisition == null) {
            throw new BusinessException("缺少要货单报文");
        }
        Map<String, Object> parameter = new LinkedHashMap<>();
        parameter.put("requisition", requisition);
        return Result.ok(erpClient.request("requisition.credit.deduct", shopNo, Map.of("parameter", parameter)));
    }

    private String requireShopNo(Object value) {
        String shopNo = value == null ? null : String.valueOf(value);
        if (!StringUtils.hasText(shopNo)) {
            throw new BusinessException("缺少门店编码");
        }
        return shopNo.trim();
    }
}

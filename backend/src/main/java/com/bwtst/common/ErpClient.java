package com.bwtst.common;

import com.bwtst.entity.SysTenant;
import com.bwtst.mapper.SysTenantMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

// add by codebuddy 20260817: 后端调用 ERP 接口的通用客户端，报文结构与前端 erp.ts 保持一致
// add by codebuddy 20260923: SaaS 改造——接口地址与账套凭证按当前租户从 sys_tenant 解析，未配置时回落全局默认
@Slf4j
@Component
@RequiredArgsConstructor
public class ErpClient {

    private final ObjectMapper objectMapper;
    private final SysTenantMapper tenantMapper;

    // add by codebuddy 20260923: 全局默认账套配置（租户未单独配置时回落使用）
    @Value("${bwtst.erp.url:http://106.14.92.92/wtoptst/ws/r/awsp920}")
    private String defaultUrl;

    @Value("${bwtst.erp.digi-key:85d966d2f533fca68f83da455edabcbe}")
    private String defaultDigiKey;

    @Value("${bwtst.erp.prod:BWZT}")
    private String defaultProd;

    @Value("${bwtst.erp.acct:tiptop}")
    private String defaultAcct;

    @Value("${bwtst.erp.ent-id:10}")
    private String defaultEntId;

    /** 解析后的 ERP 账套配置 */
    public record ErpConfig(String url, String digiKey, String prod, String acct, String entId) {
    }

    /**
     * 按当前租户解析 ERP 配置。
     * add by codebuddy 20260923: 租户未配置的字段逐项回落到全局默认，保证历史数据与未配置租户仍可用。
     */
    public ErpConfig resolveConfig() {
        SysTenant t = null;
        try {
            t = tenantMapper.selectById(TenantContext.getTenantId());
        } catch (Exception e) {
            log.warn("读取租户 ERP 配置失败，改用全局默认配置: {}", e.getMessage());
        }
        return new ErpConfig(
                pick(t == null ? null : t.getErpUrl(), defaultUrl),
                pick(t == null ? null : t.getErpDigiKey(), defaultDigiKey),
                pick(t == null ? null : t.getErpProd(), defaultProd),
                pick(t == null ? null : t.getErpAcct(), defaultAcct),
                pick(t == null ? null : t.getErpEntId(), defaultEntId));
    }

    private static String pick(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    /** 17 位时间戳 yyyyMMddHHmmssSSS */
    private static String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    /** 构建完整 ERP 报文 */
    private Map<String, Object> buildPayload(ErpConfig config, String service, String shopNo, Object stdData) {
        Map<String, Object> host = new LinkedHashMap<>();
        host.put("prod", config.prod());
        host.put("ver", "1.0");
        host.put("ip", "192.168.0.1");
        host.put("id", "postst");
        host.put("lang", "zh_CN");
        host.put("timestamp", timestamp());
        host.put("acct", config.acct());

        Map<String, Object> digiService = new LinkedHashMap<>();
        // add by codebuddy 20260820: prod 值为外部 ERP 系统约定的产品码，不可修改
        digiService.put("prod", "T100");
        digiService.put("name", service);
        digiService.put("srvver", "1.0");
        digiService.put("ip", "192.0.0.1");
        digiService.put("id", "topprd");

        Map<String, Object> dataKey = new LinkedHashMap<>();
        dataKey.put("EntId", config.entId());
        dataKey.put("CompanyId", shopNo);

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("digi-type", "sync");
        header.put("digi-host", host);
        header.put("digi-service", digiService);
        header.put("digi-key", config.digiKey());
        header.put("digi-datakey", dataKey);

        Map<String, Object> stdDataNode = new LinkedHashMap<>();
        stdDataNode.put("std_data", stdData == null ? new LinkedHashMap<>() : stdData);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("digi-header", header);
        root.put("digi-body", stdDataNode);
        return root;
    }

    /**
     * 调用 ERP 接口，成功(execution.code==0)时返回 digi-body.std_data，失败抛 BusinessException。
     * add by codebuddy 20260923: 目标地址与凭证按当前租户动态解析。
     */
    public JsonNode request(String service, String shopNo, Object stdData) {
        ErpConfig config = resolveConfig();
        try {
            String json = objectMapper.writeValueAsString(buildPayload(config, service, shopNo, stdData));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(config.url()))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode stdDataNode = root.path("digi-body").path("std_data");
            JsonNode execution = stdDataNode.path("execution");
            if (!"0".equals(execution.path("code").asText())) {
                String msg = execution.path("description").asText("");
                if (!StringUtils.hasText(msg)) {
                    msg = execution.path("message").asText("");
                }
                throw new BusinessException("ERP 接口调用失败(" + service + "): " + msg);
            }
            return stdDataNode;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("ERP 接口调用异常({})", service, e);
            throw new BusinessException("ERP 接口调用异常(" + service + "): " + e.getMessage());
        }
    }

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
}

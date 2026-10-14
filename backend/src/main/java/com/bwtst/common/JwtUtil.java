package com.bwtst.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

// add by codebuddy 20260923: SaaS 登录令牌工具（HS256 JWT，仅用 JDK 自带加密库实现，避免引入额外依赖）
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final ObjectMapper objectMapper;

    /** 签名密钥，生产环境务必通过 bwtst.jwt.secret 覆盖 */
    @Value("${bwtst.jwt.secret:bwtst-saas-change-me-in-production}")
    private String secret;

    /** 令牌有效期（分钟），默认 12 小时 */
    @Value("${bwtst.jwt.expire-minutes:720}")
    private long expireMinutes;

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    /** 签发令牌，payload 携带租户id、用户id、登录账户、平台超管标志 */
    public String createToken(Long tenantId, Integer userId, String username, boolean platformAdmin) {
        try {
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            long now = System.currentTimeMillis() / 1000;
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("tid", tenantId);
            payload.put("uid", userId);
            payload.put("sub", username);
            // add by codebuddy 20260923: 平台超级管理员标志
            payload.put("pa", platformAdmin ? 1 : 0);
            payload.put("iat", now);
            payload.put("exp", now + expireMinutes * 60);

            String signingInput = ENCODER.encodeToString(objectMapper.writeValueAsBytes(header))
                    + "." + ENCODER.encodeToString(objectMapper.writeValueAsBytes(payload));
            return signingInput + "." + ENCODER.encodeToString(hmac(signingInput));
        } catch (Exception e) {
            throw new BusinessException("签发登录令牌失败: " + e.getMessage());
        }
    }

    /**
     * 校验并解析令牌。
     * 签名不匹配、算法被篡改、已过期、格式非法一律返回 null。
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> parse(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }
        byte[] actual;
        try {
            actual = DECODER.decode(parts[2]);
        } catch (Exception e) {
            return null;
        }
        // 防时序攻击的签名比对
        if (!MessageDigest.isEqual(hmac(parts[0] + "." + parts[1]), actual)) {
            return null;
        }
        try {
            Map<String, Object> header = objectMapper.readValue(DECODER.decode(parts[0]), Map.class);
            // 固定算法，拒绝 alg=none 之类的篡改
            if (!"HS256".equals(header.get("alg"))) {
                return null;
            }
            Map<String, Object> payload = objectMapper.readValue(DECODER.decode(parts[1]), Map.class);
            Object exp = payload.get("exp");
            if (!(exp instanceof Number) || ((Number) exp).longValue() < System.currentTimeMillis() / 1000) {
                return null;
            }
            return payload;
        } catch (Exception e) {
            return null;
        }
    }

    public long getExpireMinutes() {
        return expireMinutes;
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("JWT 签名计算失败", e);
        }
    }
}

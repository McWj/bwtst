package com.bwtst.common;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

// add by codebuddy 20260923: 密码哈希工具——新密码使用 PBKDF2 加盐哈希，同时兼容历史无盐 MD5 并支持自动升级
public final class PasswordUtil {

    private static final String PBKDF2_PREFIX = "pbkdf2";
    private static final String SEPARATOR = "$";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /** 生成 PBKDF2 哈希，格式 pbkdf2$迭代次数$盐$哈希 */
    public static String hash(String raw) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] key = pbkdf2(raw, salt, ITERATIONS);
        Base64.Encoder enc = Base64.getEncoder();
        return PBKDF2_PREFIX + SEPARATOR + ITERATIONS + SEPARATOR
                + enc.encodeToString(salt) + SEPARATOR + enc.encodeToString(key);
    }

    /** 校验密码：兼容历史 MD5 存储与新的 PBKDF2 存储 */
    public static boolean matches(String raw, String stored) {
        if (raw == null || stored == null || stored.isEmpty()) {
            return false;
        }
        if (stored.startsWith(PBKDF2_PREFIX + SEPARATOR)) {
            String[] parts = stored.split("\\" + SEPARATOR);
            if (parts.length != 4) {
                return false;
            }
            try {
                int iterations = Integer.parseInt(parts[1]);
                byte[] salt = Base64.getDecoder().decode(parts[2]);
                byte[] expected = Base64.getDecoder().decode(parts[3]);
                byte[] actual = pbkdf2(raw, salt, iterations);
                return java.security.MessageDigest.isEqual(expected, actual);
            } catch (Exception e) {
                return false;
            }
        }
        // 历史数据为无盐 MD5
        return Md5Util.md5(raw).equalsIgnoreCase(stored);
    }

    /** 是否为需要升级的历史弱哈希（无盐 MD5） */
    public static boolean needsUpgrade(String stored) {
        return stored != null && !stored.isEmpty() && !stored.startsWith(PBKDF2_PREFIX + SEPARATOR);
    }

    private static byte[] pbkdf2(String raw, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(raw.toCharArray(), salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("密码哈希计算失败", e);
        }
    }
}

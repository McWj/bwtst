package com.bwtst.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// add by codebuddy 20260819: MD5 工具（用于用户密码单向哈希存储）
public class Md5Util {

    public static String md5(String plain) {
        if (plain == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString((b & 0xff));
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 算法不可用", e);
        }
    }

    public static void main(String[] args) {
        // 默认管理员密码 123456 的 MD5
        System.out.println(md5("123456"));
    }
}

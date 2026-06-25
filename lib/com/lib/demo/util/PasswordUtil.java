package com.lib.demo.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * 密码工具 — PBKDF2WithHmacSHA256 + 随机盐哈希。
 * 格式: base64(salt) + ":" + base64(hash)
 *
 * PBKDF2 通过多轮迭代故意降低哈希速度，大幅增加 GPU 暴力破解成本。
 */
public final class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SALT_BYTES = 16;
    /** PBKDF2 迭代次数（10 万轮，约 50ms/次，兼顾安全与体验） */
    private static final int PBKDF2_ITERATIONS = 100_000;
    private static final int HASH_BYTES = 32; // 256 bits

    private PasswordUtil() {}

    /** 对明文密码加盐哈希，返回 "salt:hash" 格式 */
    public static String hash(String plainPassword) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(plainPassword.toCharArray(), salt);
        return Base64.getEncoder().encodeToString(salt) + ":"
                + Base64.getEncoder().encodeToString(hash);
    }

    /** 验证明文密码是否匹配已存储的哈希 */
    public static boolean verify(String plainPassword, String storedHash) {
        if (storedHash == null || !storedHash.contains(":")) return false;
        String[] parts = storedHash.split(":", 2);
        byte[] salt;
        byte[] expected;
        try {
            salt = Base64.getDecoder().decode(parts[0]);
            expected = Base64.getDecoder().decode(parts[1]);
        } catch (IllegalArgumentException e) {
            return false;
        }
        byte[] actual = pbkdf2(plainPassword.toCharArray(), salt);
        // 常量时间比较，防时序攻击
        return java.security.MessageDigest.isEqual(expected, actual);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, HASH_BYTES * 8);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("PBKDF2WithHmacSHA256 not available", e);
        }
    }
}

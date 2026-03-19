package com.gestao.lafemme.api.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Component
public class StringEncryptUtils {

    private static final Logger log = LoggerFactory.getLogger(StringEncryptUtils.class);

    @Value("${api.public.app-secret}")
    private String appSecret;

    private static final String PLATFORM_KEY = "lf$plat#2025@core";
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    public String encrypt(String value) {
        return encrypt(0, value);
    }

    /**
     *
     * @param size  comprimento mínimo do token de saída
     * @param value valor a cifrar (ex: String.valueOf(userId))
     * @return token Base64URL — salve na tabela Configuracao
     */
    public String encrypt(int size, String value) {
        try {
            SecretKey aesKey = buildAesKey(appSecret, PLATFORM_KEY);
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(combined);

            if (size > 0 && token.length() < size) {
                StringBuilder sb = new StringBuilder(token);
                sb.append('.');
                while (sb.length() < size) {
                    sb.append('0');
                }
                return sb.toString();
            }

            return token;
        } catch (Exception e) {
            throw new RuntimeException("Encryption error", e);
        }
    }

    public String decrypt(String token) {
        try {
            String sanitizedToken = token.trim();
            int dotIndex = sanitizedToken.indexOf('.');
            if (dotIndex != -1) {
                sanitizedToken = sanitizedToken.substring(0, dotIndex);
            }

            byte[] combined = Base64.getUrlDecoder().decode(sanitizedToken);
            if (combined.length <= GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid token size");
            }

            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
            byte[] cipherText = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

            SecretKey aesKey = buildAesKey(appSecret, PLATFORM_KEY);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption error", e);
        }
    }

    public static String hashSha256(String value) {
        return hashAlgorithm(value, "SHA-256");
    }

    public static String hashMd5(String value) {
        return hashAlgorithm(value, "MD5");
    }

    public String hashSha512(String value) {
        return hashAlgorithm(value, "SHA-512");
    }

    public String encodeBase64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public String decodeBase64(String base64) {
        return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
    }

    public String encodeBase64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public String decodeBase64Url(String base64) {
        return new String(Base64.getUrlDecoder().decode(base64), StandardCharsets.UTF_8);
    }

    public static String generateRandomHash() {
        return hashSha256(UUID.randomUUID().toString() + System.currentTimeMillis());
    }

    public static String generateRandomToken(int length) {
        byte[] randomBytes = new byte[length];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes).substring(0, length);
    }

    public static boolean compareHashes(String hash1, String hash2) {
        return MessageDigest.isEqual(
                hash1.getBytes(StandardCharsets.UTF_8),
                hash2.getBytes(StandardCharsets.UTF_8));
    }

    public static String obfuscateString(String value, int visibleStart, int visibleEnd) {
        if (value == null || value.length() <= visibleStart + visibleEnd) {
            return value;
        }
        String start = value.substring(0, visibleStart);
        String end = value.substring(value.length() - visibleEnd);
        String middle = "*".repeat(value.length() - visibleStart - visibleEnd);
        return start + middle + end;
    }

    private static SecretKey buildAesKey(String a, String b) {
        try {
            List<String> keys = Arrays.asList(a, b);
            keys.sort(String::compareTo);
            String combined = String.join("|", keys);
            byte[] raw = MessageDigest.getInstance("SHA-256").digest(combined.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(Arrays.copyOf(raw, 32), "AES");
        } catch (Exception e) {
            throw new RuntimeException("Key derivation error", e);
        }
    }

    private static String hashAlgorithm(String value, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hashing error: " + algorithm, e);
        }
    }

    public void encrypt(int size, Long valor) {
        encrypt(2, valor.toString());
    }

    public void encrypt(int size, Integer valor) {
        encrypt(2, valor.toString());
    }

    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    public static boolean validateSignature(String xSignature, String xRequestId, String dataId, String secret) {
        try {
            String ts = null;
            String v1 = null;
            for (String part : xSignature.split(",")) {
                if (part.trim().startsWith("ts="))
                    ts = part.trim().substring(3);
                else if (part.trim().startsWith("v1="))
                    v1 = part.trim().substring(3);
            }

            if (ts == null || v1 == null) {
                return false;
            }

            String manifest = "id:" + dataId + ";request-id:" + xRequestId + ";ts:" + ts + ";";

            javax.crypto.Mac sha256_HMAC = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secret_key = new javax.crypto.spec.SecretKeySpec(secret.getBytes("UTF-8"),
                    "HmacSHA256");
            sha256_HMAC.init(secret_key);

            byte[] hash = sha256_HMAC.doFinal(manifest.getBytes("UTF-8"));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }

            String calculated = hexString.toString();
            boolean valid = calculated.equals(v1);

            return valid;
        } catch (Exception e) {
            log.error("❌ Erro na validação de assinatura", e);
            return false;
        }
    }
}
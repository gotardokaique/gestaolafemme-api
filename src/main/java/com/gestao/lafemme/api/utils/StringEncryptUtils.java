package com.gestao.lafemme.api.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
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

/**
 * Utilitários de criptografia, hashing, codificação e geração de tokens.
 * <p>
 * Criptografia simétrica via AES-256/GCM com IV aleatório por operação.
 * Validação de assinatura HMAC-SHA256 compatível com Mercado Pago.
 * </p>
 *
 * @author Kaique Gotardo
 * @since 1.0
 */
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
                StringBuilder sb = new StringBuilder(token).append('.');
                while (sb.length() < size)
                    sb.append('0');
                return sb.toString();
            }

            return token;
        } catch (Exception e) {
            throw new RuntimeException("Encryption error", e);
        }
    }

    public String encrypt(int size, Long valor) {
        return encrypt(size, valor.toString());
    }

    public String encrypt(int size, Integer valor) {
        return encrypt(size, valor.toString());
    }

    public String decrypt(String token) {
        try {
            String sanitized = token.trim();
            int dot = sanitized.indexOf('.');
            if (dot != -1)
                sanitized = sanitized.substring(0, dot);

            byte[] combined = Base64.getUrlDecoder().decode(sanitized);
            if (combined.length <= GCM_IV_LENGTH)
                throw new IllegalArgumentException("Invalid token size");

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

    public static String hashSha512(String value) {
        return hashAlgorithm(value, "SHA-512");
    }

    /** @deprecated MD5 é criptograficamente quebrado — use {@link #hashSha256}. */
    @Deprecated
    public static String hashMd5(String value) {
        return hashAlgorithm(value, "MD5");
    }

    public static String encodeBase64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String decodeBase64(String base64) {
        return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
    }

    public static String encodeBase64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String decodeBase64Url(String base64) {
        return new String(Base64.getUrlDecoder().decode(base64), StandardCharsets.UTF_8);
    }

    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    public static String generateRandomHash() {
        return hashSha256(UUID.randomUUID() + String.valueOf(System.currentTimeMillis()));
    }

    public static String generateRandomToken(int length) {
        if (length <= 0)
            throw new IllegalArgumentException("Length deve ser > 0");
        int byteCount = (int) Math.ceil(length * 0.75) + 4;
        byte[] randomBytes = new byte[byteCount];
        new SecureRandom().nextBytes(randomBytes);
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return encoded.substring(0, Math.min(length, encoded.length()));
    }

    public static boolean compareHashes(String hash1, String hash2) {
        if (hash1 == null || hash2 == null)
            return false;
        return MessageDigest.isEqual(
                hash1.getBytes(StandardCharsets.UTF_8),
                hash2.getBytes(StandardCharsets.UTF_8));
    }

    public static String obfuscateString(String value, int visibleStart, int visibleEnd) {
        if (value == null || value.length() <= visibleStart + visibleEnd)
            return value;
        return value.substring(0, visibleStart)
                + "*".repeat(value.length() - visibleStart - visibleEnd)
                + value.substring(value.length() - visibleEnd);
    }

    // ─── Assinatura HMAC (Mercado Pago) ───────────────────────────────────────

    /**
     * Valida assinatura HMAC-SHA256 do webhook do Mercado Pago.
     * Usa comparação em tempo constante para prevenir timing attack.
     *
     * @param xSignature header {@code x-signature} da requisição
     * @param xRequestId header {@code x-request-id} da requisição
     * @param dataId     {@code data.id} do payload do webhook
     * @param secret     secret configurado no painel do Mercado Pago
     */
    public static boolean validateSignature(String xSignature, String xRequestId,
            String dataId, String secret) {
        try {
            String ts = null;
            String v1 = null;

            for (String part : xSignature.split(",")) {
                String trimmed = part.trim();
                if (trimmed.startsWith("ts="))
                    ts = trimmed.substring(3);
                else if (trimmed.startsWith("v1="))
                    v1 = trimmed.substring(3);
            }

            if (ts == null || v1 == null) {
                log.warn("[Signature] Header x-signature malformado: {}", xSignature);
                return false;
            }

            String manifest = "id:" + dataId + ";request-id:" + xRequestId + ";ts:" + ts + ";";

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1)
                    hex.append('0');
                hex.append(h);
            }

            return MessageDigest.isEqual(
                    hex.toString().getBytes(StandardCharsets.UTF_8),
                    v1.getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            log.error("[Signature] Erro na validação: {}", e.getMessage());
            return false;
        }
    }

    private static SecretKey buildAesKey(String a, String b) {
        try {
            List<String> keys = Arrays.asList(a, b);
            keys.sort(String::compareTo);
            String combined = String.join("|", keys);
            byte[] raw = MessageDigest.getInstance("SHA-256")
                    .digest(combined.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(Arrays.copyOf(raw, 32), "AES");
        } catch (Exception e) {
            throw new RuntimeException("Key derivation error", e);
        }
    }

    private static String hashAlgorithm(String value, String algorithm) {
        try {
            byte[] hash = MessageDigest.getInstance(algorithm)
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1)
                    hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hashing error: " + algorithm, e);
        }
    }
}
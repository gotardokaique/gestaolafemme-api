package com.gestao.lafemme.api.utils;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class SecuritySanitizer {

    private SecuritySanitizer() {}

    private static final int MAX_VALUE_LEN = 255;

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_.]*$");

    private static final List<Pattern> SQLI = List.of(
        Pattern.compile("(\\bor\\b\\s+\\d+\\s*=\\s*\\d+)|(\\band\\b\\s+\\d+\\s*=\\s*\\d+)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(--|/\\*|\\*/|;\\s*$)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bunion\\s+(all\\s+)?select", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(exec|execute|sp_executesql|sleep|benchmark|waitfor\\s+delay)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(drop|delete|insert|update|truncate|alter)\\s+table\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("('\\s*(or|and)\\s*')", Pattern.CASE_INSENSITIVE),
        Pattern.compile("%27|%22|%3b|%2d%2d|%23", Pattern.CASE_INSENSITIVE)
    );

    private static final List<Pattern> XSS = List.of(
        Pattern.compile("<\\s*(script|iframe|object|embed|svg|img|link|meta|form)[^>]*", Pattern.CASE_INSENSITIVE),
        Pattern.compile("</\\s*script", Pattern.CASE_INSENSITIVE),
        Pattern.compile("on\\w+\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("javascript\\s*:|vbscript\\s*:|data\\s*:\\s*text/html", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<!\\[CDATA\\[", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(%3c|%3e|&#x?[0-9a-f]+;)", Pattern.CASE_INSENSITIVE)
    );

    private static final List<Pattern> INJECTION = List.of(
        Pattern.compile("\\$\\{.+\\}|#\\{.+\\}|\\{\\{.+\\}\\}", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\$\\{(jndi|lower|upper)[^}]*\\}", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(Runtime|ProcessBuilder|getRuntime|exec\\s*\\(|forName\\s*\\()", Pattern.CASE_INSENSITIVE),
        Pattern.compile("%00|\\\\x00|\\\\u0000")
    );

    // ── Texto ────────────────────────────────────────────────────────────────

    public static boolean looksLikeSqlInjection(String value) {
        if (value == null) return false;
        String decoded = decodeAll(value);
        return SQLI.stream().anyMatch(p -> p.matcher(decoded).find());
    }

    public static boolean looksLikeXss(String value) {
        if (value == null) return false;
        String decoded = decodeAll(value);
        return XSS.stream().anyMatch(p -> p.matcher(decoded).find());
    }

    public static boolean looksLikeInjection(String value) {
        if (value == null) return false;
        String decoded = decodeAll(value);
        return INJECTION.stream().anyMatch(p -> p.matcher(decoded).find());
    }

    public static boolean isMalicious(String value) {
        return looksLikeSqlInjection(value) || looksLikeXss(value) || looksLikeInjection(value);
    }

    public static void assertSafe(String campo, String valor) {
        if (valor == null || valor.isBlank()) return;
        if (isMalicious(valor))
            throw new InputMaliciosoException("Campo '%s' contém conteúdo inválido.".formatted(campo));
    }

    public static String safeValueOrNull(String raw) {
        if (raw == null) return null;
        String v = Normalizer.normalize(raw.trim(), Normalizer.Form.NFKC)
                             .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
        if (v.isBlank()) return null;
        if (v.length() > MAX_VALUE_LEN) v = v.substring(0, MAX_VALUE_LEN);
        return isMalicious(v) ? null : v;
    }

    public static String safeFieldOrNull(String field, Set<String> allowedFields) {
        if (field == null) return null;
        String f = field.trim();
        if (f.isEmpty() || !SAFE_IDENTIFIER.matcher(f).matches()) return null;
        if (allowedFields != null && !allowedFields.isEmpty() && !allowedFields.contains(f)) return null;
        return looksLikeSqlInjection(f) ? null : f;
    }

    public static String stripHtml(String input) {
        if (input == null) return null;
        return input.replaceAll("<[^>]*>", "");
    }

    // ── Arquivo ──────────────────────────────────────────────────────────────

    public static void assertArquivoSeguro(MultipartFile file, Set<String> tiposPermitidos, long maxBytes) {
        if (file == null || file.isEmpty())
            throw new InputMaliciosoException("Arquivo não pode ser vazio.");

        if (file.getSize() > maxBytes)
            throw new InputMaliciosoException(
                "Arquivo excede o tamanho máximo de " + FileUtils.formatFileSize(maxBytes));

        try {
            byte[] bytes = file.getBytes();
            FileUtils.validarArquivo(bytes, FileUtils.sanitizarNomeArquivo(file.getOriginalFilename()), tiposPermitidos, maxBytes);
        } catch (IOException e) {
            throw new InputMaliciosoException("Erro ao ler arquivo.");
        } catch (IllegalArgumentException e) {
            throw new InputMaliciosoException(e.getMessage());
        }
    }

    public static void assertImagemSegura(MultipartFile file) {
        assertArquivoSeguro(file, FileUtils.ALLOWED_IMAGE_TYPES, FileUtils.MAX_SIZE_AVATAR);
    }

    public static void assertDocumentoSeguro(MultipartFile file) {
        assertArquivoSeguro(file, FileUtils.ALLOWED_DOCUMENT_TYPES, FileUtils.MAX_SIZE_DOCUMENT);
    }

    // ── Interno ──────────────────────────────────────────────────────────────

    private static String decodeAll(String value) {
        String decoded = value;
        try { decoded = URLDecoder.decode(value, StandardCharsets.UTF_8); } catch (Exception ignored) {}
        try { decoded = decoded + " " + URLDecoder.decode(decoded, StandardCharsets.UTF_8); } catch (Exception ignored) {}
        return decoded.toLowerCase(Locale.ROOT);
    }

    // ── Exceção ──────────────────────────────────────────────────────────────

    public static class InputMaliciosoException extends RuntimeException {
        public InputMaliciosoException(String message) {
            super(message);
        }
    }
}
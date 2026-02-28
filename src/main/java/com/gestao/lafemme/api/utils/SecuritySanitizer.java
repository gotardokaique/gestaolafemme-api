package com.gestao.lafemme.api.utils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class SecuritySanitizer {

    private SecuritySanitizer() {}

    private static final Pattern SAFE_IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_.]*$");
    
    private static final int MAX_VALUE_LEN = 255;

    private static final Pattern SQLI_HINTS = Pattern.compile(
            "(?i)(\\bor\\b\\s+\\d+\\s*=\\s*\\d+)|" +
            "(?i)(\\band\\b\\s+\\d+\\s*=\\s*\\d+)|" +
            "(?i)(--|/\\*|\\*/)|" +
            "(?i)(;\\s*$)|" +
            "(?i)(\\bunion\\s+(all\\s+)?select)|" +
            "(?i)(\\b(exec|execute|sp_executesql)\\b)|" +
            "(?i)(\\b(sleep|benchmark|waitfor\\s+delay)\\b)|" +
            "(?i)(\\b(drop|delete|insert|update|truncate|alter)\\s+table\\b)"
    );

    private static final Pattern XSS_HINTS = Pattern.compile(
            "(?i)(<\\s*script\\b)|" +
            "(?i)(<\\s*iframe\\b)|" +
            "(?i)(<\\s*object\\b)|" +
            "(?i)(on\\w+\\s*=)|" +
            "(?i)(javascript\\s*:)|" +
            "(?i)(vbscript\\s*:)|" +
            "(?i)(data\\s*:\\s*text/html)|" +
            "(?i)(<\\s*img\\b)|" +
            "(?i)(<\\s*svg\\b)|" +
            "(?i)(<!\\[CDATA\\[)"
    );

    public static String safeFieldOrNull(String field, Set<String> allowedFields) {
        if (field == null) return null;
        String f = field.trim();

        if (f.isEmpty()) return null;

        if (!SAFE_IDENTIFIER_PATTERN.matcher(f).matches()) {
            return null;
        }

        if (allowedFields != null && !allowedFields.isEmpty() && !allowedFields.contains(f)) {
            return null;
        }

        if (looksLikeSqlInjection(f)) {
            return null;
        }

        return f;
    }

    public static String safeValueOrNull(String raw) {
        if (raw == null) return null;

        String v = raw.trim();
        if (v.isEmpty()) return null;

        v = Normalizer.normalize(v, Normalizer.Form.NFKC);
        v = v.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

        if (v.length() > MAX_VALUE_LEN) {
            v = v.substring(0, MAX_VALUE_LEN); 
        }

        if (looksLikeSqlInjection(v) || looksLikeXss(v)) {
            return null;
        }

        return v;
    }

    public static String stripHtml(String input) {
        if (input == null) return null;
        return input.replaceAll("<[^>]*>", ""); 
    }

    public static boolean looksLikeSqlInjection(String v) {
        if (v == null) return false;
        String s = v.toLowerCase(Locale.ROOT);
        return SQLI_HINTS.matcher(s).find();
    }

    public static boolean looksLikeXss(String v) {
        if (v == null) return false;
        String s = v.toLowerCase(Locale.ROOT);
        return XSS_HINTS.matcher(s).find();
    }
}
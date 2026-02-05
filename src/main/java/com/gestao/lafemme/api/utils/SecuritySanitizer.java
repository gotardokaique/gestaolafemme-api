package com.gestao.lafemme.api.utils;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class SecuritySanitizer {

    private SecuritySanitizer() {}

    private static final Pattern INVALID_FIELD = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");
    private static final int MAX_VALUE_LEN = 120;

    private static final Pattern SQLI_HINTS = Pattern.compile(
            "(?i)(\\bor\\b\\s+\\d+\\s*=\\s*\\d+)|" +     // or 1=1
            "(?i)(\\band\\b\\s+\\d+\\s*=\\s*\\d+)|" +    // and 1=1
            "(?i)(--|/\\*|\\*/)|" +                      // comentários SQL
            "(?i)(;\\s*$)|" +                            // ; no fim
            "(?i)(\\bunion\\b\\s+\\bselect\\b)|" +       // union select
            "(?i)(\\bdrop\\b|\\bdelete\\b|\\binsert\\b|\\bupdate\\b|\\btruncate\\b)" // DDL/DML
    );

    private static final Pattern XSS_HINTS = Pattern.compile(
            "(?i)(<\\s*script\\b)|" +                    // <script
            "(?i)(on\\w+\\s*=)|" +                       // onload= onclick=
            "(?i)(javascript\\s*:)|" +                    // javascript:
            "(?i)(<\\s*img\\b)|" +                       // <img
            "(?i)(<\\s*svg\\b)|" +                       // <svg
            "(?i)(&#\\d+;|&#x[0-9a-f]+;)"                // entidades numéricas comuns em bypass
    );

    /** Campo seguro: SEM ponto, SEM caminho, SEM SQL/JPQL injection via "field". */
    public static String safeFieldOrNull(String field, Set<String> allowedFields) {
        if (field == null) return null;
        field = field.trim();

        // bloqueia path tipo unidade.id, nome) or 1=1, etc
        if (!INVALID_FIELD.matcher(field).matches()) return null;

        // whitelist é a proteção nº1
        if (allowedFields != null && !allowedFields.isEmpty() && !allowedFields.contains(field)) {
            return null;
        }

        return field;
    }

    /**
     * Valor seguro:
     * - normaliza unicode
     * - remove chars de controle
     * - limita tamanho
     * - rejeita (ou neutraliza) padrões óbvios de SQLi/XSS
     *
     * Se detectar suspeita, retorna null (melhor que "limpar").
     */
    public static String safeValueOrNull(String raw) {
        if (raw == null) return null;

        String v = raw.trim();
        if (v.isEmpty()) return null;

        // normaliza unicode (evita bypass com caracteres "parecidos")
        v = Normalizer.normalize(v, Normalizer.Form.NFKC);

        // remove caracteres de controle invisíveis
        v = v.replaceAll("\\p{Cntrl}", "");

        // limita tamanho (evita payload gigante)
        if (v.length() > MAX_VALUE_LEN) {
            v = v.substring(0, MAX_VALUE_LEN);
        }

        // heurística: se parece ataque, rejeita
        if (looksLikeSqlInjection(v) || looksLikeXss(v)) {
            return null;
        }

        return v;
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

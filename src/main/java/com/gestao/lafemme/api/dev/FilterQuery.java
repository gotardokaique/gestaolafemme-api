package com.gestao.lafemme.api.dev;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gestao.lafemme.api.db.Condicao;
import com.gestao.lafemme.api.db.WhereDB;
import com.gestao.lafemme.api.utils.SecuritySanitizer;

public class FilterQuery {

    // Spring vai bindar ?f=...&f=... aqui automaticamente
    private List<String> f = new ArrayList<>();

    // itens parseados e sanitizados
    private final List<FilterItem> filters = new ArrayList<>();

    // opcional: whitelist (se você setar, ele valida também)
    private Set<String> allowedFields;

    public static class FilterItem {
        private final String field;
        private final Condicao condition;
        private final Object value;

        public FilterItem(String field, Condicao condition, Object value) {
            this.field = field;
            this.condition = condition;
            this.value = value;
        }

        public String getField() { return field; }
        public Condicao getCondition() { return condition; }
        public Object getValue() { return value; }
    }

    public List<String> getF() {
        return f;
    }

    /**
     * IMPORTANTE:
     * Qualquer lugar que usar FilterQuery via request -> passa aqui.
     * Aqui a gente SEMPRE sanitiza e parseia.
     */
    public void setF(List<String> f) {
        this.f = (f != null ? f : new ArrayList<>());
        parseAndSanitize(); // <- sempre
    }

    public List<FilterItem> getFilters() {
        return filters;
    }

    /** Opcional, mas recomendado: restringe quais campos podem filtrar. */
    public FilterQuery allowOnly(Set<String> allowedFields) {
        this.allowedFields = allowedFields;
        parseAndSanitize(); // revalida
        return this;
    }

    public void applyTo(WhereDB where) {
        if (where == null) return;
        for (FilterItem fi : filters) {
            where.add(fi.getField(), fi.getCondition(), fi.getValue());
        }
    }

    private void parseAndSanitize() {
        filters.clear();
        if (this.f == null || this.f.isEmpty()) return;

        for (String raw : this.f) {
            if (raw == null || raw.trim().isEmpty()) continue;

            // formato: field|COND|value
            String[] parts = raw.split("\\|", 3);
            if (parts.length < 3) continue;

            String fieldRaw = parts[0];
            String condRaw = parts[1];
            String valueRaw = parts[2];

            String field = SecuritySanitizer.safeFieldOrNull(fieldRaw, allowedFields);
            if (field == null) continue;

            Condicao cond;
            try {
                cond = Condicao.valueOf(condRaw.trim());
            } catch (Exception e) {
                continue;
            }

            String safeValue = SecuritySanitizer.safeValueOrNull(valueRaw);
            if (safeValue == null) continue;

            Object parsed = parseValue(safeValue);
            filters.add(new FilterItem(field, cond, parsed));
        }
    }

    private Object parseValue(String value) {
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;

        try {
            if (value.matches("^-?\\d+$")) {
                long l = Long.parseLong(value);
                if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) return (int) l;
                return l;
            }
        } catch (Exception ignored) {}

        try {
            if (value.matches("^-?\\d+\\.\\d+$")) {
                return new BigDecimal(value);
            }
        } catch (Exception ignored) {}

        return value;
    }
}

package com.gestao.lafemme.api.dev;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.gestao.lafemme.api.db.Condicao;
import com.gestao.lafemme.api.db.WhereDB;
import com.gestao.lafemme.api.utils.SecuritySanitizer;

public class FilterQuery {

    private static final Pattern INT_PATTERN = Pattern.compile("^-?\\d{1,18}$"); 
    private static final Pattern DEC_PATTERN = Pattern.compile("^-?\\d+\\.\\d+$");

    private List<String> f = new ArrayList<>();
    
    private List<FilterItem> cachedFilters;
    private Set<String> allowedFields;
    private boolean dirty = true;

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

    public void setF(List<String> f) {
        this.f = (f != null ? f : new ArrayList<>());
        this.dirty = true;
    }

    public List<String> getF() {
        return f;
    }

    public FilterQuery allowOnly(Set<String> allowedFields) {
        this.allowedFields = allowedFields;
        this.dirty = true; 
        return this;
    }

    public List<FilterItem> getFilters() {
        if (dirty || cachedFilters == null) {
            reprocess();
        }
        return Collections.unmodifiableList(cachedFilters);
    }

    public void applyTo(WhereDB where) {
        if (where == null) return;
        
        List<FilterItem> items = getFilters(); 
        for (FilterItem fi : items) {
            where.add(fi.getField(), fi.getCondition(), fi.getValue());
        }
    }

    private void reprocess() {
        cachedFilters = new ArrayList<>();
        dirty = false;
        
        if (this.f == null || this.f.isEmpty()) return;

        for (String raw : this.f) {
            if (raw == null || raw.isBlank()) continue;

            String[] parts = raw.split("\\|", 3);
            if (parts.length < 3) continue;

            String fieldRaw = parts[0];
            String condRaw = parts[1];
            String valueRaw = parts[2];

            String field = SecuritySanitizer.safeFieldOrNull(fieldRaw, allowedFields);
            if (field == null) continue;

            Condicao cond = safeEnum(condRaw);
            if (cond == null) continue;

            String safeValue = SecuritySanitizer.safeValueOrNull(valueRaw);
            if (safeValue == null) continue;

            Object parsed = parseValue(safeValue);
            cachedFilters.add(new FilterItem(field, cond, parsed));
        }
    }

    private Condicao safeEnum(String raw) {
        if (raw == null) return null;
        try {
            return Condicao.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null; 
        }
    }

    private Object parseValue(String value) {
        if ("true".equalsIgnoreCase(value)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(value)) return Boolean.FALSE;

        if (INT_PATTERN.matcher(value).matches()) {
            try {
                long l = Long.parseLong(value);
                if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) return (int) l;
                return l;
            } catch (NumberFormatException ignored) {
                return value;
            }
        }

        if (DEC_PATTERN.matcher(value).matches()) {
            return new BigDecimal(value);
        }

        return value;
    }
}
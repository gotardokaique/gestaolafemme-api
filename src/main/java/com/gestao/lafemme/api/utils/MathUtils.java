package com.gestao.lafemme.api.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

/**
 * Utilitários matemáticos.
 * <p>
 * Inclui helpers para cálculos de desconto, parcelas, percentual e formatação
 * BRL.
 * </p>
 * 
 * @author Kaique Gotardo
 * @since 1.0
 */

public final class MathUtils {

    private static final int DEFAULT_SCALE = 2;
    private static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;
    private static final Locale LOCALE_BR = new Locale("pt", "BR");
    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private MathUtils() {
    }

    public static BigDecimal sum(Number... values) {
        validate(values);
        BigDecimal result = BigDecimal.ZERO;
        for (Number n : values) {
            if (n != null)
                result = result.add(toBigDecimal(n));
        }
        return result;
    }

    public static BigDecimal subtract(Number first, Number... values) {
        Objects.requireNonNull(first, "Primeiro valor não pode ser null");
        validate(values);
        BigDecimal result = toBigDecimal(first);
        for (Number n : values) {
            if (n != null)
                result = result.subtract(toBigDecimal(n));
        }
        return result;
    }

    public static BigDecimal multiply(Number... values) {
        validate(values);
        BigDecimal result = BigDecimal.ONE;
        boolean hasValue = false;
        for (Number n : values) {
            if (n != null) {
                result = result.multiply(toBigDecimal(n));
                hasValue = true;
            }
        }
        return hasValue ? result : BigDecimal.ZERO;
    }

    public static BigDecimal divide(Number dividend, Number... divisors) {
        Objects.requireNonNull(dividend, "Dividendo não pode ser null");
        validate(divisors);
        BigDecimal result = toBigDecimal(dividend);
        for (Number n : divisors) {
            if (n == null)
                continue;
            BigDecimal divisor = toBigDecimal(n);
            if (isZero(divisor))
                throw new ArithmeticException("Divisão por zero");
            result = result.divide(divisor, DEFAULT_SCALE, DEFAULT_ROUNDING);
        }
        return result;
    }

    public static BigDecimal safeDivide(Number dividend, Number divisor) {
        if (dividend == null || divisor == null)
            return BigDecimal.ZERO;
        BigDecimal d = toBigDecimal(divisor);
        if (isZero(d))
            return BigDecimal.ZERO;
        return toBigDecimal(dividend).divide(d, DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal round(Number value) {
        return round(value, DEFAULT_SCALE);
    }

    public static BigDecimal round(Number value, int scale) {
        if (value == null)
            return BigDecimal.ZERO;
        return toBigDecimal(value).setScale(scale, DEFAULT_ROUNDING);
    }

    public static BigDecimal average(Number... values) {
        validate(values);
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (Number n : values) {
            if (n != null) {
                sum = sum.add(toBigDecimal(n));
                count++;
            }
        }
        return count == 0 ? BigDecimal.ZERO
                : sum.divide(BigDecimal.valueOf(count), DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal sumCollection(Collection<? extends Number> values) {
        if (values == null || values.isEmpty())
            return BigDecimal.ZERO;
        return values.stream()
                .filter(Objects::nonNull)
                .map(MathUtils::toBigDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal averageCollection(Collection<? extends Number> values) {
        if (values == null || values.isEmpty())
            return BigDecimal.ZERO;
        BigDecimal total = sumCollection(values);
        long count = values.stream().filter(Objects::nonNull).count();
        return safeDivide(total, count);
    }

    public static BigDecimal max(Number... values) {
        validate(values);
        BigDecimal max = null;
        for (Number n : values) {
            if (n == null)
                continue;
            BigDecimal bd = toBigDecimal(n);
            if (max == null || bd.compareTo(max) > 0)
                max = bd;
        }
        return max != null ? max : BigDecimal.ZERO;
    }

    public static BigDecimal min(Number... values) {
        validate(values);
        BigDecimal min = null;
        for (Number n : values) {
            if (n == null)
                continue;
            BigDecimal bd = toBigDecimal(n);
            if (min == null || bd.compareTo(min) < 0)
                min = bd;
        }
        return min != null ? min : BigDecimal.ZERO;
    }

    public static BigDecimal clamp(Number value, Number minVal, Number maxVal) {
        if (value == null)
            return toBigDecimal(minVal);
        BigDecimal v = toBigDecimal(value);
        BigDecimal lo = toBigDecimal(minVal);
        BigDecimal hi = toBigDecimal(maxVal);
        if (v.compareTo(lo) < 0)
            return lo;
        if (v.compareTo(hi) > 0)
            return hi;
        return v;
    }

    public static BigDecimal percentage(Number value, Number percent) {
        if (value == null || percent == null)
            return BigDecimal.ZERO;
        return toBigDecimal(value)
                .multiply(toBigDecimal(percent))
                .divide(CEM, DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal percentageOf(Number part, Number total) {
        if (part == null || total == null)
            return BigDecimal.ZERO;
        return safeDivide(toBigDecimal(part).multiply(CEM), total);
    }

    public static BigDecimal applyDiscount(Number value, Number discountPercent) {
        if (value == null)
            return BigDecimal.ZERO;
        BigDecimal desconto = percentage(value, discountPercent);
        return round(subtract(value, desconto));
    }

    public static BigDecimal applyIncrease(Number value, Number increasePercent) {
        if (value == null)
            return BigDecimal.ZERO;
        BigDecimal acrescimo = percentage(value, increasePercent);
        return round(sum(value, acrescimo));
    }

    public static BigDecimal percentageChange(Number from, Number to) {
        if (from == null || to == null)
            return BigDecimal.ZERO;
        BigDecimal f = toBigDecimal(from);
        if (isZero(f))
            return BigDecimal.ZERO;
        return safeDivide(toBigDecimal(to).subtract(f).multiply(CEM), f);
    }

    public static BigDecimal[] installments(Number value, int count) {
        if (value == null || count <= 0)
            throw new IllegalArgumentException("Valor inválido para parcelamento");

        BigDecimal total = round(value);
        BigDecimal parcela = total.divide(BigDecimal.valueOf(count), DEFAULT_SCALE, DEFAULT_ROUNDING);
        BigDecimal[] result = new BigDecimal[count];

        BigDecimal somaParcelas = parcela.multiply(BigDecimal.valueOf(count - 1));
        for (int i = 0; i < count - 1; i++)
            result[i] = parcela;
        result[count - 1] = total.subtract(somaParcelas).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);

        return result;
    }

    public static boolean isZero(Number value) {
        if (value == null)
            return true;
        return toBigDecimal(value).compareTo(BigDecimal.ZERO) == 0;
    }

    public static boolean isPositive(Number value) {
        if (value == null)
            return false;
        return toBigDecimal(value).compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isNegative(Number value) {
        if (value == null)
            return false;
        return toBigDecimal(value).compareTo(BigDecimal.ZERO) < 0;
    }

    public static boolean isGreaterThan(Number a, Number b) {
        if (a == null || b == null)
            return false;
        return toBigDecimal(a).compareTo(toBigDecimal(b)) > 0;
    }

    public static boolean isLessThan(Number a, Number b) {
        if (a == null || b == null)
            return false;
        return toBigDecimal(a).compareTo(toBigDecimal(b)) < 0;
    }

    public static boolean isBetween(Number value, Number min, Number max) {
        if (value == null || min == null || max == null)
            return false;
        BigDecimal v = toBigDecimal(value);
        return v.compareTo(toBigDecimal(min)) >= 0 && v.compareTo(toBigDecimal(max)) <= 0;
    }

    public static boolean equals(Number a, Number b) {
        if (a == null && b == null)
            return true;
        if (a == null || b == null)
            return false;
        return toBigDecimal(a).compareTo(toBigDecimal(b)) == 0;
    }

    public static String formatBRL(Number value) {
        if (value == null)
            return "R$ 0,00";
        NumberFormat fmt = NumberFormat.getCurrencyInstance(LOCALE_BR);
        return fmt.format(toBigDecimal(value));
    }

    public static String formatPercent(Number value) {
        if (value == null)
            return "0,00%";
        return String.format(LOCALE_BR, "%.2f%%", toBigDecimal(value));
    }

    private static BigDecimal toBigDecimal(Number number) {
        if (number instanceof BigDecimal bd)
            return bd;
        return new BigDecimal(number.toString());
    }

    private static void validate(Number... values) {
        if (values == null || values.length == 0)
            throw new IllegalArgumentException("Informe ao menos um valor");
    }
}
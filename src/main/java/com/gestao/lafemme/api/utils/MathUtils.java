package com.gestao.lafemme.api.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

public final class MathUtils {

    private static final int DEFAULT_SCALE = 2;

    private static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;

    private MathUtils() {
    }

    public static BigDecimal sum(Number... values) {
        validate(values);
        BigDecimal result = BigDecimal.ZERO;

        for (Number n : values) {
            if (n != null) {
                result = result.add(toBigDecimal(n));
            }
        }
        return result;
    }

    public static BigDecimal subtract(Number first, Number... values) {
        Objects.requireNonNull(first, "Primeiro valor não pode ser null");
        validate(values);

        BigDecimal result = toBigDecimal(first);

        for (Number n : values) {
            if (n != null) {
                result = result.subtract(toBigDecimal(n));
            }
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
            if (n == null) continue;

            BigDecimal divisor = toBigDecimal(n);
            if (BigDecimal.ZERO.compareTo(divisor) == 0) {
                throw new ArithmeticException("Divisão por zero");
            }

            result = result.divide(divisor, DEFAULT_SCALE, DEFAULT_ROUNDING);
        }
        return result;
    }

    public static BigDecimal round(Number value) {
        return round(value, DEFAULT_SCALE);
    }

    public static BigDecimal round(Number value, int scale) {
        if (value == null) return BigDecimal.ZERO;
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

        return count == 0
                ? BigDecimal.ZERO
                : sum.divide(BigDecimal.valueOf(count), DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal percentage(Number value, Number percent) {
        if (value == null || percent == null) return BigDecimal.ZERO;

        return toBigDecimal(value)
                .multiply(toBigDecimal(percent))
                .divide(BigDecimal.valueOf(100), DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal max(Number... values) {
        validate(values);

        BigDecimal max = null;

        for (Number n : values) {
            if (n == null) continue;
            BigDecimal bd = toBigDecimal(n);
            if (max == null || bd.compareTo(max) > 0) {
                max = bd;
            }
        }

        return max != null ? max : BigDecimal.ZERO;
    }

    public static BigDecimal min(Number... values) {
        validate(values);

        BigDecimal min = null;

        for (Number n : values) {
            if (n == null) continue;
            BigDecimal bd = toBigDecimal(n);
            if (min == null || bd.compareTo(min) < 0) {
                min = bd;
            }
        }

        return min != null ? min : BigDecimal.ZERO;
    }

    private static BigDecimal toBigDecimal(Number number) {
        if (number instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(number.toString());
    }

    private static void validate(Number... values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Informe ao menos um valor");
        }
    }
}

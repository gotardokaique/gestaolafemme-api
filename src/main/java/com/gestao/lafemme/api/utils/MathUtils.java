package com.gestao.lafemme.api.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Classe utilitária para operações matemáticas seguras.
 * Usa BigDecimal internamente para evitar erros de precisão.
 */
public final class MathUtils {

    /** Escala padrão para operações financeiras */
    private static final int DEFAULT_SCALE = 2;

    /** Modo de arredondamento padrão (padrão financeiro) */
    private static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;

    private MathUtils() {
        // Classe utilitária — não deve ser instanciada
    }

    /* =====================================================
       OPERAÇÕES BÁSICAS
       ===================================================== */

    /**
     * Soma múltiplos valores numéricos.
     * <p>
     * Aceita Integer, Long, Float, Double e BigDecimal.
     * Valores null são ignorados.
     *
     * Exemplo:
     * MathUtils.sum(10, 5.5, new BigDecimal("2.3"))
     */
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

    /**
     * Subtrai múltiplos valores a partir do primeiro.
     *
     * Exemplo:
     * MathUtils.subtract(100, 10, 20) // resultado: 70
     */
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

    /**
     * Multiplica múltiplos valores.
     * <p>
     * Se todos os valores forem null, retorna ZERO.
     *
     * Exemplo:
     * MathUtils.multiply(2, 4, 9) // resultado: 72
     */
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

    /**
     * Divide um valor por um ou mais divisores.
     * <p>
     * Lança exceção em caso de divisão por zero.
     *
     * Exemplo:
     * MathUtils.divide(100, 2, 5) // resultado: 10.00
     */
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

    /* =====================================================
       ARREDONDAMENTO
       ===================================================== */

    /**
     * Arredonda um valor usando escala padrão (2 casas).
     *
     * Exemplo:
     * MathUtils.round(10.567) // resultado: 10.57
     */
    public static BigDecimal round(Number value) {
        return round(value, DEFAULT_SCALE);
    }

    /**
     * Arredonda um valor definindo a quantidade de casas decimais.
     *
     * Exemplo:
     * MathUtils.round(10.567, 1) // resultado: 10.6
     */
    public static BigDecimal round(Number value, int scale) {
        if (value == null) return BigDecimal.ZERO;
        return toBigDecimal(value).setScale(scale, DEFAULT_ROUNDING);
    }

    /* =====================================================
       UTILITÁRIOS FINANCEIROS
       ===================================================== */

    /**
     * Calcula a média de vários valores.
     *
     * Exemplo:
     * MathUtils.average(10, 20, 30) // resultado: 20.00
     */
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

    /**
     * Calcula o percentual de um valor.
     *
     * Exemplo:
     * MathUtils.percentage(200, 15) // resultado: 30.00
     */
    public static BigDecimal percentage(Number value, Number percent) {
        if (value == null || percent == null) return BigDecimal.ZERO;

        return toBigDecimal(value)
                .multiply(toBigDecimal(percent))
                .divide(BigDecimal.valueOf(100), DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    /**
     * Retorna o maior valor informado.
     *
     * Exemplo:
     * MathUtils.max(10, 50, 30) // resultado: 50
     */
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

    /**
     * Retorna o menor valor informado.
     *
     * Exemplo:
     * MathUtils.min(10, 50, 30) // resultado: 10
     */
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

    /* =====================================================
       MÉTODOS INTERNOS
       ===================================================== */

    /**
     * Converte qualquer Number para BigDecimal de forma segura.
     */
    private static BigDecimal toBigDecimal(Number number) {
        if (number instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(number.toString());
    }

    /**
     * Valida se pelo menos um valor foi informado.
     */
    private static void validate(Number... values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Informe ao menos um valor");
        }
    }
}

package com.gestao.lafemme.api.utils;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Utilitários de manipulação, validação e formatação de strings para o contexto
 * brasileiro.
 * <p>
 * Inclui validação de CPF/CNPJ pelo algoritmo mod-11 da Receita Federal,
 * formatação de documentos, telefones, CEP e helpers gerais de limpeza e
 * normalização.
 * </p>
 *
 * @author Kaique Gotardo
 * @since 1.0
 */
public final class StringUtils {

    private StringUtils() {
    }

    private static final Pattern ONLY_DIGITS = Pattern.compile("\\D");
    private static final Pattern ONLY_LETTERS = Pattern.compile("[^a-zA-ZÀ-ÿ ]");
    private static final Pattern EXTRA_SPACES = Pattern.compile("\\s+");
    private static final Pattern CPF_FORMAT = Pattern.compile("(\\d{3})(\\d{3})(\\d{3})(\\d{2})");
    private static final Pattern CNPJ_FORMAT = Pattern.compile("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})");
    private static final Pattern PHONE_FORMAT_8 = Pattern.compile("(\\d{2})(\\d{4})(\\d{4})");
    private static final Pattern PHONE_FORMAT_9 = Pattern.compile("(\\d{2})(\\d{5})(\\d{4})");
    private static final Pattern CEP_FORMAT = Pattern.compile("(\\d{5})(\\d{3})");
    private static final Pattern SLUG_INVALID = Pattern.compile("[^a-z0-9-]");
    private static final Pattern SLUG_HYPHENS = Pattern.compile("-+");

    public static String onlyDigits(String value) {
        if (isBlank(value))
            return "";
        return ONLY_DIGITS.matcher(value).replaceAll("");
    }

    public static String onlyLetters(String value) {
        if (isBlank(value))
            return "";
        return ONLY_LETTERS.matcher(value).replaceAll("");
    }

    public static String trimSpaces(String value) {
        if (isBlank(value))
            return "";
        return EXTRA_SPACES.matcher(value.trim()).replaceAll(" ");
    }

    public static String removeAccents(String value) {
        if (isBlank(value))
            return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}", "");
    }

    public static String toSlug(String value) {
        if (isBlank(value))
            return "";
        String slug = removeAccents(value).toLowerCase().trim();
        slug = SLUG_INVALID.matcher(slug.replace(" ", "-")).replaceAll("");
        return SLUG_HYPHENS.matcher(slug).replaceAll("-");
    }

    public static String capitalize(String value) {
        if (isBlank(value))
            return "";
        return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
    }

    public static String capitalizeFull(String value) {
        if (isBlank(value))
            return "";
        String[] words = trimSpaces(value).split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isBlank())
                sb.append(capitalize(word)).append(" ");
        }
        return sb.toString().trim();
    }

    public static String truncate(String value, int maxLength) {
        if (isBlank(value))
            return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public static String truncateWithSuffix(String value, int maxLength, String suffix) {
        if (isBlank(value))
            return "";
        if (value.length() <= maxLength)
            return value;
        return value.substring(0, maxLength - suffix.length()) + suffix;
    }

    public static String mask(String value, int visibleStart, int visibleEnd, char maskChar) {
        if (isBlank(value))
            return "";
        if (value.length() <= visibleStart + visibleEnd)
            return value;
        return value.substring(0, visibleStart)
                + String.valueOf(maskChar).repeat(value.length() - visibleStart - visibleEnd)
                + value.substring(value.length() - visibleEnd);
    }

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }

    public static boolean hasMinLength(String value, int min) {
        return isNotBlank(value) && value.length() >= min;
    }

    public static boolean hasMaxLength(String value, int max) {
        return value == null || value.length() <= max;
    }

    public static boolean isNumeric(String value) {
        if (isBlank(value))
            return false;
        return value.chars().allMatch(Character::isDigit);
    }

    public static boolean isEmail(String value) {
        if (isBlank(value))
            return false;
        return value.matches("^[\\w+.%-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }

    public static boolean isUrl(String value) {
        if (isBlank(value))
            return false;
        return value.matches("^(https?|ftp)://[^\\s/$.?#].[^\\s]*$");
    }

    public static boolean isValidCpf(String cpf) {
        String digits = onlyDigits(cpf);
        if (digits.length() != 11)
            return false;
        if (digits.matches("(\\d)\\1{10}"))
            return false;

        int sum = 0;
        for (int i = 0; i < 9; i++)
            sum += Character.getNumericValue(digits.charAt(i)) * (10 - i);
        int first = 11 - (sum % 11);
        if (first >= 10)
            first = 0;

        sum = 0;
        for (int i = 0; i < 10; i++)
            sum += Character.getNumericValue(digits.charAt(i)) * (11 - i);
        int second = 11 - (sum % 11);
        if (second >= 10)
            second = 0;

        return first == Character.getNumericValue(digits.charAt(9))
                && second == Character.getNumericValue(digits.charAt(10));
    }

    public static String formatCpf(String cpf) {
        String digits = onlyDigits(cpf);
        if (digits.length() != 11)
            return cpf;
        return CPF_FORMAT.matcher(digits).replaceAll("$1.$2.$3-$4");
    }

    public static String maskCpf(String cpf) {
        String formatted = formatCpf(cpf);
        if (formatted.length() != 14)
            return formatted;
        return formatted.substring(0, 3) + ".***.***-" + formatted.substring(12);
    }

    public static boolean isValidCnpj(String cnpj) {
        String digits = onlyDigits(cnpj);
        if (digits.length() != 14)
            return false;
        if (digits.matches("(\\d)\\1{13}"))
            return false;

        int[] weights1 = { 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 };
        int sum = 0;
        for (int i = 0; i < 12; i++)
            sum += Character.getNumericValue(digits.charAt(i)) * weights1[i];
        int first = sum % 11 < 2 ? 0 : 11 - (sum % 11);

        int[] weights2 = { 6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 };
        sum = 0;
        for (int i = 0; i < 13; i++)
            sum += Character.getNumericValue(digits.charAt(i)) * weights2[i];
        int second = sum % 11 < 2 ? 0 : 11 - (sum % 11);

        return first == Character.getNumericValue(digits.charAt(12))
                && second == Character.getNumericValue(digits.charAt(13));
    }

    public static String formatCnpj(String cnpj) {
        String digits = onlyDigits(cnpj);
        if (digits.length() != 14)
            return cnpj;
        return CNPJ_FORMAT.matcher(digits).replaceAll("$1.$2.$3/$4-$5");
    }

    public static String maskCnpj(String cnpj) {
        String formatted = formatCnpj(cnpj);
        if (formatted.length() != 18)
            return formatted;
        return formatted.substring(0, 2) + ".***.***/****-" + formatted.substring(16);
    }

    public static boolean isValidDocument(String value) {
        if (isBlank(value))
            return false;
        String digits = onlyDigits(value);
        return switch (digits.length()) {
            case 11 -> isValidCpf(digits);
            case 14 -> isValidCnpj(digits);
            default -> false;
        };
    }

    public static String formatDocument(String value) {
        if (isBlank(value))
            return "";
        String digits = onlyDigits(value);
        return switch (digits.length()) {
            case 11 -> formatCpf(digits);
            case 14 -> formatCnpj(digits);
            default -> value;
        };
    }

    public static boolean isValidPhone(String phone) {
        if (isBlank(phone))
            return false;
        String digits = onlyDigits(phone);
        return digits.length() == 10 || digits.length() == 11;
    }

    public static String formatPhone(String phone) {
        if (isBlank(phone))
            return "";
        String digits = onlyDigits(phone);
        return switch (digits.length()) {
            case 10 -> PHONE_FORMAT_8.matcher(digits).replaceAll("($1) $2-$3");
            case 11 -> PHONE_FORMAT_9.matcher(digits).replaceAll("($1) $2-$3");
            default -> phone;
        };
    }

    public static boolean isValidCep(String cep) {
        if (isBlank(cep))
            return false;
        String digits = onlyDigits(cep);
        return digits.length() == 8 && !digits.matches("(\\d)\\1{7}");
    }

    public static String formatCep(String cep) {
        String digits = onlyDigits(cep);
        if (digits.length() != 8)
            return cep;
        return CEP_FORMAT.matcher(digits).replaceAll("$1-$2");
    }

    public static boolean equalsIgnoreCaseAndAccents(String a, String b) {
        if (a == null && b == null)
            return true;
        if (a == null || b == null)
            return false;
        return removeAccents(a).equalsIgnoreCase(removeAccents(b));
    }

    public static boolean containsIgnoreCaseAndAccents(String text, String search) {
        if (isBlank(text) || isBlank(search))
            return false;
        return removeAccents(text).toLowerCase().contains(removeAccents(search).toLowerCase());
    }

    public static String normalize(String value) {
        if (isBlank(value))
            return "";
        return value.trim().toLowerCase().replace(" ", "");
    }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
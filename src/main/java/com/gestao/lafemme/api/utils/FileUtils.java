package com.gestao.lafemme.api.utils;

import java.util.Base64;
import java.util.Map;
import java.util.Set;

public final class FileUtils {

    private FileUtils() {
    }

    public static final long MAX_SIZE_AVATAR = 6 * 1024 * 1024L; // 2 MB
    public static final long MAX_SIZE_DOCUMENT = 20 * 1024 * 1024L; // 10 MB
    public static final long MAX_SIZE_DEFAULT = 10 * 1024 * 1024L; // 5 MB

    public static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");

    public static final Set<String> ALLOWED_DOCUMENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private static final Map<String, String> EXTENSION_MIME_MAP = Map.of(
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp",
            "gif", "image/gif",
            "pdf", "application/pdf");

    public static String detectarMimeType(byte[] bytes) {
        if (bytes == null || bytes.length < 4)
            return "unknown";

        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF)
            return "image/jpeg";

        if (bytes.length >= 8
                && bytes[0] == (byte) 0x89 && bytes[1] == 0x50
                && bytes[2] == 0x4E && bytes[3] == 0x47)
            return "image/png";
        if (bytes.length >= 12
                && bytes[0] == 0x52 && bytes[1] == 0x49
                && bytes[2] == 0x46 && bytes[3] == 0x46
                && bytes[8] == 0x57 && bytes[9] == 0x45
                && bytes[10] == 0x42 && bytes[11] == 0x50)
            return "image/webp";

        if (bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46)
            return "image/gif";

        if (bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46)
            return "application/pdf";

        if (bytes[0] == 0x50 && bytes[1] == 0x4B)
            return "application/zip";

        return "unknown";
    }

    public static String getExtension(String filename) {
        if (filename == null || filename.isBlank())
            return "";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1)
            return "";
        return filename.substring(lastDot + 1).toLowerCase();
    }

    public static String getNameWithoutExtension(String filename) {
        if (filename == null || filename.isBlank())
            return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot < 0 ? filename : filename.substring(0, lastDot);
    }

    public static long getFileSizeBytes(byte[] bytes) {
        return bytes != null ? bytes.length : 0;
    }

    public static long getFileSizeFromBase64(String base64) {
        if (base64 == null || base64.isBlank())
            return 0;
        try {
            return Base64.getDecoder().decode(stripBase64Header(base64)).length;
        } catch (Exception e) {
            return 0;
        }
    }

    public static String formatFileSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    public static void validarArquivo(byte[] bytes, String filename, Set<String> tiposPermitidos, long maxBytes) {
        if (bytes == null || bytes.length == 0)
            throw new IllegalArgumentException("Arquivo vazio.");

        if (bytes.length > maxBytes)
            throw new IllegalArgumentException(
                    "Arquivo muito grande. Máximo: " + formatFileSize(maxBytes) +
                            ". Recebido: " + formatFileSize(bytes.length));

        String mimeDetectado = detectarMimeType(bytes);
        if ("unknown".equals(mimeDetectado))
            throw new IllegalArgumentException("Tipo de arquivo não reconhecido.");

        if (!tiposPermitidos.contains(mimeDetectado))
            throw new IllegalArgumentException(
                    "Tipo não permitido: " + mimeDetectado +
                            ". Permitidos: " + tiposPermitidos);

        String ext = getExtension(filename);
        if (!ext.isBlank()) {
            String mimeEsperadoPorExtensao = EXTENSION_MIME_MAP.get(ext);
            if (mimeEsperadoPorExtensao != null) {
                boolean zipCompativel = "application/zip".equals(mimeDetectado)
                        && (ext.equals("docx") || ext.equals("xlsx"));
                if (!zipCompativel && !mimeEsperadoPorExtensao.equals(mimeDetectado))
                    throw new IllegalArgumentException(
                            "Extensão '" + ext + "' incompatível com o conteúdo real do arquivo.");
            }
        }
    }

    public static void validarImagem(byte[] bytes, String filename) {
        validarArquivo(bytes, filename, ALLOWED_IMAGE_TYPES, MAX_SIZE_AVATAR);
    }

    public static void validarDocumento(byte[] bytes, String filename) {
        validarArquivo(bytes, filename, ALLOWED_DOCUMENT_TYPES, MAX_SIZE_DOCUMENT);
    }

    public static String sanitizarNomeArquivo(String filename) {
        if (filename == null || filename.isBlank())
            return "arquivo";

        String name = filename
                .replaceAll("[/\\\\:*?\"<>|]", "_")
                .replaceAll("\\.{2,}", ".")
                .replaceAll("^[.\\s]+", "")
                .trim();

        if (name.length() > 100) {
            String ext = getExtension(name);
            String base = getNameWithoutExtension(name).substring(0, 90);
            name = ext.isBlank() ? base : base + "." + ext;
        }

        return name.isBlank() ? "arquivo" : name;
    }

    public static String stripBase64Header(String base64) {
        if (base64 == null)
            return "";
        int comma = base64.indexOf(',');
        return comma >= 0 ? base64.substring(comma + 1) : base64;
    }

    public static byte[] decodeBase64ToBytes(String base64) {
        try {
            return Base64.getDecoder().decode(stripBase64Header(base64));
        } catch (Exception e) {
            throw new IllegalArgumentException("Base64 inválido.");
        }
    }

    public static String encodeBase64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    /** use decodeBase64ToBytes */
    @Deprecated
    public static String decodeBase64(String base64) {
        return new String(Base64.getDecoder().decode(base64));
    }

    /** use getFileSizeFromBase64 */
    @Deprecated
    public static long getFileSize(String base64) {
        return getFileSizeFromBase64(base64);
    }
}
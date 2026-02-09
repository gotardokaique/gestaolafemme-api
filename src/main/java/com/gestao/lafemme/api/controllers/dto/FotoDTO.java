package com.gestao.lafemme.api.controllers.dto;

/**
 * DTO para transferência de dados de foto/anexo em base64.
 * 
 * @param nome      Nome do arquivo (ex: "produto.jpg")
 * @param mimeType  Tipo MIME do arquivo (ex: "image/jpeg")
 * @param arquivo   Conteúdo do arquivo em Base64
 */
public record FotoDTO(
        String nome,
        String mimeType,
        String arquivo
) {}

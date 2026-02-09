package com.gestao.lafemme.api.controllers.dto;

/**
 * DTO para adicionar uma foto ao catálogo do produto.
 * 
 * @param nome        Nome/descrição da foto (ex: "COR: VERMELHA")
 * @param mimeType    Tipo MIME do arquivo (ex: "image/jpeg")
 * @param arquivo     Conteúdo do arquivo em Base64
 */
public record FotoCatalogoRequestDTO(
    String nome,
    String mimeType,
    String arquivo
) {}

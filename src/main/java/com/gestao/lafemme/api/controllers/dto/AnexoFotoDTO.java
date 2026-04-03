package com.gestao.lafemme.api.controllers.dto;

/**
 * DTO simples para transferir o binário de uma foto entre serviço e controller.
 * Não expõe Base64 — os bytes são enviados diretamente na resposta HTTP.
 */
public record AnexoFotoDTO(byte[] arquivo, String mimeType) {}

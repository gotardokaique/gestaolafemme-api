package com.gestao.lafemme.api.controllers.dto;

public record GerarPagamentoResponse(
    String tipo,           // "CHECKOUT" ou "PIX"
    String paymentLink,    // só para CHECKOUT
    String preferenceId,   // só para CHECKOUT
    String qrCode,         // só para PIX
    String qrCodeBase64    // só para PIX
) {}

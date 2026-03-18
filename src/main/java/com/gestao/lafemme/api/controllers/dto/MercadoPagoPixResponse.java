package com.gestao.lafemme.api.controllers.dto;

public record MercadoPagoPixResponse(
    String qrCode,
    String qrCodeBase64,
    String externalReference
) {}

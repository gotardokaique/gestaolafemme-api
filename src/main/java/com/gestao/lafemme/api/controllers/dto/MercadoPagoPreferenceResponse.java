package com.gestao.lafemme.api.controllers.dto;

public record MercadoPagoPreferenceResponse(
        String paymentLink,
        String preferenceId
) {
}

package com.gestao.lafemme.api.controllers.dto;

import com.gestao.lafemme.api.enuns.TipoPagamentoMP;

public record ConfiguracaoMP(
        String accessToken,
        String webhookSecret,
        TipoPagamentoMP tipoPagamento
) {
}

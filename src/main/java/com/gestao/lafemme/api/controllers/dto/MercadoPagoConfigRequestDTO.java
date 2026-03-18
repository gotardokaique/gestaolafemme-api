package com.gestao.lafemme.api.controllers.dto;

import com.gestao.lafemme.api.enuns.TipoPagamentoMP;

public record MercadoPagoConfigRequestDTO(
    TipoPagamentoMP tipoPagamento
) {}

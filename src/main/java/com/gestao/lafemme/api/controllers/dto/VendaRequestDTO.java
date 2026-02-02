package com.gestao.lafemme.api.controllers.dto;

import java.math.BigDecimal;

public record VendaRequestDTO(
        BigDecimal valorTotal,
        String formaPagamento,
        String observacao
        ) {

}

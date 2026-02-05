package com.gestao.lafemme.api.controllers.dto;

import java.math.BigDecimal;
import java.util.Date;

public record VendaRequestDTO(
        Long produtoId,
        Integer quantidade,
        BigDecimal valorTotal,
        String formaPagamento,
        String observacao,
        Date dataVenda
) {}

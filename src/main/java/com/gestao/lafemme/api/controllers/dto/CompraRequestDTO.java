package com.gestao.lafemme.api.controllers.dto;

import java.math.BigDecimal;

public record CompraRequestDTO(
        Long fornecedorId,
        BigDecimal valorTotal,
        String formaPagamento,
        Integer quantidade,
        Integer[] produtoIds,
        String observacao
) {
}

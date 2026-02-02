package com.gestao.lafemme.api.controllers.dto;

import java.math.BigDecimal;
import java.util.Date;

import com.gestao.lafemme.api.entity.Venda;

public record VendaResponseDTO(
        Long id,
        Date dataVenda,
        BigDecimal valorTotal,
        String formaPagamento
) {
    public static VendaResponseDTO from(Venda v) {
        return new VendaResponseDTO(
                v.getId(),
                v.getDataVenda(),
                v.getValorTotal(),
                v.getFormaPagamento()
        );
    }
}

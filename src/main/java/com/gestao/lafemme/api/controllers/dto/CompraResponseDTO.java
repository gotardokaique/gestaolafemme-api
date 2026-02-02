package com.gestao.lafemme.api.controllers.dto;

import java.math.BigDecimal;
import java.util.Date;

import com.gestao.lafemme.api.entity.Compra;
import com.gestao.lafemme.api.entity.Fornecedor;

public record CompraResponseDTO(
        Long id,
        Date dataCompra,
        BigDecimal valorTotal,
        String formaPagamento,
        Long fornecedorId,
        String fornecedorNome
) {
    public static CompraResponseDTO from(Compra c) {
        Fornecedor f = c.getFornecedor();
        return new CompraResponseDTO(
                c.getId(),
                c.getDataCompra(),
                c.getValorTotal(),
                c.getFormaPagamento(),
                f != null ? f.getId() : null,
                f != null ? f.getNome() : null
        );
    }
}

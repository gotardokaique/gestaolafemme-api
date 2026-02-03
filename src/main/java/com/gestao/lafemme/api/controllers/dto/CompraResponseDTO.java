package com.gestao.lafemme.api.controllers.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    public static CompraResponseDTO refactor(Compra com) {
        return new CompraResponseDTO(
                com.getId(),
                com.getDataCompra(),
                com.getValorTotal(),
                com.getFormaPagamento(),
                com.getFornecedor().getId(),
                com.getFornecedor().getNome()
        );
    }

    public static List<CompraResponseDTO> refactor(List<Compra> listCom) {
        return listCom.stream()
                .filter(Objects::nonNull)
                .map(CompraResponseDTO::refactor)
                .collect(Collectors.toList());
    }
}

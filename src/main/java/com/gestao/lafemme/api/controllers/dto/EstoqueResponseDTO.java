package com.gestao.lafemme.api.controllers.dto;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.gestao.lafemme.api.entity.Estoque;

public record EstoqueResponseDTO(
        Long productId,
        String produtoNome,
        String produtoCodigo,
        String categoriaNome,
        Integer quantidadeAtual,
        Integer estoqueMinimo
) {
    public static EstoqueResponseDTO refactor(Estoque estq) {
        return new EstoqueResponseDTO(
                estq.getProduto().getId(),
                estq.getProduto().getNome(),
                estq.getProduto().getCodigo(),
                estq.getProduto().getCategoriaProduto().getNome(),
                estq.getQuantidadeAtual(),
                estq.getEstoqueMinimo()
        );
    }

    public static List<EstoqueResponseDTO> refactor(List<Estoque> listEstq) {
        return listEstq.stream()
                .filter(Objects::nonNull)
                .map(EstoqueResponseDTO::refactor)
                .collect(Collectors.toList());
    }
}

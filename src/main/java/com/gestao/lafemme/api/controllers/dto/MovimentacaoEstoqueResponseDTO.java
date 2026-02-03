package com.gestao.lafemme.api.controllers.dto;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.gestao.lafemme.api.entity.Estoque;
import com.gestao.lafemme.api.entity.MovimentacaoEstoque;
import com.gestao.lafemme.api.entity.Produto;

public record MovimentacaoEstoqueResponseDTO(
        Long id,
        Date dataMovimentacao,
        String tipoMovimentacao,
        Integer quantidade,
        String observacao,
        Long estoqueId,
        Long produtoId,
        String produtoNome
) {
    public static MovimentacaoEstoqueResponseDTO refactor(MovimentacaoEstoque mov) {
        return new MovimentacaoEstoqueResponseDTO(
                mov.getId(),
                mov.getDataMovimentacao(),
                mov.getTipoMovimentacao().name(),
                mov.getQuantidade(),
                mov.getObservacao(),
                mov.getEstoque().getId(),
                mov.getEstoque().getProduto().getId(),
                mov.getEstoque().getProduto().getNome()
        );
    }

    public static List<MovimentacaoEstoqueResponseDTO> refactor(List<MovimentacaoEstoque> listMov) {
        return listMov.stream()
                .filter(Objects::nonNull)
                .map(MovimentacaoEstoqueResponseDTO::refactor)
                .collect(Collectors.toList());
    }
}

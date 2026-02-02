package com.gestao.lafemme.api.controllers.dto;

import java.util.Date;

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
    public static MovimentacaoEstoqueResponseDTO from(MovimentacaoEstoque mov) {
        Estoque estoque = mov.getEstoque();
        Produto produto = estoque != null ? estoque.getProduto() : null;
        return new MovimentacaoEstoqueResponseDTO(
                mov.getId(),
                mov.getDataMovimentacao(),
                mov.getTipoMovimentacao() != null ? mov.getTipoMovimentacao().name() : null,
                mov.getQuantidade(),
                mov.getObservacao(),
                estoque != null ? estoque.getId() : null,
                produto != null ? produto.getId() : null,
                produto != null ? produto.getNome() : null
        );
    }
}

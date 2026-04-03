package com.gestao.lafemme.api.controllers.dto;

import java.math.BigDecimal;

import com.gestao.lafemme.api.entity.Produto;

public record PublicProdutoDTO(
        Long id,
        String nome,
        String descricao,
        BigDecimal preco,
        String categoria,
        int quantidadeEstoque,
        boolean hasFoto
) {
    public static PublicProdutoDTO fromEntity(Produto p, boolean hasFoto) {
        return new PublicProdutoDTO(
                p.getId(),
                p.getNome(),
                p.getDescricao(),
                p.getValorVenda(),
                p.getCategoriaProduto() != null ? p.getCategoriaProduto().getNome() : null,
                p.getEstoque() != null ? p.getEstoque().getQuantidadeAtual() : 0,
                hasFoto
        );
    }
}

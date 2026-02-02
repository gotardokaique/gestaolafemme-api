package com.gestao.lafemme.api.controllers.dto;

import java.math.BigDecimal;

import com.gestao.lafemme.api.entity.CategoriaProduto;
import com.gestao.lafemme.api.entity.Estoque;
import com.gestao.lafemme.api.entity.Produto;

public record ProdutoResponseDTO(
        Long id,
        String nome,
        String codigo,
        String descricao,
        BigDecimal valorCusto,
        BigDecimal valorVenda,
        Boolean ativo,
        Long categoriaId,
        String categoriaNome,
        Integer estoqueMinimo,
        Integer quantidadeAtual
) {
    public static ProdutoResponseDTO from(Produto p) {
        Estoque estoque = p.getEstoque();
        CategoriaProduto cat = p.getCategoriaProduto();
        return new ProdutoResponseDTO(
                p.getId(),
                p.getNome(),
                p.getCodigo(),
                p.getDescricao(),
                p.getValorCusto(),
                p.getValorVenda(),
                p.isAtivo(),
                cat != null ? cat.getId() : null,
                cat != null ? cat.getNome() : null,
                estoque != null ? estoque.getEstoqueMinimo() : 0,
                estoque != null ? estoque.getQuantidadeAtual() : 0
        );
    }
}

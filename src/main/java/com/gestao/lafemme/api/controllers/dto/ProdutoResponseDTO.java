package com.gestao.lafemme.api.controllers.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    public static ProdutoResponseDTO refactor(Produto prod) {
        return new ProdutoResponseDTO(
                prod.getId(),
                prod.getNome(),
                prod.getCodigo(),
                prod.getDescricao(),
                prod.getValorCusto(),
                prod.getValorVenda(),
                prod.isAtivo(),
                prod.getCategoriaProduto().getId(),
                prod.getCategoriaProduto().getNome(),
                prod.getEstoque().getEstoqueMinimo(),
                prod.getEstoque().getQuantidadeAtual()
        );
    }
    
    public static List<ProdutoResponseDTO> refactor(List<Produto> listProd) {
        return listProd.stream()
                .filter(Objects::nonNull)
                .map(ProdutoResponseDTO::refactor)
                .collect(Collectors.toList());
    }
}

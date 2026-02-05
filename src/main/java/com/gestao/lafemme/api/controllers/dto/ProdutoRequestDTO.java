package com.gestao.lafemme.api.controllers.dto;

import java.math.BigDecimal;

public record ProdutoRequestDTO(
        String nome,
        String codigo,
        String descricao,
        Long categoriaId,
        Integer estoqueMinimo,
        Boolean ativo,
        Integer quantidadeInicial,
        BigDecimal valorCusto,
        BigDecimal valorVenda
) {}

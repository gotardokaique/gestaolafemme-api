package com.gestao.lafemme.api.controllers.dto;

public record ProdutoRequestDTO(
        String nome,
        String codigo,
        String descricao,
        Long categoriaId,
        Integer estoqueMinimo,
        Boolean ativo
) {}

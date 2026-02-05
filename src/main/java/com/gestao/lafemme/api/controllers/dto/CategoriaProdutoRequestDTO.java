package com.gestao.lafemme.api.controllers.dto;

public record CategoriaProdutoRequestDTO(
        String nome,
        String descricao,
        Boolean ativo
) {}

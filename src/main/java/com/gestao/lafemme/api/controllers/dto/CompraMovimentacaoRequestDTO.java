package com.gestao.lafemme.api.controllers.dto;

public record CompraMovimentacaoRequestDTO(
        Long produtoId,
        int quantidade,
        String observacao
) {}

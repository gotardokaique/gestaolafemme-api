package com.gestao.lafemme.api.controllers.dto;

public record VendaMovimentacaoRequestDTO(
        Long produtoId,
        int quantidade,
        String observacao
) {}

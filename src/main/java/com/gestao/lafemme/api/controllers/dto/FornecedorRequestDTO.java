package com.gestao.lafemme.api.controllers.dto;

public record FornecedorRequestDTO(
        String nome,
        String telefone,
        String email,
        Boolean ativo
) {}

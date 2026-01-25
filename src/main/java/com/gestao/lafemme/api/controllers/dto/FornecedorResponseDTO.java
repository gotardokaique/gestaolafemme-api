package com.gestao.lafemme.api.controllers.dto;

import com.gestao.lafemme.api.entity.Fornecedor;

public record FornecedorResponseDTO(
        Long id,
        String nome,
        String telefone,
        String email,
        Boolean ativo
) {
    public static FornecedorResponseDTO from(Fornecedor f) {
        return new FornecedorResponseDTO(
                f.getId(),
                f.getNome(),
                f.getTelefone(),
                f.getEmail(),
                f.isAtivo()
        );
    }
}

package com.gestao.lafemme.api.controllers.dto;

import com.gestao.lafemme.api.entity.CategoriaProduto;

public record PublicCategoriaDTO(
        Long id,
        String nome
) {
    public static PublicCategoriaDTO fromEntity(CategoriaProduto c) {
        return new PublicCategoriaDTO(
                c.getId(),
                c.getNome()
        );
    }
}

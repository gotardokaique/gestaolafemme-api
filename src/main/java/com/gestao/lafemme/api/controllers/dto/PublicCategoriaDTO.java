package com.gestao.lafemme.api.controllers.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.gestao.lafemme.api.entity.CategoriaProduto;

public record PublicCategoriaDTO(
        Long id,
        String nome) {
    public static PublicCategoriaDTO fromEntity(CategoriaProduto c) {
        return new PublicCategoriaDTO(
                c.getId(),
                c.getNome());
    }

    public static List<PublicCategoriaDTO> fromEntity(List<CategoriaProduto> categorias) {
        return categorias.stream()
                .map(PublicCategoriaDTO::fromEntity)
                .collect(Collectors.toList());
    }
}

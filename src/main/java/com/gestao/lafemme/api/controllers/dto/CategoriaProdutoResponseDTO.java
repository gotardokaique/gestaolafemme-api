package com.gestao.lafemme.api.controllers.dto;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.gestao.lafemme.api.entity.CategoriaProduto;

public record CategoriaProdutoResponseDTO(
        Long id,
        String nome,
        String descricao,
        Boolean ativo
) {
    public static CategoriaProdutoResponseDTO refactor(CategoriaProduto cat) {
        return new CategoriaProdutoResponseDTO(
                cat.getId(),
                cat.getNome(),
                cat.getDescricao(),
                cat.isAtivo()
        );
    }

    public static List<CategoriaProdutoResponseDTO> refactor(List<CategoriaProduto> listCat) {
        return listCat.stream()
                .filter(Objects::nonNull)
                .map(CategoriaProdutoResponseDTO::refactor)
                .collect(Collectors.toList());
    }
}

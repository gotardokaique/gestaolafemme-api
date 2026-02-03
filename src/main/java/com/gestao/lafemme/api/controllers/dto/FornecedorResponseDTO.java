package com.gestao.lafemme.api.controllers.dto;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.gestao.lafemme.api.entity.Fornecedor;

public record FornecedorResponseDTO(
        Long id,
        String nome,
        String telefone,
        String email,
        Boolean ativo
) {
    public static FornecedorResponseDTO refactor(Fornecedor forn) {
        return new FornecedorResponseDTO(
                forn.getId(),
                forn.getNome(),
                forn.getTelefone(),
                forn.getEmail(),
                forn.isAtivo()
        );
    }

    public static List<FornecedorResponseDTO> refactor(List<Fornecedor> listForn) {
        return listForn.stream()
                .filter(Objects::nonNull)
                .map(FornecedorResponseDTO::refactor)
                .collect(Collectors.toList());
    }
}

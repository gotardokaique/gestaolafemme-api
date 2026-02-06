package com.gestao.lafemme.api.controllers.dto;

import java.util.Date;

public record UsuarioUnidadeDTO(
        Long id,
        String nome,
        String email,
        Date dataCriacao,
        String perfilNome,
        String perfilDescricao,
        boolean ativo
) {}

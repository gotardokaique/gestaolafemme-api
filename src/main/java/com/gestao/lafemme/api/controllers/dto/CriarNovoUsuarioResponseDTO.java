package com.gestao.lafemme.api.controllers.dto;

public record CriarNovoUsuarioResponseDTO(
        String email,
        String senhaTemporaria,
        String mensagem
) {}

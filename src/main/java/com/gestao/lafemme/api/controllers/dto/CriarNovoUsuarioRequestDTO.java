package com.gestao.lafemme.api.controllers.dto;

public record CriarNovoUsuarioRequestDTO(
        String nome,
        String email
) {}

package com.gestao.lafemme.api.security.controller.DTOs;

public record RegistroUsuarioRequestDTO(
        String nome,
        String email,
        String senha
) {}

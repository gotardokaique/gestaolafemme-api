package com.gestao.lafemme.api.controllers.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarNovoUsuarioRequestDTO(
        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 2, max = 40, message = "Nome deve ter entre 2 e 40 caracteres")
        String nome,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Formato de email inválido")
        @Size(max = 40, message = "Email deve ter no máximo 40 caracteres")
        String email
) {}

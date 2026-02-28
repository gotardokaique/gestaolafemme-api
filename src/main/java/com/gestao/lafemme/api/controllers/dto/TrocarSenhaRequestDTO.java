package com.gestao.lafemme.api.controllers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrocarSenhaRequestDTO(
        @NotBlank(message = "Senha atual é obrigatória")
        String senhaAtual,
        
        @NotBlank(message = "Nova senha é obrigatória")
        @Size(min = 8, max = 120, message = "Nova senha deve ter entre 8 e 120 caracteres")
        String senhaNova,
        
        @NotBlank(message = "Confirmação de nova senha é obrigatória")
        String senhaNovaConfirmacao
) {}

package com.gestao.lafemme.api.controllers.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

public record CriarUnidadeRequestDTO(
        @NotBlank(message = "O nome da unidade é obrigatório") String nomeUnidade,
        @NotBlank(message = "O email do responsável é obrigatório") @Email(message = "O email informado é inválido") String email,
        @NotBlank(message = "O nome do usuário é obrigatório") String nomeUsuario) implements Serializable {
}

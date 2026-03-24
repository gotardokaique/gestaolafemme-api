package com.gestao.lafemme.api.controllers.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

public record CriarUnidadeRequestDTO(
    @NotBlank(message = "O nome da unidade é obrigatório") String nome,
    @NotBlank(message = "O email do responsável é obrigatório") 
    @Email(message = "O email informado é inválido") String email,
    @NotNull(message = "O plano da unidade é obrigatório") String plano
) implements Serializable {}

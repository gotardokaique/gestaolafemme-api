package com.gestao.lafemme.api.controllers.dto;

public record TrocarSenhaRequestDTO(
        String senhaAtual,
        String senhaNova,
        String senhaNovaConfirmacao
) {}

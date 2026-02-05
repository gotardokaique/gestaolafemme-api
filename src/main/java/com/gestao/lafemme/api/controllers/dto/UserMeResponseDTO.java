package com.gestao.lafemme.api.controllers.dto;

import com.gestao.lafemme.api.entity.Usuario;

public record UserMeResponseDTO(
        Long id,
        String nome,
        String email,
        Long unidadeId,
        String unidadeNome,
        Long perfilId,
        String perfilNome,
        String perfilDescricao
) {
    public static UserMeResponseDTO from(Usuario usuario) {
        return new UserMeResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getUnidadeAtiva() != null ? usuario.getUnidadeAtiva().getId() : null,
                usuario.getUnidadeAtiva() != null ? usuario.getUnidadeAtiva().getNome() : null,
                usuario.getPerfilUsuario() != null ? usuario.getPerfilUsuario().getId() : null,
                usuario.getPerfilUsuario() != null ? usuario.getPerfilUsuario().getNome() : null,
                usuario.getPerfilUsuario() != null ? usuario.getPerfilUsuario().getDescricao() : null
        );
    }
}

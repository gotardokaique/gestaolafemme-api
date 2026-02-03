package com.gestao.lafemme.api.context;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.gestao.lafemme.api.entity.Unidade;
import com.gestao.lafemme.api.entity.Usuario;

public class UserContext {

    // ===================== USUÁRIO =====================

    public static Usuario getUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Nenhum usuário autenticado.");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Usuario usuario) {
            return usuario;
        }

        throw new IllegalStateException("Usuário autenticado inválido.");
    }

    public static boolean isUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated();
    }

    public static Long getIdUsuario() {
        return getUsuarioAutenticado().getId();
    }

    public static String getEmailUsuario() {
        return getUsuarioAutenticado().getEmail();
    }

    // ===================== UNIDADE =====================

    public static Unidade getUnidade() {
        Unidade unidade = getUsuarioAutenticado().getUnidadeAtiva();
        if (unidade == null) {
            throw new IllegalStateException("Usuário não possui unidade ativa.");
        }
        return unidade;
    }

    public static Long getIdUnidade() {
        return getUnidade().getId();
    }

    public static String getNomeUnidade() {
        return getUnidade().getNome();
    }

    public static boolean isUnidadeAtiva() {
        return getUnidade().isAtivo();
    }

    // ===================== HELPERS DE CONTEXTO =====================

    public static boolean isUsuarioDaUnidade(Long unidadeId) {
        return unidadeId != null && unidadeId.equals(getIdUnidade());
    }

    public static void validarUnidade(Long unidadeId) {
        if (!isUsuarioDaUnidade(unidadeId)) {
            throw new IllegalStateException("Usuário não pertence à unidade informada.");
        }
    }
}

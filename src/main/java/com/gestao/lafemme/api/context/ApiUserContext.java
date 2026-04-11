package com.gestao.lafemme.api.context;

import com.gen.core.context.UserContext;
import com.gestao.lafemme.api.entity.Unidade;
import com.gestao.lafemme.api.entity.Usuario;

/**
 * App-specific user context helpers that cast the generic UserAccount
 * to the concrete Usuario entity, and provide Unidade access.
 */
public class ApiUserContext {

    private ApiUserContext() {}

    /**
     * Returns the authenticated user cast to Usuario.
     */
    public static Usuario getUsuario() {
        return (Usuario) UserContext.getUsuarioAutenticado();
    }

    /**
     * Returns the active Unidade of the authenticated user.
     */
    public static Unidade getUnidade() {
        Usuario usuario = getUsuario();
        Unidade unidade = usuario.getUnidadeAtiva();
        if (unidade == null) {
            throw new IllegalStateException("Usuário não possui unidade ativa.");
        }
        return unidade;
    }
}

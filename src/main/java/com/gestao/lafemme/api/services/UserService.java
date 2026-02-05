package com.gestao.lafemme.api.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestao.lafemme.api.context.UserContext;
import com.gestao.lafemme.api.controllers.dto.UserMeResponseDTO;
import com.gestao.lafemme.api.db.DAOController;
import com.gestao.lafemme.api.entity.Unidade;
import com.gestao.lafemme.api.entity.Usuario;

@Service
public class UserService {

    private final DAOController dao;

    public UserService(DAOController dao) {
        this.dao = dao;
    }

    @Transactional(readOnly = true)
    public UserMeResponseDTO getMe() {
        try {
            Usuario usuario = dao.select()
                    .from(Usuario.class)
                    .join("perfilUsuario")
                    .id(UserContext.getIdUsuario());

            // Carrega unidade explicitamente na transação atual para evitar LazyInitException com objeto do contexto
            Unidade unidade = dao.select()
                    .from(Unidade.class)
                    .id(UserContext.getIdUnidade());

            usuario.setUnidadeAtiva(unidade);

            return UserMeResponseDTO.from(usuario);
        } catch (Exception e) {
            // Fallback se falhar ao recarregar, tenta usar o do contexto direto
            return UserMeResponseDTO.from(UserContext.getUsuario());
        }
    }
}

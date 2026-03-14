package com.gestao.lafemme.api.services;

import com.gestao.lafemme.api.db.Condicao;
import com.gestao.lafemme.api.db.DAOController;
import com.gestao.lafemme.api.entity.Configuracao;
import com.gestao.lafemme.api.services.exceptions.NotFoundException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ConfiguracaoRepository {

    private final DAOController dao;

    public ConfiguracaoRepository(DAOController dao) {
        this.dao = dao;
    }

    public Optional<Configuracao> findByUsuarioId(Integer usuarioId) throws Exception {
        try {
            Configuracao config = dao.select()
                    .from(Configuracao.class)
                    .where("usuario.id", Condicao.EQUAL, usuarioId)
                    .one();
            return Optional.of(config);
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }
}
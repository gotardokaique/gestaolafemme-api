package com.gestao.lafemme.api.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestao.lafemme.api.context.UserContext;
import com.gestao.lafemme.api.db.Condicao;
import com.gestao.lafemme.api.db.DAOController;
import com.gestao.lafemme.api.db.TransactionDB;
import com.gestao.lafemme.api.entity.Configuracao;
import com.gestao.lafemme.api.entity.Usuario;
import com.gestao.lafemme.api.services.exceptions.NotFoundException;
import com.gestao.lafemme.api.utils.StringEncryptUtils;

import jakarta.persistence.EntityNotFoundException;

@Service
public class ConfiguracaoService {

    private final DAOController dao;
    private final TransactionDB trans;
    private final StringEncryptUtils encryptUtils;

    public ConfiguracaoService(DAOController dao, TransactionDB trans, StringEncryptUtils encryptUtils) {
        this.dao = dao;
        this.trans = trans;
        this.encryptUtils = encryptUtils;
    }

    @Transactional
    public String gerarToken() throws Exception {
        Long userId = UserContext.getIdUsuario();
        String token = encryptUtils.encrypt(64, String.valueOf(userId));
        Configuracao config;
        try {
            config = dao.select()
                    .from(Configuracao.class)
                    .where("usuario.id", Condicao.EQUAL, userId)
                    .one();

            config.setApiToken(token);
            config.setAtivo(true);
            config.setUpdatedAt(new Date());

            trans.update(config);

        } catch (EntityNotFoundException  e) {
            config = new Configuracao();
            Usuario usuario = trans.selectById(Usuario.class, userId);
            if (usuario == null) {
                throw new IllegalStateException("Usuário não encontrado");
            }
            config.setUsuario(usuario);
            config.setApiToken(token);
            config.setAtivo(true);
            trans.insert(config);
        }

        return token;
    }

    @Transactional(readOnly = true)
    public String buscarTokenAtivo(Long userId) {
        List<Configuracao> configs = dao.select()
                .from(Configuracao.class)
                .where("usuario.id", Condicao.EQUAL, userId)
                .list();

        Configuracao config = configs.isEmpty() ? null : configs.get(0);
        return (config != null && config.isAtivo()) ? config.getApiToken() : null;
    }

    @Transactional(readOnly = true)
    public Configuracao buscarPorToken(String token) {
        List<Configuracao> configs = dao.select()
                .from(Configuracao.class)
                .where("apiToken", Condicao.EQUAL, token)
                .list();

        Configuracao config = configs.isEmpty() ? null : configs.get(0);

        if (config == null || !config.isAtivo()) {
            return null;
        }

        try {
            String decrypted = encryptUtils.decrypt(token);
            if (!decrypted.equals(String.valueOf(config.getUsuario().getId()))) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }

        return config;
    }

    @Transactional
    public void revogarToken() throws Exception {
        Long userId = UserContext.getIdUsuario();

        try {
            Configuracao config = dao.select()
                    .from(Configuracao.class)
                    .where("usuario.id", Condicao.EQUAL, userId)
                    .one();

            config.setAtivo(false);
            config.setUpdatedAt(new Date());
            trans.update(config);
        } catch (EntityNotFoundException  e) {
            return;
        }
    }
}

package com.gestao.lafemme.api.services;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gen.core.context.UserContext;
import com.gestao.lafemme.api.context.ApiUserContext;
import com.gestao.lafemme.api.controllers.dto.ConfiguracaoMP;
import com.gestao.lafemme.api.controllers.dto.EmailConfigRequestDTO;
import com.gestao.lafemme.api.controllers.dto.EmailConfigResponseDTO;
import com.gestao.lafemme.api.controllers.dto.MercadoPagoConfigResponseDTO;
import com.gen.core.db.Condicao;
import com.gen.core.db.DAOController;
import com.gen.core.db.TransactionDB;
import com.gestao.lafemme.api.entity.Configuracao;
import com.gestao.lafemme.api.entity.Unidade;
import com.gestao.lafemme.api.entity.Usuario;
import com.gestao.lafemme.api.services.exceptions.BusinessException;
import com.gestao.lafemme.api.services.exceptions.NotFoundException;
import com.gen.core.utils.StringEncryptUtils;

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

        } catch (EntityNotFoundException e) {
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
        } catch (EntityNotFoundException e) {
            return;
        }
    }

    @Transactional
    public EmailConfigResponseDTO salvarEmailConfig(EmailConfigRequestDTO request) throws Exception {
        Long userId = UserContext.getIdUsuario();

        if (request.getEmailSenhaApp() != null && request.getEmailSenhaApp().length() > 50) {
            throw new IllegalArgumentException("Senha de app excessivamente longa");
        }

        String hash = encryptUtils.encrypt(16, request.getEmailSenhaApp());

        Configuracao config;
        try {
            config = dao.select()
                    .from(Configuracao.class)
                    .where("usuario.id", Condicao.EQUAL, userId)
                    .one();

            config.setEmailRemetente(request.getEmailRemetente());
            config.setEmailSenhaApp(hash);
            config.setUpdatedAt(new Date());
            trans.update(config);

        } catch (EntityNotFoundException e) {
            config = new Configuracao();
            Usuario usuario = trans.selectById(Usuario.class, userId);
            if (usuario == null) {
                throw new IllegalStateException("Usuário não encontrado");
            }
            config.setUsuario(usuario);
            // Garante campo obrigatório existente
            config.setApiToken("");
            config.setEmailRemetente(request.getEmailRemetente());
            config.setEmailSenhaApp(hash);
            trans.insert(config);
        }

        return new EmailConfigResponseDTO(config.getEmailRemetente(), true);
    }

    @Transactional(readOnly = true)
    public EmailConfigResponseDTO buscarEmailConfig() {
        Long userId = UserContext.getIdUsuario();
        List<Configuracao> configs = dao.select()
                .from(Configuracao.class)
                .where("usuario.id", Condicao.EQUAL, userId)
                .list();

        Configuracao config = configs.isEmpty() ? null : configs.get(0);
        String emailRemetente = (config != null) ? config.getEmailRemetente() : null;
        boolean hasSenhaApp = (config != null && config.getEmailSenhaApp() != null
                && !config.getEmailSenhaApp().isEmpty());
        return new EmailConfigResponseDTO(emailRemetente, hasSenhaApp);
    }

    @Transactional
    public void deletarEmailConfig() throws Exception {
        Long userId = UserContext.getIdUsuario();
        try {
            Configuracao config = dao.select()
                    .from(Configuracao.class)
                    .where("usuario.id", Condicao.EQUAL, userId)
                    .one();

            config.setEmailRemetente(null);
            config.setEmailSenhaApp(null);
            config.setUpdatedAt(new Date());
            trans.update(config);
        } catch (EntityNotFoundException e) {
            return;
        }
    }

    @Transactional
    public void salvarMercadoPagoConfig(com.sdk.mpoauth.model.MercadoPagoTokenResponse response) throws Exception {
        Long userId = UserContext.getIdUsuario();
        Unidade unidade = ApiUserContext.getUnidade();

        Configuracao config;
        try {
            config = dao.select()
                    .from(Configuracao.class)
                    .where("usuario.id", Condicao.EQUAL, userId)
                    .one();
        } catch (EntityNotFoundException e) {
            config = new Configuracao();
            Usuario usuario = trans.selectById(Usuario.class, userId);
            if (usuario == null) {
                throw new IllegalStateException("Usuário não encontrado");
            }
            config.setUsuario(usuario);
            config.setApiToken("");
            config.setAtivo(true);
        }

        config.setUnidade(unidade);
        config.setMpAccessToken(response.getAccessToken());
        config.setMpRefreshToken(response.getRefreshToken());
        config.setMpPublicKey(response.getPublicKey());
        config.setMpUserId(response.getUserId() != null ? String.valueOf(response.getUserId()) : null);

        if (response.getExpiresIn() != null) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, response.getExpiresIn().intValue());
            config.setMpExpiresAt(cal.getTime());
        }

        config.setUpdatedAt(new Date());

        if (config.getId() == null) {
            trans.insert(config);
        } else {
            trans.update(config);
        }
    }

    @Transactional(readOnly = true)
    public MercadoPagoConfigResponseDTO buscarMercadoPagoConfig() throws Exception {
        Unidade unidade = ApiUserContext.getUnidade();

        if (unidade == null) {
            return new MercadoPagoConfigResponseDTO(false, null);
        }

        List<Configuracao> configs;
        try {
            configs = dao.select()
                    .from(Configuracao.class)
                    .where("unidade.id", Condicao.EQUAL, unidade.getId())
                    .list();

        } catch (Exception e) {
            e.printStackTrace();
            return new MercadoPagoConfigResponseDTO(false, null);
        }

        Configuracao config = configs.isEmpty() ? null : configs.get(0);

        return new MercadoPagoConfigResponseDTO(
                config != null && config.getMpAccessToken() != null,
                config != null ? config.getTipoPagamentoMp() : null);
    }

    @Transactional
    public void atualizarTipoPagamentoMercadoPago(
            com.gestao.lafemme.api.controllers.dto.MercadoPagoConfigRequestDTO request) throws Exception {
        Unidade unidade = ApiUserContext.getUnidade();
        Long userId = UserContext.getIdUsuario();
        if (unidade == null)
            return;

        Configuracao config;
        try {
            config = dao.select()
                    .from(Configuracao.class)
                    .where("unidade.id", Condicao.EQUAL, unidade.getId())
                    .one();
        } catch (Exception e) {
            config = new Configuracao();
            Usuario usuario = trans.selectById(Usuario.class, userId);
            if (usuario == null)
                throw new IllegalStateException("Usuário não encontrado");
            config.setUsuario(usuario);
            config.setApiToken("");
            config.setAtivo(true);
            config.setUnidade(unidade);
        }

        config.setTipoPagamentoMp(request.tipoPagamento());
        config.setUpdatedAt(new Date());

        if (config.getId() == null) {
            trans.insert(config);
        } else {
            trans.update(config);
        }
    }

    @Transactional(readOnly = true)
    public ConfiguracaoMP getMercadoPagoConfig() {
        Unidade unidade = ApiUserContext.getUnidade();

        if (unidade == null) {
            return null;
        }
        Configuracao config;
        try {
            config = dao.select()
                    .from(Configuracao.class)
                    .where("unidade.id", Condicao.EQUAL, unidade.getId())
                    .one();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return new ConfiguracaoMP(
                config.getMpAccessToken(),
                config.getMpWebhookSecret(),
                config.getTipoPagamentoMp());
    }

    @Transactional(readOnly = true)
    public Configuracao buscarPorMpUserId(String mpUserId) throws Exception {
        if (mpUserId == null || mpUserId.isBlank()) {
            throw new BusinessException("MP User ID é obrigatório.");
        }
        try {
            Configuracao config = dao.select()
                    .from(Configuracao.class)
                    .where("mpUserId", Condicao.EQUAL, mpUserId)
                    .one();
            return config;

        } catch (NotFoundException e) {
            throw new NotFoundException("Configuração não encontrada.");
        }
    }

    @Transactional(readOnly = true)
    public Configuracao buscarPrimeiraConfiguracaoValida() {
        List<Configuracao> configs;
        try {
            configs = dao.select().from(Configuracao.class)
                    .list();
            for (Configuracao config : configs) {
                if (config.getMpAccessToken() != null && !config.getMpAccessToken().isBlank()) {
                    return config;
                }
            }
        } catch (Exception e) {
            throw new BusinessException("Erro ao buscar configuração.");
        }
        return null;
    }
}

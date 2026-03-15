package com.gestao.lafemme.api.bo;

import java.util.Properties;

import org.springframework.stereotype.Component;

import com.gestao.lafemme.api.entity.Configuracao;
import com.gestao.lafemme.api.services.ConfiguracaoRepository;
import com.gestao.lafemme.api.utils.StringEncryptUtils;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@Component
public class EmailBO {

    private final ConfiguracaoRepository configuracaoRepository;

    private String destinatario;
    private String remetente;
    private String senhaApp;
    private String titulo;
    private String corpo;

    private final StringEncryptUtils encryptUtils;

    public EmailBO(ConfiguracaoRepository configuracaoRepository, StringEncryptUtils encryptUtils) {
        this.configuracaoRepository = configuracaoRepository;
        this.encryptUtils = encryptUtils;
    }

    public EmailBO criar() {
        this.destinatario = null;
        this.remetente    = null;
        this.senhaApp     = null;
        this.titulo       = null;
        this.corpo        = null;
        return this;
    }

    public EmailBO destinatario(String email) {
        this.destinatario = email;
        return this;
    }

    /**
     * Preenche remetente + senha de app a partir da Configuracao do usuário logado.
     * Uso: .remetente(userContext.getUserId())
     * @throws Exception 
     * @throws IllegalStateException 
     */
    public EmailBO remetente(Integer userId) throws IllegalStateException, Exception {
        Configuracao config = configuracaoRepository.findByUsuarioId(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Configuração de e-mail não encontrada para o usuário: " + userId));

        if (config.getEmailRemetente() == null || config.getEmailSenhaApp() == null) {
            throw new IllegalStateException(
                    "E-mail remetente ou senha de app não configurados. Acesse Configurações > E-mail.");
        }

        this.remetente = config.getEmailRemetente();
        this.senhaApp  = encryptUtils.decrypt(config.getEmailSenhaApp());
        return this;
    }

    public EmailBO remetente(String email, String senhaApp) {
        this.remetente = email;
        this.senhaApp  = senhaApp;
        return this;
    }

    public EmailBO titulo(String titulo) {
        this.titulo = titulo;
        return this;
    }

    public EmailBO mensagem(String corpo) {
        this.corpo = corpo;
        return this;
    }

    public EmailBO mensagem(String titulo, String corpo) {
        this.titulo = titulo;
        this.corpo  = corpo;
        return this;
    }

    // -------------------------------------------------------------------------
    // Envio
    // -------------------------------------------------------------------------

    public void enviar() {
        validar();

        Properties props = buildSmtpProperties();

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(remetente, senhaApp);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(remetente));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            message.setSubject(titulo, "UTF-8");
            message.setContent(corpo, "text/html; charset=UTF-8");

            Transport.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Falha ao enviar e-mail para: " + destinatario, e);
        }
    }

    // -------------------------------------------------------------------------
    // Internos
    // -------------------------------------------------------------------------

    private Properties buildSmtpProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",            "587");
        props.put("mail.smtp.ssl.trust",       "smtp.gmail.com");
        return props;
    }

    private void validar() {
        if (destinatario == null || destinatario.isBlank())
            throw new IllegalStateException("Destinatário não informado.");
        if (remetente == null || remetente.isBlank())
            throw new IllegalStateException("Remetente não informado.");
        if (senhaApp == null || senhaApp.isBlank())
            throw new IllegalStateException("Senha de app não informada.");
        if (titulo == null || titulo.isBlank())
            throw new IllegalStateException("Título do e-mail não informado.");
        if (corpo == null || corpo.isBlank())
            throw new IllegalStateException("Corpo do e-mail não informado.");
    }

    public EmailBO remetente(Long id) throws IllegalStateException, Exception {
        return remetente(id.intValue());
    }
}
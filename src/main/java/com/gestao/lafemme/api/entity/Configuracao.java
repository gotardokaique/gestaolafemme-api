package com.gestao.lafemme.api.entity;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.ManyToOne;

@Entity
@Table(name = "configuracao")
public class Configuracao implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conf_id")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conf_user_id", nullable = false, unique = true)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uni_id")
    private Unidade unidade;

    @Column(name = "conf_mp_access_token", columnDefinition = "TEXT")
    private String mpAccessToken;

    @Column(name = "conf_mp_refresh_token", columnDefinition = "TEXT")
    private String mpRefreshToken;

    @Column(name = "conf_mp_public_key")
    private String mpPublicKey;

    @Column(name = "conf_mp_user_id")
    private String mpUserId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "conf_mp_expires_at")
    private Date mpExpiresAt;

    @Column(name = "conf_api_token", nullable = false, columnDefinition = "TEXT")
    private String apiToken;

    @Column(name = "conf_ativo", nullable = false)
    private boolean ativo = true;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "conf_created_at", nullable = false, updatable = false)
    private Date createdAt;

    @Column(name = "conf_email_remetente")
    private String emailRemetente;

    @Column(name = "conf_email_senha_app")
    private String emailSenhaApp;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "conf_updated_at")
    private Date updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) this.createdAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = new Date();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getEmailRemetente() {
        return emailRemetente;
    }

    public void setEmailRemetente(String emailRemetente) {
        this.emailRemetente = emailRemetente;
    }

    public String getEmailSenhaApp() {
        return emailSenhaApp;
    }

    public void setEmailSenhaApp(String emailSenhaApp) {
        this.emailSenhaApp = emailSenhaApp;
    }

    public Unidade getUnidade() {
        return unidade;
    }

    public void setUnidade(Unidade unidade) {
        this.unidade = unidade;
    }

    public String getMpAccessToken() {
        return mpAccessToken;
    }

    public void setMpAccessToken(String mpAccessToken) {
        this.mpAccessToken = mpAccessToken;
    }

    public String getMpRefreshToken() {
        return mpRefreshToken;
    }

    public void setMpRefreshToken(String mpRefreshToken) {
        this.mpRefreshToken = mpRefreshToken;
    }

    public String getMpPublicKey() {
        return mpPublicKey;
    }

    public void setMpPublicKey(String mpPublicKey) {
        this.mpPublicKey = mpPublicKey;
    }

    public String getMpUserId() {
        return mpUserId;
    }

    public void setMpUserId(String mpUserId) {
        this.mpUserId = mpUserId;
    }

    public Date getMpExpiresAt() {
        return mpExpiresAt;
    }

    public void setMpExpiresAt(Date mpExpiresAt) {
        this.mpExpiresAt = mpExpiresAt;
    }
}

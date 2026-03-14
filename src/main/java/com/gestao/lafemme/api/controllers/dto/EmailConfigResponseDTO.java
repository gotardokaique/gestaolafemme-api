package com.gestao.lafemme.api.controllers.dto;

public class EmailConfigResponseDTO {

    private String emailRemetente;
    private boolean hasSenhaApp;

    public EmailConfigResponseDTO() {}

    public EmailConfigResponseDTO(String emailRemetente, boolean hasSenhaApp) {
        this.emailRemetente = emailRemetente;
        this.hasSenhaApp = hasSenhaApp;
    }

    public String getEmailRemetente() {
        return emailRemetente;
    }

    public void setEmailRemetente(String emailRemetente) {
        this.emailRemetente = emailRemetente;
    }

    public boolean isHasSenhaApp() {
        return hasSenhaApp;
    }

    public void setHasSenhaApp(boolean hasSenhaApp) {
        this.hasSenhaApp = hasSenhaApp;
    }
}

package com.gestao.lafemme.api.controllers.dto;

public class EmailConfigRequestDTO {

    private String emailRemetente;
    private String emailSenhaApp;

    public EmailConfigRequestDTO() {}

    public EmailConfigRequestDTO(String emailRemetente, String emailSenhaApp) {
        this.emailRemetente = emailRemetente;
        this.emailSenhaApp = emailSenhaApp;
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
}

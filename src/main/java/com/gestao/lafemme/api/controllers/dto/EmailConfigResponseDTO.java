package com.gestao.lafemme.api.controllers.dto;

public class EmailConfigResponseDTO {

    private String emailRemetente;

    public EmailConfigResponseDTO() {}

    public EmailConfigResponseDTO(String emailRemetente) {
        this.emailRemetente = emailRemetente;
    }

    public String getEmailRemetente() {
        return emailRemetente;
    }

    public void setEmailRemetente(String emailRemetente) {
        this.emailRemetente = emailRemetente;
    }
}

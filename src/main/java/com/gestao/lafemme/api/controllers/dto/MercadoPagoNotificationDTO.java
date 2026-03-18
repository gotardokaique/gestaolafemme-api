package com.gestao.lafemme.api.controllers.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MercadoPagoNotificationDTO {
    private String type;
    
    @JsonProperty("user_id")
    private String userId;
    
    private Data data;

    public static class Data {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    private String action;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}

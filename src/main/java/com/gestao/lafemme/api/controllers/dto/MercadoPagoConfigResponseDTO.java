package com.gestao.lafemme.api.controllers.dto;

import java.io.Serializable;

public record MercadoPagoConfigResponseDTO(
    boolean conectado
) implements Serializable {}

package com.gestao.lafemme.api.controllers.dto;

import java.util.UUID;
import java.io.Serializable;

public record CriarUnidadeResponseDTO(UUID idUnidade) implements Serializable {}

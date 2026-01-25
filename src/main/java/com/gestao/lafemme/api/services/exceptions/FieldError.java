package com.gestao.lafemme.api.services.exceptions;

public record FieldError(
    String fieldName,
    String message
) {}
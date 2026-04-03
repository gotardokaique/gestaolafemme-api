package com.gestao.lafemme.api.config;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestao.lafemme.api.controllers.dto.ApiResponse;

@ControllerAdvice
public class ApiResponseWrapperAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        // Não interceptar respostas binárias — ByteArrayHttpMessageConverter
        // espera byte[] e lançaria ClassCastException se recebesse ApiResponse
        return converterType != ByteArrayHttpMessageConverter.class;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                 MethodParameter returnType,
                                 MediaType selectedContentType,
                                 Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                 ServerHttpRequest request,
                                 ServerHttpResponse response) {

        if (body instanceof ApiResponse<?>) return body;

        // Não envolver tipos binários / não-JSON
        if (selectedContentType != null && !selectedContentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
            return body;
        }

        if (selectedConverterType == StringHttpMessageConverter.class) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            String msg = (body == null ? "" : body.toString());
            try {
                return "{\"success\":true,\"message\":" + objectMapper.writeValueAsString(msg) + "}";
            } catch (Exception e) {
                return "{\"success\":false,\"message\":\"Erro ao processar resposta\"}";
            }
        }

        return ApiResponse.ok(body);
    }
}


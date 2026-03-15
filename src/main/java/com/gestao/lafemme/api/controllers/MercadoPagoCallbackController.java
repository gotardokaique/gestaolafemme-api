package com.gestao.lafemme.api.controllers;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gestao.lafemme.api.services.ConfiguracaoService;
import com.sdk.mpoauth.controller.MercadoPagoOAuthController;
import com.sdk.mpoauth.model.MercadoPagoTokenResponse;
import com.sdk.mpoauth.service.MercadoPagoOAuthService;

@RestController
public class MercadoPagoCallbackController extends MercadoPagoOAuthController {

    private final MercadoPagoOAuthService oAuthService;
    private final ConfiguracaoService configuracaoService;

    public MercadoPagoCallbackController(MercadoPagoOAuthService oAuthService, ConfiguracaoService configuracaoService) {
        super(oAuthService);
        this.oAuthService = oAuthService;
        this.configuracaoService = configuracaoService;
    }

    @Override
    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String state
    ) {
        try {
            // Se houver erro ou não houver code, redireciona de volta com erro
            if (error != null || code == null || code.isBlank()) {
                return ResponseEntity.status(302)
                        .location(URI.create("/configuracoes?error=mp_auth_failed"))
                        .build();
            }

            // Troca o code pelo token usando o SDK
            MercadoPagoTokenResponse tokenResponse = oAuthService.trocarCodePorToken(code);

            // Salva a configuração no banco de dados
            configuracaoService.salvarMercadoPagoConfig(tokenResponse);

            // Redireciona de volta para a tela de configurações no frontend
            return ResponseEntity.status(302)
                    .location(URI.create("/configuracoes?success=mp_connected"))
                    .build();
        } catch (Exception e) {
            return ResponseEntity.status(302)
                    .location(URI.create("/configuracoes?error=mp_save_failed"))
                    .build();
        }
    }
}

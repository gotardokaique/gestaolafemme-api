package com.gestao.lafemme.api.controllers;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gestao.lafemme.api.services.ConfiguracaoService;
import com.sdk.mpoauth.model.MercadoPagoTokenResponse;
import com.sdk.mpoauth.service.MercadoPagoOAuthService;

@RestController
@RequestMapping("/mp")
public class MercadoPagoCallbackController {

    private final MercadoPagoOAuthService oAuthService;
    private final ConfiguracaoService configuracaoService;

    // Injeção da URL do frontend a partir do application.properties
    @Value("${app.frontend.url}")
    private String frontendUrl;

    public MercadoPagoCallbackController(MercadoPagoOAuthService oAuthService, ConfiguracaoService configuracaoService) {
        this.oAuthService = oAuthService;
        this.configuracaoService = configuracaoService;
    }

    @GetMapping("/callback")
    public ResponseEntity<?> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String state
    ) {
        try {
            if (state == null || state.isBlank()) {
                return redirecionarComErro("mp_invalid_state");
            }

            if (error != null || code == null || code.isBlank()) {
                return redirecionarComErro("mp_auth_failed");
            }

            MercadoPagoTokenResponse tokenResponse = oAuthService.trocarCodePorToken(code);

            configuracaoService.salvarMercadoPagoConfig(tokenResponse);

            return ResponseEntity.status(302)
                    .location(URI.create(frontendUrl + "/configuracoes?success=mp_connected"))
                    .build();

        } catch (Exception e) {
            return redirecionarComErro("mp_save_failed");	
        }
    }
    
    @GetMapping("/autorizar")
    public ResponseEntity<Void> autorizar(@RequestParam(required = false) String state) {
        String url = oAuthService.gerarUrlAutorizacao(state);
        
        return ResponseEntity.status(302)
                .location(URI.create(url))
                .build();
    }

    private ResponseEntity<Void> redirecionarComErro(String motivo) {
        return ResponseEntity.status(302)
                .location(URI.create(frontendUrl + "/configuracoes?error=" + motivo))
                .build();
    }
}
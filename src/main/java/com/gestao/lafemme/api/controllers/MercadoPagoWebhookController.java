package com.gestao.lafemme.api.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.gestao.lafemme.api.controllers.dto.MercadoPagoNotificationDTO;
import com.gestao.lafemme.api.entity.Configuracao;
import com.gestao.lafemme.api.services.ConfiguracaoService;
import com.gestao.lafemme.api.services.VendaService;

@RestController
@RequestMapping("/api/v1/vendas/webhook/mercadopago")
public class MercadoPagoWebhookController {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookController.class);

    private final ConfiguracaoService configuracaoService;
    private final VendaService vendaService;

    public MercadoPagoWebhookController(ConfiguracaoService configuracaoService, VendaService vendaService) {
        this.configuracaoService = configuracaoService;
        this.vendaService = vendaService;
    }

    @PostMapping
    public ResponseEntity<String> receiveWebhook(
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestBody MercadoPagoNotificationDTO payload) {

        try {
            if (!"payment".equals(payload.getType())) {
                return ResponseEntity.ok("Ignored non-payment");
            }

            if (payload.getUserId() == null || payload.getData() == null || payload.getData().getId() == null) {
                return ResponseEntity.badRequest().body("Invalid payload");
            }

            Configuracao config = configuracaoService.buscarPorMpUserId(payload.getUserId());
            if (config == null || config.getMpWebhookSecret() == null) {
                log.warn("Webhook received but no configuration found for user {}", payload.getUserId());
                return ResponseEntity.badRequest().body("Config missing");
            }

            if (xSignature == null || xRequestId == null) {
                log.warn("Missing signature headers");
                return ResponseEntity.badRequest().body("Missing headers");
            }

            if (!validateSignature(xSignature, xRequestId, payload.getData().getId(), config.getMpWebhookSecret())) {
                log.warn("Invalid signature for payment {}", payload.getData().getId());
                return ResponseEntity.badRequest().body("Invalid signature");
            }

            // Authenticate temporarily as the user associated with this webhook config
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    config.getUsuario(), null, config.getUsuario().getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            try {
                vendaService.confirmarPagamentoViaMp(payload.getData().getId(), config);
            } finally {
                SecurityContextHolder.clearContext();
            }

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error processing Mercado Pago webhook", e);
            return ResponseEntity.internalServerError().body("Error processing webhook");
        }
    }

    private boolean validateSignature(String xSignature, String xRequestId, String dataId, String secret) {
        try {
            String ts = null;
            String v1 = null;
            for (String part : xSignature.split(",")) {
                if (part.trim().startsWith("ts=")) ts = part.trim().substring(3);
                else if (part.trim().startsWith("v1=")) v1 = part.trim().substring(3);
            }

            if (ts == null || v1 == null) return false;

            String manifest = "id:" + dataId + ";request-id:" + xRequestId + ";ts:" + ts + ";";
            javax.crypto.Mac sha256_HMAC = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secret_key = new javax.crypto.spec.SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            
            byte[] hash = sha256_HMAC.doFinal(manifest.getBytes("UTF-8"));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString().equals(v1);
        } catch (Exception e) {
            log.error("Signature validation error", e);
            return false;
        }
    }
}

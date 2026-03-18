package com.gestao.lafemme.api.controllers;

import com.gestao.lafemme.api.controllers.dto.MercadoPagoNotificationDTO;
import com.gestao.lafemme.api.entity.Configuracao;
import com.gestao.lafemme.api.services.ConfiguracaoService;
import com.gestao.lafemme.api.services.VendaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mp/webhook/mercadopago")
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

        log.info("🔔 WEBHOOK RECEBIDO");
        log.info("Headers -> x-signature: {}", xSignature != null ? "✓" : "✗");
        log.info("Headers -> x-request-id: {}", xRequestId != null ? "✓" : "✗");
        log.info("Payload type: {}", payload.getType());
        log.info("Payload action: {}", payload.getAction());
        log.info("Payload user_id: {}", payload.getUserId());
        log.info("Payload data.id: {}", payload.getData() != null ? payload.getData().getId() : "NULL");

        try {
            boolean isPayment = "payment".equals(payload.getType()) ||
                    (payload.getAction() != null && payload.getAction().startsWith("payment."));

            log.info("Is payment? {}", isPayment);

            if (!isPayment) {
                log.warn("❌ Ignorado: não é pagamento");
                return ResponseEntity.ok("Ignored non-payment");
            }

            if (payload.getData() == null || payload.getData().getId() == null) {
                log.error("❌ Payload inválido: falta data.id");
                return ResponseEntity.badRequest().body("Invalid payload: missing data.id");
            }

            Configuracao config = null;
            if (payload.getUserId() != null) {
                log.info("Buscando config por user_id: {}", payload.getUserId());
                config = configuracaoService.buscarPorMpUserId(payload.getUserId());
            } else {
                log.info("Buscando primeira config válida");
                config = configuracaoService.buscarPrimeiraConfiguracaoValida();
            }

            if (config == null) {
                log.error("❌ Nenhuma configuração encontrada");
                return ResponseEntity.badRequest().body("Config missing");
            }

            log.info("✓ Config encontrada: conf_id={}", config.getId());

            if (config.getMpWebhookSecret() == null) {
                log.error("❌ Configuração sem webhook secret!");
                return ResponseEntity.badRequest().body("No webhook secret configured");
            }

            log.info("✓ Webhook secret existe");

            if (xSignature == null || xRequestId == null) {
                log.error("❌ Headers de assinatura ausentes!");
                return ResponseEntity.badRequest().body("Missing headers");
            }

            log.info("Validando assinatura...");
            if (!validateSignature(xSignature, xRequestId, payload.getData().getId(), config.getMpWebhookSecret())) {
                log.error("❌ Assinatura inválida!");
                return ResponseEntity.badRequest().body("Invalid signature");
            }

            log.info("✓ Assinatura validada com sucesso!");

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    config.getUsuario(), null, config.getUsuario().getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            try {
                log.info("Confirmando pagamento para payment_id: {}", payload.getData().getId());
                vendaService.confirmarPagamentoViaMp(payload.getData().getId(), config);
                log.info("✓ Pagamento confirmado com sucesso!");
            } finally {
                SecurityContextHolder.clearContext();
            }

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("❌ Erro ao processar webhook", e);
            return ResponseEntity.internalServerError().body("Error processing webhook");
        }
    }

    private boolean validateSignature(String xSignature, String xRequestId, String dataId, String secret) {
        try {
            String ts = null;
            String v1 = null;
            for (String part : xSignature.split(",")) {
                if (part.trim().startsWith("ts="))
                    ts = part.trim().substring(3);
                else if (part.trim().startsWith("v1="))
                    v1 = part.trim().substring(3);
            }

            if (ts == null || v1 == null) {
                log.error("❌ Assinatura malformada: faltam ts ou v1");
                return false;
            }

            String manifest = "id:" + dataId + ";request-id:" + xRequestId + ";ts:" + ts + ";";
            log.debug("Manifest para validar: {}", manifest);

            javax.crypto.Mac sha256_HMAC = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secret_key = new javax.crypto.spec.SecretKeySpec(secret.getBytes("UTF-8"),
                    "HmacSHA256");
            sha256_HMAC.init(secret_key);

            byte[] hash = sha256_HMAC.doFinal(manifest.getBytes("UTF-8"));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }

            String calculated = hexString.toString();
            boolean valid = calculated.equals(v1);

            log.info("Comparando assinaturas:");
            log.info("  Esperado: {}", v1);
            log.info("  Calculado: {}", calculated);
            log.info("  Válido? {}", valid);

            return valid;
        } catch (Exception e) {
            log.error("❌ Erro na validação de assinatura", e);
            return false;
        }
    }
}
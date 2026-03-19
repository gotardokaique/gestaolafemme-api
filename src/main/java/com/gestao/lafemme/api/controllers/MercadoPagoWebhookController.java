package com.gestao.lafemme.api.controllers;

import com.gestao.lafemme.api.controllers.dto.MercadoPagoNotificationDTO;
import com.gestao.lafemme.api.entity.Configuracao;
import com.gestao.lafemme.api.services.ConfiguracaoService;
import com.gestao.lafemme.api.services.VendaService;
import com.gestao.lafemme.api.utils.StringEncryptUtils;

import jakarta.servlet.http.HttpServletRequest;

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
    private boolean isPayment = false;

    public MercadoPagoWebhookController(ConfiguracaoService configuracaoService, VendaService vendaService) {
        this.configuracaoService = configuracaoService;
        this.vendaService = vendaService;
    }

    @PostMapping
    public ResponseEntity<String> receiveWebhook(
            HttpServletRequest request,
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestBody MercadoPagoNotificationDTO payload) {

        try {
            isPayment = "payment".equals(payload.getType())
                    || (payload.getAction() != null && payload.getAction().startsWith("payment."));

            if (!isPayment) {
                return ResponseEntity.ok("Ignorado por não ser pagamento.");
            }

            if (payload.getData() == null || payload.getData().getId() == null) {
                return ResponseEntity.badRequest().body("Payload inválido: falta data.id");
            }

            Configuracao config = null;
            if (payload.getUserId() != null) {
                config = configuracaoService.buscarPorMpUserId(payload.getUserId());
            } else {
                config = configuracaoService.buscarPrimeiraConfiguracaoValida();
            }

            if (config == null) {
                return ResponseEntity.badRequest().body("Configuração não encontrada.");
            }

            if (config.getMpWebhookSecret() == null) {
                return ResponseEntity.badRequest().body("Nenhum webhook secret configurado.");
            }

            if (xSignature == null || xRequestId == null) {
                return ResponseEntity.badRequest().body("Headers de assinatura ausentes.");
            }

            log.info("Validando assinatura...");
            if (!StringEncryptUtils.validateSignature(xSignature, xRequestId, payload.getData().getId(),
                    config.getMpWebhookSecret())) {
                log.error("❌ Assinatura inválida!");
                return ResponseEntity.badRequest().body("Assinatura inválida.");
            }

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
            log.error("Erro ao processar webhook", e);
            return ResponseEntity.internalServerError().body("Erro ao processar webhook");
        }
    }
}
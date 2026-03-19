package com.gestao.lafemme.api.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.gestao.lafemme.api.controllers.dto.MercadoPagoPixResponse;
import com.gestao.lafemme.api.controllers.dto.MercadoPagoPreferenceResponse;
import com.gestao.lafemme.api.db.Condicao;
import com.gestao.lafemme.api.db.DAOController;
import com.gestao.lafemme.api.entity.MovimentacaoEstoque;
import com.gestao.lafemme.api.entity.Venda;
import com.gestao.lafemme.api.services.exceptions.BusinessException;

@Service
public class MercadoPagoService {

    @Value("${app.base-url}")
    private String baseUrl;

    private final DAOController dao;
    private final String urlPreference = "https://api.mercadopago.com/checkout/preferences";
    private final String urlPayment = "https://api.mercadopago.com/v1/payments";

    public MercadoPagoService(DAOController dao) {
        this.dao = dao;
    }

    public MercadoPagoPreferenceResponse criarPreference(Venda venda, String accessToken) throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        List<MovimentacaoEstoque> movs = dao.select()
                .from(MovimentacaoEstoque.class)
                .join("produto")
                .where("venda.id", Condicao.EQUAL, venda.getId())
                .list();

        List<Map<String, Object>> items = new ArrayList<>();

        // Montamos os itens do pagamento
        if (movs.isEmpty()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", UUID.randomUUID().toString());
            item.put("title", "Venda #" + venda.getId());
            item.put("quantity", 1);
            item.put("unit_price", venda.getValorTotal());
            item.put("currency_id", "BRL");
            items.add(item);
        } else {
            for (MovimentacaoEstoque mov : movs) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", mov.getProduto().getId().toString());
                item.put("title", mov.getProduto().getNome());
                item.put("quantity", mov.getQuantidade());
                item.put("unit_price", mov.getProduto().getValorVenda());
                item.put("currency_id", "BRL");
                items.add(item);
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("items", items);

        Map<String, String> backUrls = new HashMap<>();
        backUrls.put("success", baseUrl + "/mp/webhook/mercadopago");
        backUrls.put("failure", baseUrl + "/mp/webhook/mercadopago");
        backUrls.put("pending", baseUrl + "/mp/webhook/mercadopago");

        body.put("back_urls", backUrls);
        body.put("auto_return", "approved");
        body.put("notification_url", baseUrl + "/mp/webhook/mercadopago");
        body.put("external_reference", venda.getMpExternalReference());

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(urlPreference, requestEntity, Map.class);
            if (response != null && response.containsKey("init_point") && response.containsKey("id")) {
                return new MercadoPagoPreferenceResponse(
                        response.get("init_point").toString(),
                        response.get("id").toString());
            }
            throw new BusinessException("Resposta inválida do Mercado Pago.");
        } catch (Exception e) {
            throw new BusinessException("Erro ao gerar link de pagamento: " + e.getMessage());
        }
    }

    public Map<String, Object> consultarPagamento(String paymentId, String accessToken) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        String url = urlPayment + paymentId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate
                .exchange(url, org.springframework.http.HttpMethod.GET, entity, Map.class).getBody();
        return response;
    }

    public MercadoPagoPixResponse criarPagamentoPix(Venda venda, String accessToken) throws Exception {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.set("X-Idempotency-Key", UUID.randomUUID().toString());

        Map<String, Object> body = new HashMap<>();
        body.put("transaction_amount", venda.getValorTotal());
        body.put("description", "Venda #" + venda.getId());
        body.put("payment_method_id", "pix");

        Map<String, String> payer = new HashMap<>();
        payer.put("email", "cliente@gestaolafemme.com.br");
        body.put("payer", payer);

        body.put("external_reference", venda.getMpExternalReference());
        body.put("notification_url", baseUrl + "/mp/webhook/mercadopago");

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(urlPayment, requestEntity, Map.class);

            if (response != null && response.containsKey("point_of_interaction")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> poi = (Map<String, Object>) response.get("point_of_interaction");
                @SuppressWarnings("unchecked")
                Map<String, Object> txData = (Map<String, Object>) poi.get("transaction_data");

                String qrCode = txData.get("qr_code").toString();
                String qrCodeBase64 = txData.get("qr_code_base64").toString();
                String extRef = response.containsKey("external_reference") && response.get("external_reference") != null
                        ? response.get("external_reference").toString()
                        : venda.getMpExternalReference();

                return new com.gestao.lafemme.api.controllers.dto.MercadoPagoPixResponse(qrCode, qrCodeBase64, extRef);
            }
            throw new BusinessException("Resposta inválida do Mercado Pago ao gerar Pix.");
        } catch (Exception e) {
            throw new BusinessException("Erro ao gerar Pix: " + e.getMessage());
        }
    }
}

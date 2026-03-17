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

    public MercadoPagoService(DAOController dao) {
        this.dao = dao;
    }

    public MercadoPagoPreferenceResponse criarPreference(Venda venda, String accessToken) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.mercadopago.com/checkout/preferences";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        List<MovimentacaoEstoque> movs = dao.select()
                .from(MovimentacaoEstoque.class)
                .join("produto")
                .where("venda.id", Condicao.EQUAL, venda.getId())
                .list();

        List<Map<String, Object>> items = new ArrayList<>();
        
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

        String externalRef = UUID.randomUUID().toString();

        Map<String, Object> body = new HashMap<>();
        body.put("items", items);
        
        Map<String, String> backUrls = new HashMap<>();
        backUrls.put("success", baseUrl + "/api/vendas/webhook/mercadopago");
        backUrls.put("failure", baseUrl + "/api/vendas/webhook/mercadopago");
        backUrls.put("pending", baseUrl + "/api/vendas/webhook/mercadopago");
        
        body.put("back_urls", backUrls);
        body.put("auto_return", "approved");
        body.put("notification_url", baseUrl + "/api/vendas/webhook/mercadopago");
        body.put("external_reference", externalRef);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, requestEntity, Map.class);
            if (response != null && response.containsKey("init_point") && response.containsKey("id")) {
                return new MercadoPagoPreferenceResponse(
                        response.get("init_point").toString(),
                        response.get("id").toString()
                );
            }
            throw new BusinessException("Resposta inválida do Mercado Pago.");
        } catch (Exception e) {
            throw new BusinessException("Erro ao gerar link de pagamento: " + e.getMessage());
        }
    }
}

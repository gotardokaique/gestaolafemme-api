package com.gestao.lafemme.api.controllers.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.gestao.lafemme.api.entity.Venda;

public record VendaResponseDTO(
        Long id,
        Date dataVenda,
        BigDecimal valorTotal,
        String formaPagamento,
        SituacaoDTO situacao
) {
    public record SituacaoDTO(Integer id, String nome) {}

    public static VendaResponseDTO from(Venda venda) {
        SituacaoDTO sitDto = venda.getSituacao() != null 
            ? new SituacaoDTO(venda.getSituacao().getId(), venda.getSituacao().getNome()) 
            : null;

        return new VendaResponseDTO(
                venda.getId(),
                venda.getDataVenda(),
                venda.getValorTotal(),
                venda.getFormaPagamento(),
                sitDto
        );
    }

    public static List<VendaResponseDTO> refactor(List<Venda> listVenda) {
        return listVenda.stream()
                .filter(Objects::nonNull)
                .map(VendaResponseDTO::from)
                .collect(Collectors.toList());
    }
}

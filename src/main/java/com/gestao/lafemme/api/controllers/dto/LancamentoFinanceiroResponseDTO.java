package com.gestao.lafemme.api.controllers.dto;

import com.gestao.lafemme.api.entity.LancamentoFinanceiro;
import com.gestao.lafemme.api.entity.TipoLancamentoFinanceiro;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public record LancamentoFinanceiroResponseDTO(
        Long id,
        Date dataLancamento,
        TipoLancamentoFinanceiro tipo,
        BigDecimal valor,
        String descricao
) {
    public static LancamentoFinanceiroResponseDTO from(LancamentoFinanceiro entity) {
        return new LancamentoFinanceiroResponseDTO(
                entity.getId(),
                entity.getDataLancamento(),
                entity.getTipo(),
                entity.getValor(),
                entity.getDescricao()
        );
    }

    public static List<LancamentoFinanceiroResponseDTO> refactor(List<LancamentoFinanceiro> entities) {
        return entities.stream().map(LancamentoFinanceiroResponseDTO::from).collect(Collectors.toList());
    }
}

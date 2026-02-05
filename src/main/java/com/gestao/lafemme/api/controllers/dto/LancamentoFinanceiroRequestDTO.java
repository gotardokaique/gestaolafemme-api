package com.gestao.lafemme.api.controllers.dto;

import java.math.BigDecimal;
import java.util.Date;

import com.gestao.lafemme.api.entity.TipoLancamentoFinanceiro;

public record LancamentoFinanceiroRequestDTO(
        Date dataLancamento,
        TipoLancamentoFinanceiro tipo,
        BigDecimal valor,
        String descricao
) {}

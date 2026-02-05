package com.gestao.lafemme.api.controllers.dto;

import java.math.BigDecimal;
import java.util.List;

public record FinanceiroResumoDTO(
        BigDecimal saldoAtual,
        BigDecimal totalEntradas,
        BigDecimal totalSaidas,
        List<LancamentoFinanceiroResponseDTO> lancamentos
) {}

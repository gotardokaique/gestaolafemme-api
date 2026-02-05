package com.gestao.lafemme.api.controllers.dto;

import java.math.BigDecimal;

public record DashboardDTO(
        BigDecimal saldoAtual,
        Long totalVendasMes,
        BigDecimal valorTotalVendasMes,
        Long totalComprasMes,
        BigDecimal valorTotalComprasMes
) {}

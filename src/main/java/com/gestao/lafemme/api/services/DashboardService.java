package com.gestao.lafemme.api.services;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestao.lafemme.api.context.UserContext;
import com.gestao.lafemme.api.controllers.dto.DashboardDTO;
import com.gestao.lafemme.api.controllers.dto.FinanceiroResumoDTO;
import com.gestao.lafemme.api.db.Condicao;
import com.gestao.lafemme.api.db.DAOController;
import com.gestao.lafemme.api.entity.Compra;
import com.gestao.lafemme.api.entity.Venda;

@Service
public class DashboardService {

    private final DAOController dao;
    private final FinanceiroService financeiroService;

    public DashboardService(DAOController dao, FinanceiroService financeiroService) {
        this.dao = dao;
        this.financeiroService = financeiroService;
    }

    @Transactional(readOnly = true)
    public DashboardDTO obterDashboard() {
        // 1. Saldo Atual (via FinanceiroService)
        FinanceiroResumoDTO financeiro = financeiroService.obterResumoFinanceiro();
        BigDecimal saldoAtual = financeiro.saldoAtual();

        // Datas do mês atual
        Date[] rangeMes = getRangeMesAtual();
        Date dataInicio = rangeMes[0];
        Date dataFim = rangeMes[1];

        Long unidadeId = UserContext.getIdUnidade();

        // 2. Vendas no Mês
        List<Venda> vendasMes = dao.select()
                .from(Venda.class)
                .where("unidade.id", Condicao.EQUAL, unidadeId)
                .where("dataVenda", Condicao.GREATER_OR_EQUAL, dataInicio)
                .where("dataVenda", Condicao.LESS_OR_EQUAL, dataFim)
                .list();

        Long totalVendasMes = (long) vendasMes.size();
        BigDecimal valorTotalVendasMes = vendasMes.stream()
                .map(Venda::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Compras no Mês
        List<Compra> comprasMes = dao.select()
                .from(Compra.class)
                .where("unidade.id", Condicao.EQUAL, unidadeId)
                .where("dataCompra", Condicao.GREATER_OR_EQUAL, dataInicio)
                .where("dataCompra", Condicao.LESS_OR_EQUAL, dataFim)
                .list();

        Long totalComprasMes = (long) comprasMes.size();
        BigDecimal valorTotalComprasMes = comprasMes.stream()
                .map(Compra::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardDTO(
                saldoAtual,
                totalVendasMes,
                valorTotalVendasMes,
                totalComprasMes,
                valorTotalComprasMes
        );
    }

    private Date[] getRangeMesAtual() {
        Calendar cal = Calendar.getInstance();
        
        // Inicio do mês: dia 1, 00:00:00
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date start = cal.getTime();

        // Fim do mês: último dia, 23:59:59
        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.SECOND, -1);
        Date end = cal.getTime();

        return new Date[] { start, end };
    }
}

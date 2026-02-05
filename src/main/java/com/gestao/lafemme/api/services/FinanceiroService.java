package com.gestao.lafemme.api.services;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestao.lafemme.api.context.UserContext;
import com.gestao.lafemme.api.controllers.dto.FinanceiroResumoDTO;
import com.gestao.lafemme.api.controllers.dto.LancamentoFinanceiroRequestDTO;
import com.gestao.lafemme.api.controllers.dto.LancamentoFinanceiroResponseDTO;
import com.gestao.lafemme.api.db.Condicao;
import com.gestao.lafemme.api.db.DAOController;
import com.gestao.lafemme.api.entity.LancamentoFinanceiro;
import com.gestao.lafemme.api.entity.TipoLancamentoFinanceiro;

@Service
public class FinanceiroService {

    private final DAOController dao;

    public FinanceiroService(DAOController dao) {
        this.dao = dao;
    }	

    @Transactional(readOnly = true)
    public FinanceiroResumoDTO obterResumoFinanceiro() {
        List<LancamentoFinanceiro> lancamentos = dao.select()
                .from(LancamentoFinanceiro.class)
                .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                .orderBy("dataLancamento", false)
                .list();

        BigDecimal entries = BigDecimal.ZERO;
        BigDecimal exits = BigDecimal.ZERO;

        for (LancamentoFinanceiro l : lancamentos) {
            if (l.getTipo() == TipoLancamentoFinanceiro.ENTRADA) {
                entries = entries.add(l.getValor());
            } else {
                exits = exits.add(l.getValor());
            }
        }

        BigDecimal balance = entries.subtract(exits);

        return new FinanceiroResumoDTO(
                balance,
                entries,
                exits,
                LancamentoFinanceiroResponseDTO.refactor(lancamentos)
        );
    }

    @Transactional
    public void criarLancamento(LancamentoFinanceiroRequestDTO dto) {
        LancamentoFinanceiro lanc = new LancamentoFinanceiro();
        lanc.setDataLancamento(dto.dataLancamento() != null ? dto.dataLancamento() : new Date());
        lanc.setTipo(dto.tipo());
        lanc.setValor(dto.valor());
        lanc.setDescricao(dto.descricao());
        lanc.setUsuario(UserContext.getUsuario());
        lanc.setUnidade(UserContext.getUnidade());

        dao.insert(lanc);
    }
}

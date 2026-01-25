package com.gestao.lafemme.api.services;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestao.lafemme.api.context.UserContext;
import com.gestao.lafemme.api.db.Condicao;
import com.gestao.lafemme.api.db.DAOController;
import com.gestao.lafemme.api.entity.Compra;
import com.gestao.lafemme.api.entity.Fornecedor;
import com.gestao.lafemme.api.entity.LancamentoFinanceiro;
import com.gestao.lafemme.api.entity.MovimentacaoEstoque;
import com.gestao.lafemme.api.entity.TipoLancamentoFinanceiro;
import com.gestao.lafemme.api.entity.TipoMovimentacaoEstoque;
import com.gestao.lafemme.api.entity.Usuario;
import com.gestao.lafemme.api.services.exceptions.BusinessException;
import com.gestao.lafemme.api.services.exceptions.NotFoundException;

@Service
public class CompraService {

    private final DAOController dao;

    public CompraService(DAOController dao) {
        this.dao = dao;
    }

    // ===================== CRIAR COMPRA =====================

    /**
     * Cria uma compra (financeira).
     * As movimentações de estoque serão criadas SEPARADAMENTE.
     */
    @Transactional
    public Compra criarCompra(
            Long fornecedorId,
            BigDecimal valorTotal,
            String formaPagamento
    ) {

        if (fornecedorId == null) {
            throw new BusinessException("Fornecedor é obrigatório.");
        }

        if (valorTotal == null || valorTotal.signum() <= 0) {
            throw new BusinessException("Valor total da compra deve ser maior que zero.");
        }

        if (formaPagamento == null || formaPagamento.isBlank()) {
            throw new BusinessException("Forma de pagamento é obrigatória.");
        }

        Fornecedor fornecedor = buscarFornecedor(fornecedorId);

        Compra compra = new Compra();
        compra.setFornecedor(fornecedor);
        compra.setValorTotal(valorTotal);
        compra.setFormaPagamento(formaPagamento);

        Usuario usuarioRef = new Usuario();
        usuarioRef.setId(UserContext.getIdUsuario());
        compra.setUsuario(usuarioRef);

        return dao.insert(compra);
    }

    // ===================== LANÇAMENTO FINANCEIRO =====================

    /**
     * Registra a saída financeira da compra.
     */
    @Transactional
    public void gerarLancamentoFinanceiro(Compra compra) {

        LancamentoFinanceiro lanc = new LancamentoFinanceiro();
        lanc.setCompra(compra);
        lanc.setTipo(TipoLancamentoFinanceiro.SAIDA);
        lanc.setValor(compra.getValorTotal());
        lanc.setDescricao("Compra - " + compra.getFormaPagamento());
        lanc.setDataLancamento(new Date());

        Usuario usuarioRef = new Usuario();
        usuarioRef.setId(UserContext.getIdUsuario());
        lanc.setUsuario(usuarioRef);

        dao.insert(lanc);
    }

    // ===================== MOVIMENTAÇÃO DE ESTOQUE =====================

    /**
     * Associa uma movimentação de estoque a uma compra já criada.
     */
    @Transactional
    public MovimentacaoEstoque adicionarMovimentacaoEstoque(
            Compra compra,
            MovimentacaoEstoque movimentacao
    ) {

        movimentacao.setCompra(compra);
        movimentacao.setTipoMovimentacao(TipoMovimentacaoEstoque.ENTRADA);
        movimentacao.setDataMovimentacao(new Date());

        Usuario usuarioRef = new Usuario();
        usuarioRef.setId(UserContext.getIdUsuario());
        movimentacao.setUsuario(usuarioRef);

        return dao.insert(movimentacao);
    }

    // ===================== CONSULTAS =====================

    @Transactional(readOnly = true)
    public List<Compra> listarCompras() {
        return dao.select()
                .from(Compra.class)
                .join("fornecedor")
                .join("usuario")
                .where("usuario.id", Condicao.EQUAL, UserContext.getIdUsuario())
                .orderBy("dataCompra", false)
                .list();
    }

    @Transactional(readOnly = true)
    public Compra buscarPorId(Long id) {
        try {
            return dao.select()
                    .from(Compra.class)
                    .join("fornecedor")
                    .join("usuario")
                    .where("usuario.id", Condicao.EQUAL, UserContext.getIdUsuario())
                    .id(id);
        } catch (Exception e) {
            throw new NotFoundException("Compra não encontrada: " + id);
        }
    }

    // ===================== HELPERS =====================

    private Fornecedor buscarFornecedor(Long fornecedorId) {
        try {
            return dao.select()
                    .from(Fornecedor.class)
                    .join("usuario")
                    .where("usuario.id", Condicao.EQUAL, UserContext.getIdUsuario())
                    .id(fornecedorId);
        } catch (Exception e) {
            throw new NotFoundException("Fornecedor não encontrado: " + fornecedorId);
        }
    }
}

package com.gestao.lafemme.api.services;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestao.lafemme.api.context.UserContext;
import com.gestao.lafemme.api.db.Condicao;
import com.gestao.lafemme.api.db.DAOController;
import com.gestao.lafemme.api.entity.Estoque;
import com.gestao.lafemme.api.entity.LancamentoFinanceiro;
import com.gestao.lafemme.api.entity.MovimentacaoEstoque;
import com.gestao.lafemme.api.entity.Produto;
import com.gestao.lafemme.api.entity.TipoLancamentoFinanceiro;
import com.gestao.lafemme.api.entity.TipoMovimentacaoEstoque;
import com.gestao.lafemme.api.entity.Venda;
import com.gestao.lafemme.api.services.exceptions.BusinessException;
import com.gestao.lafemme.api.services.exceptions.NotFoundException;

@Service
public class VendaService {

    private final DAOController dao;

    public VendaService(DAOController dao) {
        this.dao = dao;
    }

    // ===================== CRIAR VENDA =====================

    /**
     * Cria a venda (financeira).
     * As movimentações (itens) são adicionadas separadamente.
     */
    @Transactional
    public Venda criarVenda(
    		BigDecimal valorTotal,
    		String formaPagamento,
    		String observacao
    		) {

        if (valorTotal == null || valorTotal.signum() <= 0) {
            throw new BusinessException("Valor total da venda deve ser maior que zero.");
        }

        if (formaPagamento == null || formaPagamento.isBlank()) {
            throw new BusinessException("Forma de pagamento é obrigatória.");
        }

        Venda venda = new Venda();
        venda.setDataVenda(new Date());
        venda.setValorTotal(valorTotal);
        venda.setFormaPagamento(formaPagamento);
//        venda.setObservacao(observacao);
        venda.setUsuario(UserContext.getUsuarioAutenticado());

        return dao.insert(venda);
    }

    // ===================== ITEM: SAÍDA DE ESTOQUE =====================

    /**
     * Adiciona uma movimentação (SAÍDA) vinculada a uma venda.
     *
     * Regras:
     * - quantidade > 0
     * - estoque não pode ficar negativo
     * @throws Exception 
     */
    @Transactional
    private MovimentacaoEstoque adicionarMovimentacaoSaida(
            Long vendaId,
            Long produtoId,
            int quantidade,
            String observacao
    ) throws Exception {

        if (vendaId == null) throw new BusinessException("vendaId é obrigatório.");
        if (produtoId == null) throw new BusinessException("produtoId é obrigatório.");
        if (quantidade <= 0) throw new BusinessException("Quantidade deve ser maior que zero.");

        Venda venda = buscarVenda(vendaId);

        Produto produto = buscarProduto(produtoId);

        Estoque estoque = buscarEstoquePorProduto(produtoId);

        int atual = estoque.getQuantidadeAtual();
        int novo = atual - quantidade;

        if (novo < 0) {
            throw new BusinessException("Estoque insuficiente para o produto: " + produto.getNome()
                    + ". Atual: " + atual + ", solicitado: " + quantidade);
        }

        // atualiza estoque
        estoque.setQuantidadeAtual(novo);
        dao.update(estoque);

        // cria movimentação
        MovimentacaoEstoque mov = new MovimentacaoEstoque();
        mov.setDataMovimentacao(new Date());
        mov.setTipoMovimentacao(TipoMovimentacaoEstoque.SAIDA);
        mov.setQuantidade(quantidade);
        mov.setObservacao(observacao);
        mov.setEstoque(estoque);
        mov.setVenda(venda);
        mov.setUsuario(UserContext.getUsuarioAutenticado());

        return dao.insert(mov);
    }

    // ===================== LANÇAMENTO FINANCEIRO (ENTRADA) =====================

    @Transactional
    private void gerarLancamentoFinanceiro(Venda venda) {

        LancamentoFinanceiro lanc = new LancamentoFinanceiro();
        lanc.setVenda(venda);
        lanc.setTipo(TipoLancamentoFinanceiro.ENTRADA);
        lanc.setValor(venda.getValorTotal());
        lanc.setDescricao("Venda - " + venda.getFormaPagamento());
        lanc.setDataLancamento(new Date());
        lanc.setUsuario(UserContext.getUsuarioAutenticado());

        dao.insert(lanc);
    }

    // ===================== CONSULTAS =====================

    @Transactional(readOnly = true)
    public List<Venda> listarVendas() {
        return dao.select()
                .from(Venda.class)
                .join("usuario")
                .where("usuario.id", Condicao.EQUAL, UserContext.getIdUsuario())
                .orderBy("dataVenda", false)
                .list();
    }

    @Transactional(readOnly = true)
    public Venda buscarPorId(Long id) {
        return buscarVenda(id);
    }

    // ===================== HELPERS =====================

    private Venda buscarVenda(Long id) {
        try {
            return dao.select()
                    .from(Venda.class)
                    .join("usuario")
                    .where("usuario.id", Condicao.EQUAL, UserContext.getIdUsuario())
                    .id(id);
        } catch (Exception e) {
            throw new NotFoundException("Venda não encontrada: " + id);
        }
    }

    private Produto buscarProduto(Long id) {
        try {
            return dao.select()
                    .from(Produto.class)
                    .join("categoriaProduto")
                    .id(id);
        } catch (Exception e) {
            throw new NotFoundException("Produto não encontrado: " + id);
        }
    }

    private Estoque buscarEstoquePorProduto(Long produtoId) throws Exception {
        Estoque estoque = dao.select()
                .from(Estoque.class)
                .join("produto")
                .where("produto.id", Condicao.EQUAL, produtoId)
                .one();

        if (estoque == null) {
            throw new NotFoundException("Estoque não encontrado para o produto: " + produtoId);
        }
        return estoque;
    }
}

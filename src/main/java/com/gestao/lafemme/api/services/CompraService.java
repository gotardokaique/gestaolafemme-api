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
import com.gestao.lafemme.api.entity.Estoque;
import com.gestao.lafemme.api.entity.Fornecedor;
import com.gestao.lafemme.api.entity.LancamentoFinanceiro;
import com.gestao.lafemme.api.entity.MovimentacaoEstoque;
import com.gestao.lafemme.api.entity.TipoLancamentoFinanceiro;
import com.gestao.lafemme.api.entity.TipoMovimentacaoEstoque;
import com.gestao.lafemme.api.services.exceptions.BusinessException;
import com.gestao.lafemme.api.services.exceptions.NotFoundException;
import com.gestao.lafemme.api.services.exceptions.ResourceNotFoundException;

@Service
public class CompraService {

    private final DAOController dao;

    public CompraService(DAOController dao) {
        this.dao = dao;
    }

    // ===================== CRIAR COMPRA (ATÔMICA) =====================

    /**
     * Cria compra + gera lançamento financeiro (SAÍDA) + movimenta estoque (ENTRADA).
     * Tudo no mesmo @Transactional.
     *
     * Observação: este método aplica a MESMA quantidade para todos os produtoIds,
     * exatamente como você desenhou hoje.
     * @throws Exception 
     */
    @Transactional
    public void criarCompra(
            Long fornecedorId,
            BigDecimal valorTotal,
            String formaPagamento,
            Integer quantidade,
            Integer[] produtoIds,
            String observacao
    ) throws Exception {

        if (fornecedorId == null) throw new BusinessException("Fornecedor é obrigatório.");
        if (valorTotal == null || valorTotal.signum() <= 0) throw new BusinessException("Valor total da compra deve ser maior que zero.");
        if (formaPagamento == null || formaPagamento.isBlank()) throw new BusinessException("Forma de pagamento é obrigatória.");
        if (quantidade == null || quantidade <= 0) throw new BusinessException("Quantidade deve ser maior que zero.");
        if (produtoIds == null || produtoIds.length == 0) throw new BusinessException("produtoIds é obrigatório.");

        Fornecedor fornecedor = buscarFornecedor(fornecedorId);

        Compra compra = new Compra();
        compra.setFornecedor(fornecedor);
        compra.setValorTotal(valorTotal);
        compra.setFormaPagamento(formaPagamento.trim());
        compra.setDataCompra(new Date());
        compra.setUsuario(UserContext.getUsuarioAutenticado());

        // se existir no seu model:
        // compra.setObservacao(observacao);
        // compra.setAtivo(true);

        dao.insert(compra);

        gerarLancamentoFinanceiro(compra);

        adicionarMovimentacaoEntrada(compra, produtoIds, quantidade, observacao);
    }

    // ===================== EDITAR COMPRA =====================

    /**
     * Edita dados da compra (não mexe em estoque automaticamente).
     * Se você quiser editar itens/estoque, isso é OUTRA operação de domínio (estorno/ajuste).
     */
    @Transactional
    public void editarCompra(
            Long compraId,
            Long fornecedorId,
            BigDecimal valorTotal,
            String formaPagamento
    ) {

        if (compraId == null) throw new BusinessException("compraId é obrigatório.");

        Compra compra = buscarCompra(compraId);

        if (fornecedorId != null) {
            Fornecedor fornecedor = buscarFornecedor(fornecedorId);
            compra.setFornecedor(fornecedor);
        }

        if (valorTotal != null) {
            if (valorTotal.signum() <= 0) throw new BusinessException("Valor total da compra deve ser maior que zero.");
            compra.setValorTotal(valorTotal);
        }

        if (formaPagamento != null) {
            if (formaPagamento.isBlank()) throw new BusinessException("Forma de pagamento é obrigatória.");
            compra.setFormaPagamento(formaPagamento.trim());
        }

        dao.update(compra);

        // Se você editar valorTotal/formaPagamento, ideal é atualizar o lançamento também.
        // Não vou inventar seu relacionamento aqui; se existir 1–1, eu ajusto.
    }

    // ===================== ATIVAR / INATIVAR (MESMO MÉTODO) =====================

//    @Transactional
//    public void alterarStatusCompra(Long compraId, boolean ativo) {
//
//        Compra compra = buscarCompra(compraId);
//
//        // se Compra não tiver campo ativo, isso aqui não compila — aí você remove.
//        if (Boolean.valueOf(ativo).equals(compra.getAtivo())) {
//            return; // idempotente
//        }
//
//        compra.setAtivo(ativo);
//        dao.update(compra);
//    }

    // ===================== EXCLUIR FÍSICO (TRAVADO) =====================

    /**
     * Exclui compra SOMENTE se não houver lançamento/movimentação vinculados.
     * Caso exista, você deve inativar/estornar, nunca deletar.
     * @throws Exception 
     */
    @Transactional
    public void excluirCompraFisico(Long compraId) throws Exception {

        Compra compra = buscarCompra(compraId);

        // Bloqueia se houver movimentação de estoque vinculada
        try {
            dao.select()
               .from(MovimentacaoEstoque.class)
               .join("compra")
               .where("compra.id", Condicao.EQUAL, compraId)
               .one();

            throw new BusinessException("Não é permitido excluir compra com movimentações de estoque vinculadas.");
        } catch (NotFoundException not) {
            // ok
        }

        // Bloqueia se houver lançamento financeiro vinculado
        try {
            dao.select()
               .from(LancamentoFinanceiro.class)
               .join("compra")
               .where("compra.id", Condicao.EQUAL, compraId)
               .one();

            throw new BusinessException("Não é permitido excluir compra com lançamento financeiro vinculado.");
        } catch (NotFoundException not) {
            // ok
        }

        dao.delete(compra);
    }

    // ===================== LANÇAMENTO FINANCEIRO (SAÍDA) =====================

    private void gerarLancamentoFinanceiro(Compra compra) {

        LancamentoFinanceiro lanc = new LancamentoFinanceiro();
        lanc.setCompra(compra);
        lanc.setTipo(TipoLancamentoFinanceiro.SAIDA);
        lanc.setValor(compra.getValorTotal());
        lanc.setDescricao("Compra - " + compra.getFormaPagamento());
        lanc.setDataLancamento(new Date());
        lanc.setUsuario(UserContext.getUsuarioAutenticado());

        dao.insert(lanc);
    }

    // ===================== MOVIMENTAÇÃO DE ESTOQUE (ENTRADA) =====================

    private void adicionarMovimentacaoEntrada(
            Compra compra,
            Integer[] produtoIds,
            int quantidade,
            String observacao
    ) throws Exception {

        for (Integer produtoId : produtoIds) {
            if (produtoId == null) throw new BusinessException("produtoId inválido.");

            Estoque estoque = buscarEstoquePorProduto(produtoId);

            estoque.setQuantidadeAtual(estoque.getQuantidadeAtual() + quantidade);
            dao.update(estoque);

            MovimentacaoEstoque mov = new MovimentacaoEstoque();
            mov.setDataMovimentacao(new Date());
            mov.setTipoMovimentacao(TipoMovimentacaoEstoque.ENTRADA);
            mov.setQuantidade(quantidade);
            mov.setObservacao(observacao);
            mov.setEstoque(estoque);
            mov.setCompra(compra);
            mov.setUsuario(UserContext.getUsuarioAutenticado());

            dao.insert(mov);
        }
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
        return buscarCompra(id);
    }

    // ===================== HELPERS =====================

    private Compra buscarCompra(Long id) {
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

    private Estoque buscarEstoquePorProduto(Integer produtoId) throws Exception {
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

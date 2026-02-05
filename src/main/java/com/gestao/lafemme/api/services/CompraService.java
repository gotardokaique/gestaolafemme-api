package com.gestao.lafemme.api.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestao.lafemme.api.context.UserContext;
import com.gestao.lafemme.api.controllers.dto.CompraRequestDTO;
import com.gestao.lafemme.api.controllers.dto.CompraResponseDTO;
import com.gestao.lafemme.api.db.Condicao;
import com.gestao.lafemme.api.db.DAOController;
import com.gestao.lafemme.api.entity.Compra;
import com.gestao.lafemme.api.entity.Estoque;
import com.gestao.lafemme.api.entity.Fornecedor;
import com.gestao.lafemme.api.entity.LancamentoFinanceiro;
import com.gestao.lafemme.api.entity.MovimentacaoEstoque;
import com.gestao.lafemme.api.entity.Produto;
import com.gestao.lafemme.api.entity.TipoLancamentoFinanceiro;
import com.gestao.lafemme.api.entity.TipoMovimentacaoEstoque;
import com.gestao.lafemme.api.services.exceptions.BusinessException;
import com.gestao.lafemme.api.services.exceptions.NotFoundException;
import com.gestao.lafemme.api.utils.MathUtils;

@Service
public class CompraService {

    private final DAOController dao;

    public CompraService(DAOController dao) {
        this.dao = dao;
    }

    @Transactional
    public void criarCompra(CompraRequestDTO dto) throws Exception {

        if (dto.fornecedorId() == null) throw new BusinessException("Fornecedor é obrigatório.");
        if (dto.formaPagamento() == null || dto.formaPagamento().isBlank()) throw new BusinessException("Forma de pagamento é obrigatória.");
        if (dto.quantidade() == null || dto.quantidade() <= 0) throw new BusinessException("Quantidade deve ser maior que zero.");
        if (dto.produtoIds() == null || dto.produtoIds().length == 0) throw new BusinessException("Produto é obrigatório.");

        Long produtoId = Long.valueOf(dto.produtoIds()[0]);
        Fornecedor fornecedor = buscarFornecedor(dto.fornecedorId());
        BigDecimal valorTotal = calcValorTotal(produtoId, dto.quantidade());

        Compra compra = new Compra();
        compra.setFornecedor(fornecedor);
        compra.setValorTotal(valorTotal);
        compra.setFormaPagamento(dto.formaPagamento().trim());
        compra.setDataCompra(dto.dataCompra() != null ? dto.dataCompra() : new Date());
        compra.setUsuario(UserContext.getUsuario());
        compra.setUnidade(UserContext.getUnidade());

        dao.insert(compra);

        gerarLancamentoFinanceiro(compra);

        adicionarMovimentacaoEntrada(compra, produtoId, dto.quantidade(), dto.observacao());
    }

    // ===================== EDITAR COMPRA =====================

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
    }

    // ===================== EXCLUIR FÍSICO (TRAVADO) =====================

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
        lanc.setUsuario(UserContext.getUsuario());
        lanc.setUnidade(UserContext.getUnidade());

        dao.insert(lanc);
    }

    // ===================== MOVIMENTAÇÃO DE ESTOQUE (ENTRADA) =====================

    private void adicionarMovimentacaoEntrada(Compra compra, Long produtoId, int quantidade, String observacao)
            throws Exception {

        if (produtoId == null) throw new BusinessException("Produto inválido.");

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
        mov.setUsuario(UserContext.getUsuario());
        mov.setUnidade(UserContext.getUnidade());

        dao.insert(mov);
    }

    // ===================== CONSULTAS =====================

    @Transactional(readOnly = true)
    public List<CompraResponseDTO> listarCompras() {
        List<Compra> compList; 
        try {
            compList = dao.select()
            .from(Compra.class)
            .join("fornecedor")
            .join("unidade")
            .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
            .orderBy("dataCompra", false)
            .list();
        } catch (NotFoundException e) {
            compList = new ArrayList<Compra>();
        } 
        
        return CompraResponseDTO.refactor(compList);
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
                    .join("unidade")
                    .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                    .id(id);
        } catch (Exception e) {
            throw new NotFoundException("Compra não encontrada: " + id);
        }
    }

    private Fornecedor buscarFornecedor(Long fornecedorId) {
        try {
            return dao.select()
                    .from(Fornecedor.class)
                    .join("unidade")
                    .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                    .id(fornecedorId);
        } catch (Exception e) {
            throw new NotFoundException("Fornecedor não encontrado: " + fornecedorId);
        }
    }

    private Estoque buscarEstoquePorProduto(Long produtoId) throws Exception {
        try {
            return dao.select()
                    .from(Estoque.class)
                    .join("produto")
                    .where("produto.id", Condicao.EQUAL, produtoId)
                    .one();
        } catch (NotFoundException e) {
            throw new NotFoundException("Estoque não encontrado para o produto: " + produtoId);
        }
    }
    
    private BigDecimal calcValorTotal (Long produtoId, Integer qtd) {
        Produto produto;
        try {
            produto = dao.select()
                    .from(Produto.class)
                    .join("unidade")
                    .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                    .id(produtoId);
            
        } catch (NotFoundException n) {
            throw new BusinessException("Produto inválido.");
        }
        
        BigDecimal valorTotal = MathUtils.multiply(produto.getValorCusto(), qtd);
        return MathUtils.round(valorTotal, 2);
    }
}


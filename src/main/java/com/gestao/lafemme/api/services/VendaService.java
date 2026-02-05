package com.gestao.lafemme.api.services;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestao.lafemme.api.context.UserContext;
import com.gestao.lafemme.api.controllers.dto.VendaRequestDTO;
import com.gestao.lafemme.api.controllers.dto.VendaResponseDTO;
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

    @Transactional
    public void criarVenda(VendaRequestDTO dto) throws Exception {

        if (dto.produtoId() == null) throw new BusinessException("Produto é obrigatório.");
        if (dto.quantidade() == null || dto.quantidade() <= 0) throw new BusinessException("Quantidade deve ser maior que zero.");
        if (dto.formaPagamento() == null || dto.formaPagamento().isBlank()) throw new BusinessException("Forma de pagamento é obrigatória.");

        Produto produto = buscarProduto(dto.produtoId());
        
        // Se o valorTotal não vier do front, calculamos (opcional, dependendo da regra)
        BigDecimal valorTotal = dto.valorTotal();
        if (valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
            valorTotal = produto.getValorVenda().multiply(new BigDecimal(dto.quantidade()));
        }

        Venda venda = new Venda();
        venda.setDataVenda(dto.dataVenda() != null ? dto.dataVenda() : new Date());
        venda.setValorTotal(valorTotal);
        venda.setFormaPagamento(dto.formaPagamento().trim());
        venda.setUsuario(UserContext.getUsuario());
        venda.setUnidade(UserContext.getUnidade());

        dao.insert(venda);

        gerarLancamentoFinanceiro(venda);

        adicionarMovimentacaoSaida(venda, produto, dto.quantidade(), dto.observacao());
    }

    @Transactional
    private void adicionarMovimentacaoSaida(
            Venda venda,
            Produto produto,
            int quantidade,
            String observacao
    ) throws Exception {

        Estoque estoque = buscarEstoquePorProduto(produto.getId());

        int atual = estoque.getQuantidadeAtual();
        int novo = atual - quantidade;

        if (novo < 0) {
            throw new BusinessException("Estoque insuficiente para o produto: " + produto.getNome()
                    + ". Atual: " + atual + ", solicitado: " + quantidade);
        }

        estoque.setQuantidadeAtual(novo);
        dao.update(estoque);

        MovimentacaoEstoque mov = new MovimentacaoEstoque();
        mov.setDataMovimentacao(new Date());
        mov.setTipoMovimentacao(TipoMovimentacaoEstoque.SAIDA);
        mov.setQuantidade(quantidade);
        mov.setObservacao(observacao);
        mov.setEstoque(estoque);
        mov.setVenda(venda);
        mov.setProduto(produto);
        mov.setUsuario(UserContext.getUsuario());
        mov.setUnidade(UserContext.getUnidade());

        dao.insert(mov);
    }

    @Transactional
    private void gerarLancamentoFinanceiro(Venda venda) {
        LancamentoFinanceiro lanc = new LancamentoFinanceiro();
        lanc.setVenda(venda);
        lanc.setTipo(TipoLancamentoFinanceiro.ENTRADA);
        lanc.setValor(venda.getValorTotal());
        lanc.setDescricao("Venda - " + venda.getFormaPagamento());
        lanc.setDataLancamento(venda.getDataVenda());
        lanc.setUsuario(UserContext.getUsuario());
        lanc.setUnidade(UserContext.getUnidade());

        dao.insert(lanc);
    }

    @Transactional(readOnly = true)
    public List<VendaResponseDTO> listarVendas() {
        List<Venda> lista = dao.select()
                .from(Venda.class)
                .join("usuario")
                .join("unidade")
                .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                .orderBy("dataVenda", false)
                .list();
        
        return VendaResponseDTO.refactor(lista);
    }

    @Transactional(readOnly = true)
    public VendaResponseDTO buscarPorId(Long id) throws Exception {
        try {
            Venda v = dao.select()
                    .from(Venda.class)
                    .join("usuario")
                    .join("unidade")
                    .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                    .id(id);
            return VendaResponseDTO.from(v);
        } catch (Exception e) {
            throw new NotFoundException("Venda não encontrada.");
        }
    }

    private Produto buscarProduto(Long id) throws Exception {
        try {
            return dao.select()
                    .from(Produto.class)
                    .join("unidade")
                    .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                    .id(id);
        } catch (Exception e) {
            throw new NotFoundException("Produto não encontrado.");
        }
    }

    private Estoque buscarEstoquePorProduto(Long produtoId) throws Exception {
        try {
            return dao.select()
                    .from(Estoque.class)
                    .join("produto")
                    .join("unidade")
                    .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                    .where("produto.id", Condicao.EQUAL, produtoId)
                    .one();
        } catch (Exception e) {
            throw new NotFoundException("Estoque não encontrado para o produto.");
        }
    }
}

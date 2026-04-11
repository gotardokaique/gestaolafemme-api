package com.gestao.lafemme.api.services;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestao.lafemme.api.constants.SitId;
import com.gen.core.context.UserContext;
import com.gestao.lafemme.api.context.ApiUserContext;
import com.gestao.lafemme.api.controllers.dto.ConfiguracaoMP;
import com.gestao.lafemme.api.controllers.dto.GerarPagamentoResponse;
import com.gestao.lafemme.api.controllers.dto.MercadoPagoPixResponse;
import com.gestao.lafemme.api.controllers.dto.MercadoPagoPreferenceResponse;
import com.gestao.lafemme.api.controllers.dto.VendaRequestDTO;
import com.gestao.lafemme.api.controllers.dto.VendaResponseDTO;
import com.gen.core.db.Condicao;
import com.gen.core.db.DAOController;
import com.gestao.lafemme.api.entity.Configuracao;
import com.gestao.lafemme.api.entity.Estoque;
import com.gestao.lafemme.api.entity.LancamentoFinanceiro;
import com.gestao.lafemme.api.entity.MovimentacaoEstoque;
import com.gestao.lafemme.api.entity.Produto;
import com.gestao.lafemme.api.entity.Situacao;
import com.gestao.lafemme.api.entity.Venda;
import com.gestao.lafemme.api.enuns.TipoLancamentoFinanceiro;
import com.gestao.lafemme.api.enuns.TipoMovimentacaoEstoque;
import com.gestao.lafemme.api.enuns.TipoPagamentoMP;
import com.gestao.lafemme.api.services.exceptions.BusinessException;
import com.gestao.lafemme.api.services.exceptions.NotFoundException;
import com.gen.core.utils.StringEncryptUtils;

@Service
public class VendaService {

    private final DAOController dao;
    private final MercadoPagoService mercadoPagoService;
    private final ConfiguracaoService configuracaoService;
    private static final Logger log = LoggerFactory.getLogger(VendaService.class);

    public VendaService(DAOController dao, MercadoPagoService mercadoPagoService,
            ConfiguracaoService configuracaoService) {
        this.dao = dao;
        this.mercadoPagoService = mercadoPagoService;
        this.configuracaoService = configuracaoService;
    }

    @Transactional
    public void criarVenda(VendaRequestDTO dto) throws Exception {

        if (dto.produtoId() == null)
            throw new BusinessException("Produto é obrigatório.");
        if (dto.quantidade() == null || dto.quantidade() <= 0)
            throw new BusinessException("Quantidade deve ser maior que zero.");
        if (dto.formaPagamento() == null || dto.formaPagamento().isBlank())
            throw new BusinessException("Forma de pagamento é obrigatória.");

        Produto produto = buscarProduto(dto.produtoId());
        BigDecimal valorTotal = produto.getValorVenda().multiply(new BigDecimal(dto.quantidade()));

        Venda venda = new Venda();
        venda.setDataVenda(dto.dataVenda() != null ? dto.dataVenda() : new Date());
        venda.setValorTotal(valorTotal);
        venda.setFormaPagamento(dto.formaPagamento().trim());
        venda.setUsuario(ApiUserContext.getUsuario());
        venda.setUnidade(ApiUserContext.getUnidade());
        venda.setSituacao(new Situacao(SitId.PENDENTE));

        dao.insert(venda);

        adicionarMovimentacaoSaida(venda, produto, dto.quantidade(), dto.observacao());
    }

    @Transactional
    private void adicionarMovimentacaoSaida(Venda venda, Produto produto, int quantidade, String observacao)
            throws Exception {

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
        mov.setUsuario(ApiUserContext.getUsuario());
        mov.setUnidade(ApiUserContext.getUnidade());

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
        lanc.setUsuario(venda.getUsuario());
        lanc.setUnidade(venda.getUnidade());

        dao.insert(lanc);
    }

    @Transactional(readOnly = true)
    public List<VendaResponseDTO> listarVendas() {
        List<Venda> lista = dao.select()
                .from(Venda.class)
                .join("usuario")
                .join("unidade")
                .join("situacao")
                .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                .orderBy("dataVenda", false)
                .list();

        return VendaResponseDTO.refactor(lista);
    }

    @Transactional(readOnly = true)
    public VendaResponseDTO buscarPorId(Long id) throws Exception {
        try {
            Venda venda = dao.select()
                    .from(Venda.class)
                    .join("usuario")
                    .join("unidade")
                    .join("situacao")
                    .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                    .id(id);
            return VendaResponseDTO.from(venda);
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

    @Transactional
    public void concluirVenda(Long id) throws Exception {
        Venda venda = buscarEntityPorId(id);
        if (SitId.PENDENTE.equals(venda.getSituacao().getId()) == false) {
            throw new BusinessException("Apenas vendas pendentes podem ser concluídas.");
        }
        venda.setSituacao(new Situacao(SitId.CONCLUIDO));
        dao.update(venda);
        gerarLancamentoFinanceiro(venda);
    }

    @Transactional
    public void cancelarVenda(Long id) throws Exception {
        Venda venda = buscarEntityPorId(id);
        if (!SitId.PENDENTE.equals(venda.getSituacao().getId())) {
            throw new BusinessException("Apenas vendas pendentes podem ser canceladas.");
        }
        venda.setSituacao(new Situacao(SitId.CANCELADO));
        dao.update(venda);

        MovimentacaoEstoque movRef = dao.select()
                .from(MovimentacaoEstoque.class)
                .join("venda")
                .where("venda.id", Condicao.EQUAL, venda.getId())
                .one();

        if (movRef != null) {
            Produto produto = movRef.getProduto();
            Estoque estoque = buscarEstoquePorProduto(produto.getId());
            int devolvido = movRef.getQuantidade();
            estoque.setQuantidadeAtual(estoque.getQuantidadeAtual() + devolvido);
            dao.update(estoque);

            MovimentacaoEstoque compensacao = new MovimentacaoEstoque();
            compensacao.setDataMovimentacao(new Date());
            compensacao.setTipoMovimentacao(TipoMovimentacaoEstoque.ENTRADA);
            compensacao.setQuantidade(devolvido);
            compensacao.setObservacao("Cancelamento de venda #" + venda.getId());
            compensacao.setEstoque(estoque);
            compensacao.setVenda(venda);
            compensacao.setProduto(produto);
            compensacao.setUsuario(ApiUserContext.getUsuario());
            compensacao.setUnidade(ApiUserContext.getUnidade());
            dao.insert(compensacao);
        }
    }

    private Venda buscarEntityPorId(Long id) throws Exception {
        try {
            return dao.select()
                    .from(Venda.class)
                    .join("usuario")
                    .join("unidade")
                    .join("situacao")
                    .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                    .id(id);
        } catch (Exception e) {
            throw new NotFoundException("Venda não encontrada.");
        }
    }

    @Transactional
    public GerarPagamentoResponse gerarLinkPagamento(Long id) throws Exception {
        Venda venda = buscarEntityPorId(id);
        if (venda.getSituacao().getId().equals(SitId.PENDENTE) == false) {
            throw new BusinessException("Apenas vendas pendentes podem gerar link de pagamento.");
        }

        ConfiguracaoMP configMp = configuracaoService.getMercadoPagoConfig();
        if (configMp == null) {
            throw new BusinessException("Você não conectou sua conta do Mercado Pago em configurações.");
        }

        if (venda.getMpExternalReference() == null) {
            venda.setMpExternalReference(StringEncryptUtils.randomUUID());
            dao.update(venda);
        }

        if (TipoPagamentoMP.PIX.equals(configMp.tipoPagamento())) {
            MercadoPagoPixResponse pix = mercadoPagoService
                    .criarPagamentoPix(venda, configMp.accessToken());
            venda.setMpQrCode(pix.qrCode());
            venda.setMpQrCodeBase64(pix.qrCodeBase64());
            dao.update(venda);
            return new com.gestao.lafemme.api.controllers.dto.GerarPagamentoResponse("PIX", null, null, pix.qrCode(),
                    pix.qrCodeBase64());
        } else {
            MercadoPagoPreferenceResponse preference = mercadoPagoService
                    .criarPreference(venda, configMp.accessToken());
            venda.setMpPreferenceId(preference.preferenceId());
            venda.setMpPaymentLink(preference.paymentLink());
            dao.update(venda);
            return new com.gestao.lafemme.api.controllers.dto.GerarPagamentoResponse("CHECKOUT",
                    preference.paymentLink(), preference.preferenceId(), null, null);
        }
    }

    @Transactional
    public void confirmarPagamentoViaMp(String paymentId, Configuracao config) throws Exception {
        Map<String, Object> payment = mercadoPagoService.consultarPagamento(paymentId, config.getMpAccessToken());
        if (payment == null || !"approved".equals(payment.get("status"))) {
            return;
        }

        String externalRef = payment.get("external_reference").toString();

        if (externalRef == null || externalRef.length() > 255) {
            log.error("External reference inválida");
            return;
        }

        List<Venda> vendas = dao.select()
                .from(Venda.class)
                .join("situacao")
                .where("mpExternalReference", Condicao.EQUAL, externalRef)
                .list();

        if (vendas.isEmpty()) {
            return;
        }

        Venda venda = vendas.get(0);

        if (venda.getSituacao().getId().equals(SitId.PENDENTE) == false) {
            return;
        }

        venda.setSituacao(new Situacao(SitId.CONCLUIDO));
        dao.update(venda);
        gerarLancamentoFinanceiro(venda);
    }
}

package com.gestao.lafemme.api.entity;

import jakarta.persistence.*;
import java.util.Date;
import java.util.Objects;

import com.gestao.lafemme.api.enuns.TipoMovimentacaoEstoque;

@Entity
@Table(name = "movimentacao_estoque")
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mvst_id")
    private Long id;

    @Temporal(TemporalType.DATE)
    @Column(name = "mvst_data_movimentacao", nullable = false)
    private Date dataMovimentacao;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "mvst_data_cadastro", nullable = false)
    private Date dataCadastro;

    @Enumerated(EnumType.STRING)
    @Column(name = "mvst_tipo_movimentacao", nullable = false, length = 20)
    private TipoMovimentacaoEstoque tipoMovimentacao;

    @Column(name = "mvst_quantidade", nullable = false)
    private int quantidade;

    @Column(name = "mvst_observacao", length = 255)
    private String observacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estq_id", nullable = false)
    private Estoque estoque;

    // ✅ ligação direta com produto (consulta/auditoria)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prod_id", nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comp_id")
    private Compra compra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vend_id")
    private Venda venda;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usu_id", nullable = false)
    private Usuario usuario;
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uni_id", nullable = false)
    private Unidade unidade;
    
    


    public Unidade getUnidade() {
		return unidade;
	}

	public void setUnidade(Unidade unidade) {
		this.unidade = unidade;
	}

	public MovimentacaoEstoque() {}

    @PrePersist
    protected void onCreate() {
        if (this.dataMovimentacao == null) this.dataMovimentacao = new Date();
        if (this.dataCadastro == null) this.dataCadastro = new Date();
    }

    // ===================== Getters / Setters =====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Date getDataMovimentacao() { return dataMovimentacao; }
    public void setDataMovimentacao(Date dataMovimentacao) { this.dataMovimentacao = dataMovimentacao; }

    public Date getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(Date dataCadastro) { this.dataCadastro = dataCadastro; }

    public TipoMovimentacaoEstoque getTipoMovimentacao() { return tipoMovimentacao; }
    public void setTipoMovimentacao(TipoMovimentacaoEstoque tipoMovimentacao) { this.tipoMovimentacao = tipoMovimentacao; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    public Estoque getEstoque() { return estoque; }

    /**
     * Mantém consistência: ao setar estoque, tenta puxar produto.
     * Ainda assim, preferível setar produto explicitamente no service.
     */
    public void setEstoque(Estoque estoque) {
        this.estoque = estoque;
        if (estoque != null && estoque.getProduto() != null) {
            this.produto = estoque.getProduto();
        }
    }

    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }

    public Compra getCompra() { return compra; }
    public void setCompra(Compra compra) { this.compra = compra; }

    public Venda getVenda() { return venda; }
    public void setVenda(Venda venda) { this.venda = venda; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    // ===================== equals / hashCode =====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MovimentacaoEstoque other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}

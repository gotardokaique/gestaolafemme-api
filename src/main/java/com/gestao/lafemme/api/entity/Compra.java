	package com.gestao.lafemme.api.entity;
	
	import jakarta.persistence.*;
	import java.math.BigDecimal;
	import java.util.ArrayList;
	import java.util.Date;
	import java.util.List;
	import java.util.Objects;
	
	@Entity
	@Table(name = "compra")
	public class Compra {
	
	    @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    @Column(name = "comp_id")
	    private Long id;
	
	    // data efetiva da compra (regra de negócio)
	    @Temporal(TemporalType.DATE)
	    @Column(name = "comp_data_compra", nullable = false)
	    private Date dataCompra;
	
	    // auditoria / cadastro
	    @Temporal(TemporalType.TIMESTAMP)
	    @Column(name = "comp_data_cadastro", nullable = false)
	    private Date dataCadastro;
	
	    @Column(
	        name = "comp_valor_total",
	        nullable = false,
	        precision = 19,
	        scale = 2
	    )
	    private BigDecimal valorTotal;
	
	    @Column(name = "comp_forma_pagamento", nullable = false, length = 60)
	    private String formaPagamento;
	
	    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	    @JoinColumn(name = "forn_id", nullable = false)
	    private Fornecedor fornecedor;
	
	    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	    @JoinColumn(name = "usu_id", nullable = false)
	    private Usuario usuario;
	
	    @OneToMany(mappedBy = "compra", fetch = FetchType.LAZY)
	    private List<MovimentacaoEstoque> movimentacoes = new ArrayList<>();
	
	    @OneToMany(mappedBy = "compra", fetch = FetchType.LAZY)
	    private List<LancamentoFinanceiro> lancamentos = new ArrayList<>();
	    
	    @ManyToOne(fetch = FetchType.LAZY, optional = false)
	    @JoinColumn(name = "uni_id", nullable = false)
	    private Unidade unidade;
	
	
	    public Compra() {}
	
	    @PrePersist
	    protected void onCreate() {
	        if (this.dataCompra == null) {
	            this.dataCompra = new Date();
	        }
	        if (this.dataCadastro == null) {
	            this.dataCadastro = new Date();
	        }
	    }
	
	    // ===================== Getters / Setters =====================
	
	    public Long getId() {
	        return id;
	    }
	
	    // evite setId em produção; mantenho se precisar em testes
	    public void setId(Long id) {
	        this.id = id;
	    }
	    
	
	    public Unidade getUnidade() {
			return unidade;
		}

		public void setUnidade(Unidade unidade) {
			this.unidade = unidade;
		}

		public Date getDataCompra() {
	        return dataCompra;
	    }
	
	    public void setDataCompra(Date dataCompra) {
	        this.dataCompra = dataCompra;
	    }
	
	    public Date getDataCadastro() {
	        return dataCadastro;
	    }
	
	    public void setDataCadastro(Date dataCadastro) {
	        this.dataCadastro = dataCadastro;
	    }
	
	    public BigDecimal getValorTotal() {
	        return valorTotal;
	    }
	
	    public void setValorTotal(BigDecimal valorTotal) {
	        this.valorTotal = valorTotal;
	    }
	
	    public String getFormaPagamento() {
	        return formaPagamento;
	    }
	
	    public void setFormaPagamento(String formaPagamento) {
	        this.formaPagamento = formaPagamento;
	    }
	
	    public Fornecedor getFornecedor() {
	        return fornecedor;
	    }
	
	    public void setFornecedor(Fornecedor fornecedor) {
	        this.fornecedor = fornecedor;
	    }
	
	    public Usuario getUsuario() {
	        return usuario;
	    }
	
	    public void setUsuario(Usuario usuario) {
	        this.usuario = usuario;
	    }
	
	    public List<MovimentacaoEstoque> getMovimentacoes() {
	        return movimentacoes;
	    }
	
	    public void setMovimentacoes(List<MovimentacaoEstoque> movimentacoes) {
	        this.movimentacoes = movimentacoes;
	    }
	
	    public List<LancamentoFinanceiro> getLancamentos() {
	        return lancamentos;
	    }
	
	    public void setLancamentos(List<LancamentoFinanceiro> lancamentos) {
	        this.lancamentos = lancamentos;
	    }
	
	    // ===================== equals / hashCode =====================
	
	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof Compra other)) return false;
	        return id != null && Objects.equals(id, other.id);
	    }
	
	    @Override
	    public int hashCode() {
	        return 31;
	    }
	}

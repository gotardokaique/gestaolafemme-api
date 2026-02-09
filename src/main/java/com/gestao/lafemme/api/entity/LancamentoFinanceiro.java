package com.gestao.lafemme.api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

import com.gestao.lafemme.api.enuns.TipoLancamentoFinanceiro;

@Entity
@Table(name = "lancamento_financeiro")
public class LancamentoFinanceiro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lanf_id")
    private Long id;

    // data do lançamento (regra de negócio)
    @Temporal(TemporalType.DATE)
    @Column(name = "lanf_data_lancamento", nullable = false)
    private Date dataLancamento;

    // auditoria / cadastro
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "lanf_data_cadastro", nullable = false)
    private Date dataCadastro;

    @Enumerated(EnumType.STRING)
    @Column(name = "lanf_tipo", nullable = false, length = 20)
    private TipoLancamentoFinanceiro tipo;

    @Column(name = "lanf_valor", nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @Column(name = "lanf_descricao", nullable = false, length = 255)
    private String descricao;

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


    public LancamentoFinanceiro() {}

    @PrePersist
    protected void onCreate() {
        if (this.dataLancamento == null) this.dataLancamento = new Date();
        if (this.dataCadastro == null) this.dataCadastro = new Date();
    }

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

	public Date getDataLancamento() {
        return dataLancamento;
    }

    public void setDataLancamento(Date dataLancamento) {
        this.dataLancamento = dataLancamento;
    }

    public Date getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(Date dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    public TipoLancamentoFinanceiro getTipo() {
        return tipo;
    }

    public void setTipo(TipoLancamentoFinanceiro tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Compra getCompra() {
        return compra;
    }

    public void setCompra(Compra compra) {
        this.compra = compra;
    }

    public Venda getVenda() {
        return venda;
    }

    public void setVenda(Venda venda) {
        this.venda = venda;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LancamentoFinanceiro other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}

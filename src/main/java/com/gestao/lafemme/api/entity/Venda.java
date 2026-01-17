package com.gestao.lafemme.api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "vendas")
public class Venda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Temporal(TemporalType.DATE)
    @Column(name = "data_venda", nullable = false)
    private Date dataVenda;

    @Column(name = "valor_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "forma_pagamento", nullable = false, length = 60)
    private String formaPagamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @OneToMany(mappedBy = "venda", fetch = FetchType.LAZY)
    private List<MovimentacaoEstoque> movimentacoes = new ArrayList<>();

    @OneToMany(mappedBy = "venda", fetch = FetchType.LAZY)
    private List<LancamentoFinanceiro> lancamentos = new ArrayList<>();

    public Venda() {
    }

    @PrePersist
    protected void onCreate() {
        if (this.dataVenda == null) {
            this.dataVenda = new Date();
        }
    }

    public Long getId() {
        return id;
    }

    public Date getDataVenda() {
        return dataVenda;
    }

    public void setDataVenda(Date dataVenda) {
        this.dataVenda = dataVenda;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Venda other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}

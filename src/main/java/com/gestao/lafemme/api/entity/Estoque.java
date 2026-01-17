package com.gestao.lafemme.api.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "estoques")
public class Estoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quantidade_atual", nullable = false)
    private int quantidadeAtual;

    @Column(name = "estoque_minimo", nullable = false)
    private int estoqueMinimo;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false, unique = true)
    private Produto produto;

    @OneToMany(mappedBy = "estoque", fetch = FetchType.LAZY)
    private List<MovimentacaoEstoque> movimentacoes = new ArrayList<>();

    public Estoque() {}

    public Long getId() {
        return id;
    }

    public int getQuantidadeAtual() {
        return quantidadeAtual;
    }

    public void setQuantidadeAtual(int quantidadeAtual) {
        this.quantidadeAtual = quantidadeAtual;
    }

    public int getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public void setEstoqueMinimo(int estoqueMinimo) {
        this.estoqueMinimo = estoqueMinimo;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public List<MovimentacaoEstoque> getMovimentacoes() {
        return movimentacoes;
    }

    public void setMovimentacoes(List<MovimentacaoEstoque> movimentacoes) {
        this.movimentacoes = movimentacoes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Estoque other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}

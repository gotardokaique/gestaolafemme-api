package com.gestao.lafemme.api.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "estoque")
public class Estoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "estq_id")
    private Long id;

    @Column(name = "estq_quantidade_atual", nullable = false)
    private int quantidadeAtual;

    @Column(name = "estq_estoque_minimo", nullable = false)
    private int estoqueMinimo;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "estq_data_cadastro", nullable = false)
    private Date dataCadastro;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prod_id", nullable = false, unique = true)
    private Produto produto;

    @OneToMany(mappedBy = "estoque", fetch = FetchType.LAZY)
    private List<MovimentacaoEstoque> movimentacoes = new ArrayList<>();
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uni_id", nullable = false)
    private Unidade unidade;


    public Estoque() {}

    @PrePersist
    protected void onCreate() {
        if (this.dataCadastro == null) this.dataCadastro = new Date();
    }

    public Long getId() {
        return id;
    }

    // evite setId em produção; mantenho se precisar em testes
    public void setId(Long id) {
        this.id = id;
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

    public Date getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(Date dataCadastro) {
        this.dataCadastro = dataCadastro;
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

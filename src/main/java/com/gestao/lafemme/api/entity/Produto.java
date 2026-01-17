package com.gestao.lafemme.api.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "produtos")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 140)
    private String nome;

    @Column(nullable = false, unique = true, length = 60)
    private String codigo;

    @Column(length = 255)
    private String descricao;

    @Column(name = "valor_custo", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorCusto;

    @Column(name = "valor_venda", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorVenda;

    @Column(nullable = false)
    private boolean ativo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_produto_id", nullable = false)
    private CategoriaProduto categoriaProduto;

    @OneToOne(mappedBy = "produto", fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false)
    private Estoque estoque;

    public Produto() {}

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getValorCusto() {
        return valorCusto;
    }

    public void setValorCusto(BigDecimal valorCusto) {
        this.valorCusto = valorCusto;
    }

    public BigDecimal getValorVenda() {
        return valorVenda;
    }

    public void setValorVenda(BigDecimal valorVenda) {
        this.valorVenda = valorVenda;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public CategoriaProduto getCategoriaProduto() {
        return categoriaProduto;
    }

    public void setCategoriaProduto(CategoriaProduto categoriaProduto) {
        this.categoriaProduto = categoriaProduto;
    }

    public Estoque getEstoque() {
        return estoque;
    }

    /**
     * Mantém a consistência do 1-1 Produto <-> Estoque.
     */
    public void setEstoque(Estoque estoque) {
        this.estoque = estoque;
        if (estoque != null) {
            estoque.setProduto(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Produto other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}

package com.gestao.lafemme.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "situacao")
public class Situacao {
    @Id
    @Column(name = "sit_id", nullable = false, updatable = false)
    private Integer id;

    @Column(name = "sit_nome", length = 20, nullable = false)
    private String nome;

    @Column(name = "sit_descricao", length = 255, nullable = false)
    private String descricao;

    public Situacao() {}

    public Situacao(Integer id) {
        this.id = id;
    }

    public Situacao(Integer id, String nome, String descricao) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
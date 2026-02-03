package com.gestao.lafemme.api.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "perfil_usuario")
public class PerfilUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "per_id")
    private Long id;

    @Column(name = "per_nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "per_descricao", length = 255)
    private String descricao;

    @Column(name = "per_ativo", nullable = false)
    private boolean ativo;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "per_data_cadastro", nullable = false)
    private Date dataCadastro;

    @OneToMany(mappedBy = "perfilUsuario", fetch = FetchType.LAZY)
    private List<Usuario> usuarios = new ArrayList<>();
    
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uni_id", nullable = false)
    private Unidade unidade;


    public PerfilUsuario() {}

    public PerfilUsuario(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
        this.ativo = true;
    }

    @PrePersist
    protected void onCreate() {
        if (this.dataCadastro == null) this.dataCadastro = new Date();
        // boolean default é false, então garante ativo true se ninguém setar
        if (!this.ativo) this.ativo = true;
    }

    public Long getId() { return id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    public Date getDataCadastro() { return dataCadastro; }
    public void setDataCadastro(Date dataCadastro) { this.dataCadastro = dataCadastro; }

    public List<Usuario> getUsuarios() { return usuarios; }
    public void setUsuarios(List<Usuario> usuarios) { this.usuarios = usuarios; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PerfilUsuario other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}

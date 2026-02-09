package com.gestao.lafemme.api.entity;

import java.util.Date;

import com.gestao.lafemme.api.enuns.TipoAnexo;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@Table(name = "anexo", indexes = {
    @Index(name = "idx_anexo_usuario", columnList = "usu_id"),
    @Index(name = "idx_anexo_produto", columnList = "prod_id"),
    @Index(name = "idx_anexo_unidade", columnList = "uni_id"),
    @Index(name = "idx_anexo_tipo", columnList = "anex_tipo")
})
public class Anexo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "anex_id")
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "anex_data_cadastro", nullable = false)
    private Date dataCadastro;

    @Enumerated(EnumType.STRING)
    @Column(name = "anex_tipo", nullable = false, length = 40)
    private TipoAnexo tipo;

    @Column(name = "anex_nome", nullable = false, length = 160)
    private String nome;

    @Column(name = "anex_mime_type", nullable = false, length = 120)
    private String mimeType;

    @Column(name = "anex_tamanho_bytes", nullable = false)
    private Long tamanhoBytes;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "anex_arquivo", nullable = false, columnDefinition = "bytea")
    private byte[] arquivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usu_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prod_id")
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uni_id")
    private Unidade unidade;

    @PrePersist
    private void prePersist() {
        if (dataCadastro == null) dataCadastro = new Date();
        validar();
    }

    @PreUpdate
    private void preUpdate() {
        validar();
    }

    private void validar() {
        if (tipo == null) throw new IllegalStateException("tipo é obrigatório.");
        if (nome == null || nome.isBlank()) throw new IllegalStateException("nome é obrigatório.");
        if (mimeType == null || mimeType.isBlank()) throw new IllegalStateException("mimeType é obrigatório.");
        if (tamanhoBytes == null || tamanhoBytes <= 0) throw new IllegalStateException("tamanhoBytes inválido.");
        if (arquivo == null || arquivo.length == 0) throw new IllegalStateException("arquivo é obrigatório.");
        if (tamanhoBytes.longValue() != arquivo.length) throw new IllegalStateException("tamanhoBytes não bate com arquivo.length.");

        int count = 0;
        if (usuario != null) count++;
        if (produto != null) count++;
        if (unidade != null) count++;

        if (count != 1) {
            throw new IllegalStateException("Anexo deve pertencer a exatamente UM: usuario OU produto OU unidade.");
        }
    }

    public Long getId() {
        return id;
    }

    public Date getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(Date dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    public TipoAnexo getTipo() {
        return tipo;
    }

    public void setTipo(TipoAnexo tipo) {
        this.tipo = tipo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getTamanhoBytes() {
        return tamanhoBytes;
    }

    public void setTamanhoBytes(Long tamanhoBytes) {
        this.tamanhoBytes = tamanhoBytes;
    }

    public byte[] getArquivo() {
        return arquivo;
    }

    public void setArquivo(byte[] arquivo) {
        this.arquivo = arquivo;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
        if (usuario != null) {
            this.produto = null;
            this.unidade = null;
        }
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
        if (produto != null) {
            this.usuario = null;
            this.unidade = null;
        }
    }

    public Unidade getUnidade() {
        return unidade;
    }

    public void setUnidade(Unidade unidade) {
        this.unidade = unidade;
        if (unidade != null) {
            this.usuario = null;
            this.produto = null;
        }
    }
}

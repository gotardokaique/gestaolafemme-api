package com.gestao.lafemme.api.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestao.lafemme.api.db.Condicao;
import com.gestao.lafemme.api.db.DAOController;
import com.gestao.lafemme.api.entity.CategoriaProduto;
import com.gestao.lafemme.api.entity.Produto;
import com.gestao.lafemme.api.services.exceptions.BusinessException;
import com.gestao.lafemme.api.services.exceptions.NotFoundException;

@Service
public class CategoriaProdutoService {

    private final DAOController dao;

    public CategoriaProdutoService(DAOController dao) {
        this.dao = dao;
    }

    // ===================== CRIAR =====================

    @Transactional
    public CategoriaProduto criarCategoria(String nome, String descricao) throws Exception {

        validarTexto(nome, "nome");

        String nomeNormalizado = nome.trim();

        try {
            dao.select()
               .from(CategoriaProduto.class)
               .where("nome", Condicao.EQUAL, nomeNormalizado)
               .one();

            throw new BusinessException("Já existe categoria com esse nome.");
        } catch (NotFoundException not) {
            // ok
        }

        CategoriaProduto cat = new CategoriaProduto();
        cat.setNome(nomeNormalizado);
        cat.setDescricao(descricao);
        cat.setAtivo(true);

        return dao.insert(cat);
    }

    // ===================== EDITAR =====================

    @Transactional
    public CategoriaProduto editarCategoria(
            Long categoriaId,
            String nome,
            String descricao
    ) throws Exception {

        if (categoriaId == null) {
            throw new BusinessException("Campo obrigatório: categoriaId");
        }

        CategoriaProduto cat = dao
                .select()
                .from(CategoriaProduto.class)
                .id(categoriaId);

        if (cat == null) {
            throw new NotFoundException("Categoria não encontrada: " + categoriaId);
        }

        if (nome != null) {
            validarTexto(nome, "nome");
            String novoNome = nome.trim();

            if (!novoNome.equals(cat.getNome())) {
                try {
                    CategoriaProduto outra = dao.select()
                            .from(CategoriaProduto.class)
                            .where("nome", Condicao.EQUAL, novoNome)
                            .one();

                    if (outra != null && !outra.getId().equals(categoriaId)) {
                        throw new BusinessException("Já existe categoria com esse nome.");
                    }
                } catch (NotFoundException not) {
                    // ok
                }

                cat.setNome(novoNome);
            }
        }

        if (descricao != null) {
            cat.setDescricao(descricao);
        }

        return dao.update(cat);
    }

    // ===================== ATIVAR / INATIVAR (MESMO MÉTODO) =====================

    @Transactional
    public void alterarStatusCategoria(Long categoriaId, boolean ativo) throws Exception {

        CategoriaProduto cat = dao
                .select()
                .from(CategoriaProduto.class)
                .id(categoriaId);

        if (cat == null) {
            throw new NotFoundException("Categoria não encontrada: " + categoriaId);
        }

        // idempotente
        if (Boolean.valueOf(ativo).equals(cat.isAtivo())) {
            return;
        }

        cat.setAtivo(ativo);
        dao.update(cat);
    }

    // ===================== DELETE FÍSICO (SÓ SE NÃO TIVER PRODUTOS) =====================

    @Transactional
    public void excluirCategoriaFisico(Long categoriaId) throws Exception {

        CategoriaProduto cat = dao
                .select()
                .from(CategoriaProduto.class)
                .id(categoriaId);

        if (cat == null) {
            throw new NotFoundException("Categoria não encontrada: " + categoriaId);
        }

        try {
            dao.select()
               .from(Produto.class)
               .join("categoriaProduto")
               .where("categoriaProduto.id", Condicao.EQUAL, categoriaId)
               .one();

            throw new BusinessException("Não é permitido excluir categoria com produtos vinculados.");
        } catch (NotFoundException not) {
            // ok
        }

        dao.delete(cat);
    }

    // ===================== HELPERS =====================

    private void validarTexto(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new BusinessException("Campo obrigatório: " + campo);
        }
    }
}

package com.gestao.lafemme.api.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestao.lafemme.api.context.UserContext;
import com.gestao.lafemme.api.controllers.dto.CategoriaProdutoRequestDTO;
import com.gestao.lafemme.api.controllers.dto.CategoriaProdutoResponseDTO;
import com.gestao.lafemme.api.db.Condicao;
import com.gestao.lafemme.api.db.DAOController;
import com.gestao.lafemme.api.db.WhereDB;
import com.gestao.lafemme.api.dev.FilterQuery;
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
    public CategoriaProduto criarCategoria(CategoriaProdutoRequestDTO dto) throws Exception {

        validarTexto(dto.nome(), "nome");

        String nomeNormalizado = dto.nome().trim();

        try {
            dao.select()
               .from(CategoriaProduto.class)
               .join("unidade")
               .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
               .where("nome", Condicao.EQUAL, nomeNormalizado)
               .one();

            throw new BusinessException("Já existe categoria com esse nome.");
        } catch (NotFoundException not) {
            // ok
        }

        CategoriaProduto cat = new CategoriaProduto();
        cat.setNome(nomeNormalizado);
        cat.setDescricao(dto.descricao());
        cat.setAtivo(true);
        cat.setUnidade(UserContext.getUnidade());

        return dao.insert(cat);
    }

    // ===================== EDITAR =====================

    @Transactional
    public CategoriaProduto editarCategoria(
            Long categoriaId,
            CategoriaProdutoRequestDTO dto
    ) throws Exception {

        if (categoriaId == null) {
            throw new BusinessException("Campo obrigatório: categoriaId");
        }

        CategoriaProduto cat = buscarPorId(categoriaId);

        if (dto.nome() != null) {
            validarTexto(dto.nome(), "nome");
            String novoNome = dto.nome().trim();

            if (!novoNome.equals(cat.getNome())) {
                try {
                    CategoriaProduto outra = dao.select()
                            .from(CategoriaProduto.class)
                            .join("unidade")
                            .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
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

        if (dto.descricao() != null) {
            cat.setDescricao(dto.descricao());
        }

        if (dto.ativo() != null) {
            cat.setAtivo(dto.ativo());
        }

        return dao.update(cat);
    }

    // ===================== LISTAR =====================

    @Transactional(readOnly = true)
    public List<CategoriaProdutoResponseDTO> listar(Boolean ativo, FilterQuery filter) {
        List<CategoriaProduto> catList;

        WhereDB where = new WhereDB();

        if (ativo != null) {
            where.add("ativo", Condicao.EQUAL, ativo);
        }

        if (filter != null) {
            filter.applyTo(where);
        }

        try {
            catList = dao.select()
                    .from(CategoriaProduto.class)
                    .join("unidade")
                    .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                    .where(where)
                    .orderBy("nome", true)
                    .list();

        } catch (NotFoundException no) {
            catList = new ArrayList<>();
        }

        return CategoriaProdutoResponseDTO.refactor(catList);
    }

    // ===================== BUSCAR POR ID =====================

    @Transactional(readOnly = true)
    public CategoriaProduto buscarPorId(Long id) {
        try {
            return dao.select()
                    .from(CategoriaProduto.class)
                    .join("unidade")
                    .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                    .id(id);
        } catch (Exception e) {
            throw new NotFoundException("Categoria não encontrada: " + id);
        }
    }

    // ===================== ATIVAR / INATIVAR (MESMO MÉTODO) =====================

    @Transactional
    public void alterarStatusCategoria(Long categoriaId, boolean ativo) throws Exception {

        CategoriaProduto cat = buscarPorId(categoriaId);

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

        CategoriaProduto cat = buscarPorId(categoriaId);

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


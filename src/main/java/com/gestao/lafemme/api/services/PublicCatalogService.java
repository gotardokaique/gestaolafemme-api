package com.gestao.lafemme.api.services;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestao.lafemme.api.controllers.dto.PublicProdutoDTO;
import com.gestao.lafemme.api.controllers.dto.PublicCategoriaDTO;
import com.gestao.lafemme.api.db.Condicao;
import com.gestao.lafemme.api.db.DAOController;
import com.gestao.lafemme.api.entity.CategoriaProduto;
import com.gestao.lafemme.api.entity.Configuracao;
import com.gestao.lafemme.api.entity.Produto;
import com.gestao.lafemme.api.entity.UsuarioUnidade;

import jakarta.persistence.EntityNotFoundException;

@Service
public class PublicCatalogService {

    private final DAOController dao;
    private final ConfiguracaoService configuracaoService;

    public PublicCatalogService(DAOController dao, ConfiguracaoService configuracaoService) {
        this.dao = dao;
        this.configuracaoService = configuracaoService;
    }

    private List<Long> getUnidadesIds(String token) {
        Configuracao config = configuracaoService.buscarPorToken(token);
        if (config == null) {
            throw new IllegalArgumentException("Token inválido ou não encontrado");
        }

        List<UsuarioUnidade> usuUnis = dao.select()
                .from(UsuarioUnidade.class)
                .where("usuario.id", Condicao.EQUAL, config.getUsuario().getId())
                .list();

        return usuUnis.stream()
                .map(uu -> uu.getUnidade().getId())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PublicProdutoDTO> listarProdutos(String token) {
        List<Long> uniIds = getUnidadesIds(token);
        if (uniIds.isEmpty())
            return List.of();

        List<Produto> produtos = dao.select()
                .from(Produto.class)
                .where("unidade.id", Condicao.IN, uniIds)
                .where("ativo", Condicao.EQUAL, true)
                .where("estoque.quantidadeAtual", Condicao.GREATER_THAN, 0)
                .list();

        return produtos.stream()
                .map(PublicProdutoDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PublicProdutoDTO buscarProduto(String token, Long id) throws Exception {
        List<Long> uniIds = getUnidadesIds(token);
        if (uniIds.isEmpty())
            return null;

        try {
            Produto p = dao.select()
                    .from(Produto.class)
                    .join("estoque")
                    .where("id", Condicao.EQUAL, id)
                    .where("unidade.id", Condicao.IN, uniIds)
                    .where("ativo", Condicao.EQUAL, true)
                    .where("estoque.quantidadeAtual", Condicao.GREATER_THAN, 0)
                    .one();

            return PublicProdutoDTO.fromEntity(p);
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public List<PublicCategoriaDTO> listarCategorias(String token) {
        List<Long> uniIds = getUnidadesIds(token);
        if (uniIds.isEmpty())
            return List.of();

        List<CategoriaProduto> categorias = dao.select()
                .from(CategoriaProduto.class)
                .where("unidade.id", Condicao.IN, uniIds)
                .where("ativo", Condicao.EQUAL, true)
                .list();

        return categorias.stream()
                .map(PublicCategoriaDTO::fromEntity)
                .collect(Collectors.toList());
    }
}

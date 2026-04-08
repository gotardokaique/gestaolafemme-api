package com.gestao.lafemme.api.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestao.lafemme.api.controllers.dto.AnexoFotoDTO;
import com.gestao.lafemme.api.controllers.dto.PublicCategoriaDTO;
import com.gestao.lafemme.api.controllers.dto.PublicProdutoDTO;
import com.gestao.lafemme.api.db.Condicao;
import com.gestao.lafemme.api.db.DAOController;
import com.gestao.lafemme.api.entity.Anexo;
import com.gestao.lafemme.api.entity.CategoriaProduto;
import com.gestao.lafemme.api.entity.Configuracao;
import com.gestao.lafemme.api.entity.Produto;
import com.gestao.lafemme.api.entity.UsuarioUnidade;
import com.gestao.lafemme.api.enuns.TipoAnexo;
import com.gestao.lafemme.api.services.exceptions.NotFoundException;

@Service
public class PublicCatalogService {

    private final DAOController dao;
    private final ConfiguracaoService configuracaoService;

    public PublicCatalogService(DAOController dao, ConfiguracaoService configuracaoService) {
        this.dao = dao;
        this.configuracaoService = configuracaoService;
    }

    // ── Infraestrutura privada ──────────────────────────────────────────────────

    private List<Long> resolverUnidadesDoToken(String token) {
        Configuracao config = configuracaoService.buscarPorToken(token);
        if (config == null) {
            throw new IllegalArgumentException("Token inválido ou não encontrado.");
        }

        List<UsuarioUnidade> usuUnis = new ArrayList<>();
        try {
            usuUnis = dao.select()
                    .from(UsuarioUnidade.class)
                    .where("usuario.id", Condicao.EQUAL, config.getUsuario().getId())
                    .list();
        } catch (NotFoundException e) {
            // sem unidades vinculadas — retorna lista vazia
        }

        return usuUnis.stream()
                .map(uu -> uu.getUnidade().getId())
                .collect(Collectors.toList());
    }

    private Set<Long> buscarIdsProdutosComFoto(List<Long> produtoIds) {
        if (produtoIds.isEmpty())
            return Set.of();

        List<Anexo> anexos = new ArrayList<>();
        try {
            anexos = dao.select()
                    .from(Anexo.class)
                    .where("produto.id", Condicao.IN, produtoIds)
                    .where("tipo", Condicao.IN, List.of(TipoAnexo.FOTO_PRODUTO, TipoAnexo.FOTO_CATALOGO_PRODUTO))
                    .list();
        } catch (NotFoundException e) {
            // nenhum anexo encontrado
        }

        return anexos.stream()
                .map(a -> a.getProduto().getId())
                .collect(Collectors.toSet());
    }

    private List<PublicProdutoDTO> mapearProdutos(List<Produto> produtos) {
        if (produtos.isEmpty())
            return List.of();

        List<Long> ids = produtos.stream().map(Produto::getId).collect(Collectors.toList());
        Set<Long> comFoto = buscarIdsProdutosComFoto(ids);

        return produtos.stream()
                .map(p -> PublicProdutoDTO.fromEntity(p, comFoto.contains(p.getId())))
                .collect(Collectors.toList());
    }

    // ── Operações públicas ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PublicProdutoDTO> listarProdutos(String token) {
        List<Long> uniIds = resolverUnidadesDoToken(token);
        if (uniIds.isEmpty())
            return List.of();

        List<Produto> produtos = new ArrayList<>();
        try {
            produtos = dao.select()
                    .from(Produto.class)
                    .where("unidade.id", Condicao.IN, uniIds)
                    .where("ativo", Condicao.EQUAL, true)
                    .where("estoque.quantidadeAtual", Condicao.GREATER_THAN, 0)
                    .list();
        } catch (NotFoundException e) {
            // nenhum produto encontrado — retorna vazio
        }

        return mapearProdutos(produtos);
    }

    @Transactional(readOnly = true)
    public PublicProdutoDTO buscarProduto(String token, Long id) throws Exception {
        List<Long> uniIds = resolverUnidadesDoToken(token);
        if (uniIds.isEmpty())
            return null;

        Produto produto = new Produto();
        try {
            produto = dao.select()
                    .from(Produto.class)
                    .where("id", Condicao.EQUAL, id)
                    .where("unidade.id", Condicao.IN, uniIds)
                    .where("ativo", Condicao.EQUAL, true)
                    .where("estoque.quantidadeAtual", Condicao.GREATER_THAN, 0)
                    .one();
        } catch (NotFoundException e) {
            return null;
        }

        boolean hasFoto = !buscarIdsProdutosComFoto(List.of(produto.getId())).isEmpty();
        return PublicProdutoDTO.fromEntity(produto, hasFoto);
    }

    @Transactional(readOnly = true)
    public AnexoFotoDTO buscarFoto(String token, Long produtoId) {
        List<Long> uniIds = resolverUnidadesDoToken(token);
        if (uniIds.isEmpty())
            return null;

        List<Anexo> anexos = new ArrayList<>();
        try {
            anexos = dao.select()
                    .from(Anexo.class)
                    .where("produto.id", Condicao.EQUAL, produtoId)
                    .where("tipo", Condicao.IN, List.of(TipoAnexo.FOTO_PRODUTO, TipoAnexo.FOTO_CATALOGO_PRODUTO))
                    .where("produto.unidade.id", Condicao.IN, uniIds)
                    .list();
        } catch (NotFoundException e) {
            return null;
        }

        if (anexos.isEmpty())
            return null;

        Anexo foto = anexos.stream()
                .filter(a -> a.getTipo() == TipoAnexo.FOTO_PRODUTO)
                .findFirst()
                .orElse(anexos.get(0));

        return new AnexoFotoDTO(foto.getArquivo(), foto.getMimeType());
    }

    @Transactional(readOnly = true)
    public List<PublicCategoriaDTO> listarCategorias(String token) {
        List<Long> uniIds = resolverUnidadesDoToken(token);
        if (uniIds.isEmpty())
            return List.of();

        List<CategoriaProduto> categorias = new ArrayList<>();
        try {
            categorias = dao.select()
                    .from(CategoriaProduto.class)
                    .where("unidade.id", Condicao.IN, uniIds)
                    .where("ativo", Condicao.EQUAL, true)
                    .list();
        } catch (NotFoundException e) {
            // nenhuma categoria encontrada
        }

        return PublicCategoriaDTO.fromEntity(categorias);
    }
}
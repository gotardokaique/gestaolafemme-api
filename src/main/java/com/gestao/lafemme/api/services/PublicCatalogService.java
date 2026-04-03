package com.gestao.lafemme.api.services;

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

    /**
     * Uma query batch para saber quais produtos têm FOTO_PRODUTO.
     * Evita N+1: um único SELECT no lugar de um por produto.
     */
    private Set<Long> produtosComFoto(List<Long> produtoIds) {
        if (produtoIds.isEmpty()) return Set.of();
        List<Anexo> anexos = dao.select()
                .from(Anexo.class)
                .where("produto.id", Condicao.IN, produtoIds)
                .where("tipo", Condicao.IN, List.of(TipoAnexo.FOTO_PRODUTO, TipoAnexo.FOTO_CATALOGO_PRODUTO))
                .list();
        return anexos.stream()
                .map(a -> a.getProduto().getId())
                .collect(Collectors.toSet());
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

        List<Long> ids = produtos.stream().map(Produto::getId).collect(Collectors.toList());
        Set<Long> comFoto = produtosComFoto(ids);

        return produtos.stream()
                .map(p -> PublicProdutoDTO.fromEntity(p, comFoto.contains(p.getId())))
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

            boolean hasFoto = !produtosComFoto(List.of(p.getId())).isEmpty();
            return PublicProdutoDTO.fromEntity(p, hasFoto);
        } catch (EntityNotFoundException e) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public AnexoFotoDTO buscarFoto(String token, Long produtoId) {
        List<Long> uniIds = getUnidadesIds(token);
        if (uniIds.isEmpty())
            return null;

        // Prioridade: FOTO_PRODUTO primeiro, depois FOTO_CATALOGO_PRODUTO
        List<Anexo> anexos = dao.select()
                .from(Anexo.class)
                .where("produto.id", Condicao.EQUAL, produtoId)
                .where("tipo", Condicao.IN, List.of(TipoAnexo.FOTO_PRODUTO, TipoAnexo.FOTO_CATALOGO_PRODUTO))
                .where("produto.unidade.id", Condicao.IN, uniIds)
                .list();

        if (anexos.isEmpty())
            return null;

        // Prefere FOTO_PRODUTO; se não tiver, usa a primeira FOTO_CATALOGO_PRODUTO
        Anexo foto = anexos.stream()
                .filter(a -> a.getTipo() == TipoAnexo.FOTO_PRODUTO)
                .findFirst()
                .orElse(anexos.get(0));

        return new AnexoFotoDTO(foto.getArquivo(), foto.getMimeType());
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

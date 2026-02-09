package com.gestao.lafemme.api.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestao.lafemme.api.context.UserContext;
import com.gestao.lafemme.api.controllers.dto.FotoDTO;
import com.gestao.lafemme.api.controllers.dto.ProdutoRequestDTO;
import com.gestao.lafemme.api.controllers.dto.ProdutoResponseDTO;
import com.gestao.lafemme.api.db.Condicao;
import com.gestao.lafemme.api.db.DAOController;
import com.gestao.lafemme.api.db.WhereDB;
import com.gestao.lafemme.api.entity.Anexo;
import com.gestao.lafemme.api.entity.CategoriaProduto;
import com.gestao.lafemme.api.entity.Estoque;
import com.gestao.lafemme.api.entity.MovimentacaoEstoque;
import com.gestao.lafemme.api.entity.Produto;
import com.gestao.lafemme.api.enuns.TipoAnexo;
import com.gestao.lafemme.api.enuns.TipoMovimentacaoEstoque;
import com.gestao.lafemme.api.services.exceptions.BusinessException;
import com.gestao.lafemme.api.services.exceptions.NotFoundException;

@Service
public class ProdutoService {

    private final DAOController dao;

    public ProdutoService(DAOController dao) {
        this.dao = dao;
    }

    @Transactional(readOnly = true)
    public List<ProdutoResponseDTO> listarProdutos(Boolean ativos) {
        List<Produto> listProd;
        
        WhereDB where = new WhereDB();
        where.add("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade());
        if (ativos != null) {
            where.add("ativo", Condicao.EQUAL, ativos);
        }

        try {
            listProd = dao.select()
                    .from(Produto.class)
                    .join("categoriaProduto")
                    .join("estoque")
                    .join("unidade")
                    .where(where)
                    .orderBy("nome", true)
                    .list();
            
        } catch (NotFoundException not) {
            listProd = new ArrayList<>();
        }

        return ProdutoResponseDTO.refactor(listProd);
    }

    @Transactional(readOnly = true)
    public ProdutoResponseDTO buscarPorId(Long id) throws Exception {
        try {
            Produto prod = dao.select()
                    .from(Produto.class)
                    .join("categoriaProduto")
                    .join("estoque")
                    .join("unidade")
                    .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                    .id(id);
            return ProdutoResponseDTO.refactor(prod);
            
        } catch (NotFoundException e) {
            throw new NotFoundException("Produto não encontrado: " + id);
        }
    }

    @Transactional
    public Produto criarProduto(ProdutoRequestDTO dto) throws Exception {

        validarTexto(dto.nome(), "nome");
        validarTexto(dto.codigo(), "codigo");

        if (dto.categoriaId() == null) {
            throw new BusinessException("Campo obrigatório: categoriaId");
        }

        CategoriaProduto categoria = dao
                .select()
                .from(CategoriaProduto.class)
                .join("unidade")
                .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                .id(dto.categoriaId());

        try {
            dao.select()
                .from(Produto.class)
                .join("unidade")
                .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                .where("codigo", Condicao.EQUAL, dto.codigo().trim())
                .one();

            throw new BusinessException("Já existe produto com esse código nesta unidade.");
        } catch (NotFoundException not) {
            // ok
        }

        Produto produto = new Produto();
        produto.setNome(dto.nome().trim());
        produto.setCodigo(dto.codigo().trim());
        produto.setDescricao(dto.descricao());
        produto.setValorCusto(dto.valorCusto() != null ? dto.valorCusto() : BigDecimal.ZERO);
        produto.setValorVenda(dto.valorVenda() != null ? dto.valorVenda() : BigDecimal.ZERO);
        produto.setCategoriaProduto(categoria);
        produto.setAtivo(true);
        produto.setUnidade(UserContext.getUnidade());

        Produto salvo = dao.insert(produto);
        
        Estoque estoque = new Estoque();
        estoque.setProduto(salvo);
        estoque.setQuantidadeAtual(dto.quantidadeInicial() != null ? dto.quantidadeInicial() : 0);
        estoque.setEstoqueMinimo(dto.estoqueMinimo() != null ? Math.max(dto.estoqueMinimo(), 0) : 0);
        estoque.setUnidade(UserContext.getUnidade());

        dao.insert(estoque);

        // Salvar foto do produto, se informada
        if (dto.foto() != null) {
            salvarFotoProduto(salvo, dto.foto());
        }

        return salvo;
    }

    @Transactional
    public Produto editarProduto(Long produtoId, ProdutoRequestDTO dto) throws Exception {
        
        Produto produto = carregarProdutoEntidade(produtoId);

        if (dto.nome() != null) {
            validarTexto(dto.nome(), "nome");
            produto.setNome(dto.nome().trim());
        }

        if (dto.codigo() != null) {
            validarTexto(dto.codigo(), "codigo");
            String novoCodigo = dto.codigo().trim();

            if (!novoCodigo.equals(produto.getCodigo())) {
                try {
                    dao.select()
                            .from(Produto.class)
                            .join("unidade")
                            .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                            .where("codigo", Condicao.EQUAL, novoCodigo)
                            .one();

                     throw new BusinessException("Já existe produto com esse código nesta unidade.");
                } catch (NotFoundException not) {
                    // ok
                }
                produto.setCodigo(novoCodigo);
            }
        }

        if (dto.descricao() != null) produto.setDescricao(dto.descricao());
        if (dto.valorCusto() != null) produto.setValorCusto(dto.valorCusto());
        if (dto.valorVenda() != null) produto.setValorVenda(dto.valorVenda());

        if (dto.categoriaId() != null) {
            CategoriaProduto categoria = dao
                    .select()
                    .from(CategoriaProduto.class)
                    .join("unidade")
                    .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                    .id(dto.categoriaId());
            
            produto.setCategoriaProduto(categoria);
        }

        if (dto.ativo() != null) {
            produto.setAtivo(dto.ativo());
        }

        if (dto.estoqueMinimo() != null) {
            produto.getEstoque().setEstoqueMinimo(Math.max(dto.estoqueMinimo(), 0));
        }

        // Atualizar foto do produto, se informada
        if (dto.foto() != null) {
            salvarFotoProduto(produto, dto.foto());
        }

        return dao.update(produto);
    }

    @Transactional
    public void ativarInativarProduto(Long produtoId) throws Exception {
    	
        Produto produto = carregarProdutoEntidade(produtoId);
        produto.setAtivo(!produto.isAtivo());
        dao.update(produto);
    }


    @Transactional
    public void excluirProdutoFisico(Long produtoId) throws Exception {

        Produto produto = carregarProdutoEntidade(produtoId);

        try {
            dao.select()
               .from(MovimentacaoEstoque.class)
               .join("estoque")
               .where("estoque.produto.id", Condicao.EQUAL, produtoId)
               .one();

            throw new BusinessException("Não é permitido excluir produto com movimentações registradas.");
        } catch (NotFoundException not) {
            // ok
        }

        // remove estoque (CascadeType.ALL no Produto cuidaria disso, mas vamos garantir se necessário)
        // Produto.java tem @OneToOne(mappedBy = "produto", cascade = CascadeType.ALL)
        dao.delete(produto);
    }

    @Transactional
    public void ajustarEstoque(
            Long produtoId,
            int novaQuantidade,
            String observacao
    ) throws Exception {

        if (novaQuantidade < 0) {
            throw new BusinessException("Quantidade não pode ser negativa.");
        }

        Produto produto = carregarProdutoEntidade(produtoId);
        Estoque estoque = produto.getEstoque();

        int quantidadeAnterior = estoque.getQuantidadeAtual();
        int diferenca = novaQuantidade - quantidadeAnterior;

        if (diferenca == 0) return;

        estoque.setQuantidadeAtual(novaQuantidade);
        dao.update(estoque);

        MovimentacaoEstoque mov = new MovimentacaoEstoque();
        mov.setDataMovimentacao(new Date());
        mov.setTipoMovimentacao(TipoMovimentacaoEstoque.AJUSTE);
        mov.setQuantidade(Math.abs(diferenca));
        mov.setObservacao(observacao);
        mov.setEstoque(estoque);
        mov.setProduto(produto);
        mov.setUsuario(UserContext.getUsuario());
        mov.setUnidade(UserContext.getUnidade());

        dao.insert(mov);
    }

    private void validarTexto(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new BusinessException("Campo obrigatório: " + campo);
        }
    }
    
    private Produto carregarProdutoEntidade(Long id) throws Exception {
        try {
            return dao.select()
                    .from(Produto.class)
                    .join("unidade")
                    .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                    .id(id);
        } catch (NotFoundException e) {
            throw new NotFoundException("Produto não encontrado.");
        }
    }

    /**
     * Salva ou atualiza a foto do produto como Anexo do tipo FOTO_PRODUTO.
     * Remove a foto anterior, se existir, garantindo apenas uma foto por produto.
     */
    private void salvarFotoProduto(Produto produto, FotoDTO fotoDTO) {
        if (fotoDTO.arquivo() == null || fotoDTO.arquivo().isBlank()) {
            return;
        }

        byte[] arquivoBytes = Base64.getDecoder().decode(fotoDTO.arquivo());

        // Remover foto anterior, se existir
        try {
            Anexo fotoAnterior = dao.select()
                    .from(Anexo.class)
                    .where("produto.id", Condicao.EQUAL, produto.getId())
                    .where("tipo", Condicao.EQUAL, TipoAnexo.FOTO_PRODUTO)
                    .one();
            dao.delete(fotoAnterior);
        } catch (NotFoundException not) {
            // Não havia foto anterior
        } catch (Exception e) {
            throw new BusinessException("Erro ao remover foto anterior: " + e.getMessage());
        }

        // Criar novo anexo
        Anexo anexo = new Anexo();
        anexo.setTipo(TipoAnexo.FOTO_PRODUTO);
        anexo.setNome(fotoDTO.nome() != null ? fotoDTO.nome() : "foto_produto");
        anexo.setMimeType(fotoDTO.mimeType() != null ? fotoDTO.mimeType() : "image/jpeg");
        anexo.setArquivo(arquivoBytes);
        anexo.setTamanhoBytes((long) arquivoBytes.length);
        anexo.setProduto(produto);

        dao.insert(anexo);
    }

    // ============ Métodos para Catálogo de Fotos ============

    /**
     * Adiciona uma foto ao catálogo do produto.
     * 
     * @param produtoId ID do produto
     * @param nome      Nome/descrição da foto (ex: "COR: VERMELHA")
     * @param mimeType  Tipo MIME do arquivo
     * @param arquivo   Conteúdo do arquivo em Base64
     * @return ID do anexo criado
     */
    @Transactional
    public Long adicionarFotoCatalogo(Long produtoId, String nome, String mimeType, String arquivo) throws Exception {
        Produto produto = carregarProdutoEntidade(produtoId);

        if (arquivo == null || arquivo.isBlank()) {
            throw new BusinessException("Arquivo é obrigatório.");
        }

        byte[] arquivoBytes = Base64.getDecoder().decode(arquivo);

        Anexo anexo = new Anexo();
        anexo.setTipo(TipoAnexo.FOTO_CATALOGO_PRODUTO);
        anexo.setNome(nome != null && !nome.isBlank() ? nome : "Foto catálogo");
        anexo.setMimeType(mimeType != null ? mimeType : "image/jpeg");
        anexo.setArquivo(arquivoBytes);
        anexo.setTamanhoBytes((long) arquivoBytes.length);
        anexo.setProduto(produto);

        dao.insert(anexo);

        return anexo.getId();
    }

    /**
     * Remove uma foto do catálogo do produto.
     * 
     * @param produtoId ID do produto
     * @param anexoId   ID do anexo a ser removido
     */
    @Transactional
    public void removerFotoCatalogo(Long produtoId, Long anexoId) throws Exception {
        // Validar que o produto existe e pertence à unidade
        carregarProdutoEntidade(produtoId);

        Anexo anexo;
        try {
            anexo = dao.select()
                    .from(Anexo.class)
                    .join("produto")
                    .join("produto.unidade")
                    .where("id", Condicao.EQUAL, anexoId)
                    .where("produto.id", Condicao.EQUAL, produtoId)
                    .where("produto.unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                    .where("tipo", Condicao.EQUAL, TipoAnexo.FOTO_CATALOGO_PRODUTO)
                    .one();
        } catch (NotFoundException e) {
            throw new NotFoundException("Foto não encontrada no catálogo.");
        }

        dao.delete(anexo);
    }
}

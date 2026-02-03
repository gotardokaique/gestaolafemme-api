package com.gestao.lafemme.api.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestao.lafemme.api.context.UserContext;
import com.gestao.lafemme.api.controllers.dto.ProdutoRequestDTO;
import com.gestao.lafemme.api.controllers.dto.ProdutoResponseDTO;
import com.gestao.lafemme.api.db.Condicao;
import com.gestao.lafemme.api.db.DAOController;
import com.gestao.lafemme.api.entity.CategoriaProduto;
import com.gestao.lafemme.api.entity.Estoque;
import com.gestao.lafemme.api.entity.MovimentacaoEstoque;
import com.gestao.lafemme.api.entity.Produto;
import com.gestao.lafemme.api.entity.TipoMovimentacaoEstoque;
import com.gestao.lafemme.api.entity.Usuario;
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
    	
    	try {
    		listProd = dao.select()
    				.from(Produto.class)
    				.join("categoriaProduto")
    				.join("estoque")
    				.orderBy("nome", true)
    				.list();
    		
    	} catch (NotFoundException not) {
    		listProd = new ArrayList<Produto>();
    	}

        return ProdutoResponseDTO.refactor(listProd);
    }

    @Transactional(readOnly = true)
    public ProdutoResponseDTO buscarPorId(Long id) throws Exception {
    	Produto prod;
    	
        try {
        	prod = dao.select()
                    .from(Produto.class)
                    .join("categoriaProduto")
                    .join("estoque")
                    .id(id);
        	return ProdutoResponseDTO.refactor(prod);
        	
        } catch (NotFoundException e) {
            throw new NotFoundException("Produto não encontrado: " + id);
        }
    }

    // ===================== CRIAR PRODUTO =====================

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
                .id(dto.categoriaId());

        try {
            dao
                .select()
                .from(Produto.class)
                .where("codigo", Condicao.EQUAL, dto.codigo().trim())
                .one();

            throw new BusinessException("Já existe produto com esse código.");

        } catch (NotFoundException not) {
            // ok
        }

        Produto produto = new Produto();
        produto.setNome(dto.nome().trim());
        produto.setCodigo(dto.codigo().trim());
        produto.setDescricao(dto.descricao());
        produto.setValorCusto(BigDecimal.ZERO);
        produto.setValorVenda(BigDecimal.ZERO);
        produto.setCategoriaProduto(categoria);
        produto.setAtivo(true);

        Produto salvo = dao.insert(produto);

        // cria estoque 1–1
        Estoque estoque = new Estoque();
        estoque.setProduto(salvo);
        estoque.setQuantidadeAtual(dto.quantidadeId());
        estoque.setEstoqueMinimo(Math.max(dto.estoqueMinimo(), 0));

        dao.insert(estoque);

        return salvo;
    }

    // ===================== EDITAR PRODUTO =====================

    @Transactional
    public Produto editarProduto(Integer produtoId, ProdutoRequestDTO dto) throws Exception {
        
        Produto produto = carregarProduto(produtoId);

        if (dto.nome() != null) {
            validarTexto(dto.nome(), "nome");
            produto.setNome(dto.nome().trim());
        }

        if (dto.codigo() != null) {
            validarTexto(dto.codigo(), "codigo");
            String novoCodigo = dto.codigo().trim();

            if (novoCodigo.equals(produto.getCodigo()) == false) {
                try {
                    Produto outro = dao
                            .select()
                            .from(Produto.class)
                            .where("codigo", Condicao.EQUAL, novoCodigo)
                            .one();

                    if (outro != null ) {
                        throw new BusinessException("Já existe produto com esse código.");
                    }
                } catch (NotFoundException not) {
                    // ok
                }
                produto.setCodigo(novoCodigo);
            }
        }

        produto.setDescricao(dto.descricao());
        

        if (dto.categoriaId() != null) {
            CategoriaProduto categoria = dao
                    .select()
                    .from(CategoriaProduto.class)
                    .id(dto.categoriaId());
            
            produto.setCategoriaProduto(categoria);
        }

        if (dto.ativo() != null) {
            produto.setAtivo(dto.ativo());
        }

        return dao.update(produto);
    }

    // ===================== APAGAR (DESATIVAR) PRODUTO =====================

    @Transactional
    public void ativarInativarProduto(Integer produtoId, String motivo) throws Exception {
    	
        Produto produto = carregarProduto(produtoId);


        if (produto.isAtivo() == false) {
        	produto.setAtivo(true);
        } else {
        	produto.setAtivo(false);
        }

        dao.update(produto);

        // auditoria opcional: registra AJUSTE 0 só pra deixar rastro
        registrarAuditoria(produtoId, motivo != null ? motivo : "Produto desativado.");
    }


    @Transactional
    public void excluirProdutoFisico(Long produtoId) throws Exception {

        Produto produto = dao
                .select()
                .from(Produto.class)
                .id(produtoId);

        if (produto == null) {
            throw new NotFoundException("Produto não encontrado: " + produtoId);
        }

        // bloqueia se houver movimentações (compra/venda também geram movimentação)
        try {
            dao.select()
               .from(MovimentacaoEstoque.class)
               .join("estoque")
               .where("estoque.produto.id", Condicao.EQUAL, produtoId)
               .one();

            throw new BusinessException("Não é permitido excluir produto com movimentações registradas.");
        } catch (NotFoundException not) {
            // ok: nenhuma movimentação
        }

        // remove estoque 1–1
        try {
            Estoque estoque = dao
                    .select()
                    .from(Estoque.class)
                    .join("produto")
                    .where("produto.id", Condicao.EQUAL, produtoId)
                    .one();

            if (estoque != null) {
                dao.delete(estoque);
            }
        } catch (NotFoundException not) {
            // ok
        }

        // remove produto
        dao.delete(produto);
    }

    // ===================== AJUSTE INICIAL DE ESTOQUE =====================

    @Transactional
    public void ajustarEstoqueInicial(
            Long produtoId,
            int novaQuantidade,
            String observacao
    ) throws Exception {

        if (novaQuantidade < 0) {
            throw new BusinessException("Quantidade não pode ser negativa.");
        }

        Produto produto = dao
                .select()
                .from(Produto.class)
                .id(produtoId);

        if (produto == null) {
            throw new NotFoundException("Produto não encontrado: " + produtoId);
        }

        Estoque estoque = dao
                .select()
                .from(Estoque.class)
                .join("produto")
                .where("produto.id", Condicao.EQUAL, produtoId)
                .one();

        int quantidadeAnterior = estoque.getQuantidadeAtual();

        estoque.setQuantidadeAtual(novaQuantidade);
        dao.update(estoque);

        MovimentacaoEstoque mov = new MovimentacaoEstoque();
        mov.setDataMovimentacao(new Date());
        mov.setTipoMovimentacao(TipoMovimentacaoEstoque.AJUSTE);
        mov.setQuantidade(Math.abs(novaQuantidade - quantidadeAnterior));
        mov.setObservacao(observacao);
        mov.setEstoque(estoque);
        mov.setProduto(produto);

        Usuario usuarioRef = new Usuario();
        usuarioRef.setId(UserContext.getIdUsuario());
        mov.setUsuario(usuarioRef);

        dao.insert(mov);
    }

    // ===================== AUDITORIA HELPERS =====================

    private void registrarAuditoria(Integer produtoId, String observacao) {
        try {
            Estoque estoque = dao
                    .select()
                    .from(Estoque.class)
                    .join("produto")
                    .where("produto.id", Condicao.EQUAL, produtoId)
                    .one();

            MovimentacaoEstoque mov = new MovimentacaoEstoque();
            mov.setDataMovimentacao(new Date());
            mov.setTipoMovimentacao(TipoMovimentacaoEstoque.AJUSTE);
            mov.setQuantidade(0);
            mov.setObservacao(observacao);
            mov.setEstoque(estoque);

            Usuario usuarioRef = new Usuario();
            usuarioRef.setId(UserContext.getIdUsuario());
            mov.setUsuario(usuarioRef);

            dao.insert(mov);
        } catch (Exception e) {
            // auditoria não pode quebrar regra de negócio
        }
    }

    // ===================== HELPERS =====================

    private void validarTexto(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new BusinessException("Campo obrigatório: " + campo);
        }
    }
    
    
    private Produto carregarProduto(Integer id) throws Exception {
    	   Produto produto = dao
                   .select()
                   .from(Produto.class)
                   .id(id);
    	   
    	   return produto;
    }
}

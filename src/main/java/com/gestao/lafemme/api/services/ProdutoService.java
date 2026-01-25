package com.gestao.lafemme.api.services;

import java.util.Date;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestao.lafemme.api.context.UserContext;
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

    // ===================== CRIAR PRODUTO =====================

    @Transactional
    public Produto criarProduto(String nome, String codigo, String descricao, Long categoriaId, int estoqueMinimo) {

        validarTexto(nome, "nome");
        validarTexto(codigo, "codigo");

        // valida categoria
        CategoriaProduto categoria = dao
                .select()
                .from(CategoriaProduto.class)
                .id(categoriaId);

        if (categoria == null) {
            throw new NotFoundException("Categoria não encontrada: " + categoriaId);
        }

        // valida código único
        boolean existeCodigo = false;
        
        try {
        	Produto proBean = dao
        			.select()
        			.from(Produto.class)
        			.where("codigo", Condicao.EQUAL, codigo)
        			.one();
        	
        	existeCodigo = true;
        	
        } catch (NotFoundException not){
        	//segue a vida
        }

        if (existeCodigo) {
            throw new BusinessException("Já existe produto com esse código.");
        }

        Produto produto = new Produto();
        produto.setNome(nome.trim());
        produto.setCodigo(codigo.trim());
        produto.setDescricao(descricao);
        produto.setCategoriaProduto(categoria);
        produto.setAtivo(true);

        Produto salvo = dao.insert(produto);

        // ===================== CRIA ESTOQUE 1–1 =====================

        Estoque estoque = new Estoque();
        estoque.setProduto(salvo);
        estoque.setQuantidadeAtual(0);
        estoque.setEstoqueMinimo(Math.max(estoqueMinimo, 0));

        dao.insert(estoque);

        return salvo;
    }

    // ===================== AJUSTE INICIAL DE ESTOQUE =====================

    @Transactional
    public void ajustarEstoqueInicial(
            Long produtoId,
            int novaQuantidade,
            String observacao
    ) {

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

        // ===================== MOVIMENTAÇÃO (AUDITORIA) =====================

        MovimentacaoEstoque mov = new MovimentacaoEstoque();
        mov.setDataMovimentacao(new Date());
        mov.setTipoMovimentacao(TipoMovimentacaoEstoque.AJUSTE);
        mov.setQuantidade(Math.abs(novaQuantidade - quantidadeAnterior));
        mov.setObservacao(observacao);
        mov.setEstoque(estoque);

        Usuario usuarioRef = new Usuario();
        usuarioRef.setId(UserContext.getIdUsuario());
        mov.setUsuario(usuarioRef);

        dao.insert(mov);
    }

    // ===================== HELPERS =====================

    private void validarTexto(String valor, String campo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new BusinessException("Campo obrigatório: " + campo);
        }
    }
}

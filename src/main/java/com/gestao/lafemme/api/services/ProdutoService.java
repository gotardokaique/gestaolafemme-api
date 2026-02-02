package com.gestao.lafemme.api.services;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

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

    @Transactional(readOnly = true)
    public List<Produto> listarProdutos(Boolean ativos) {
        var q = dao.select()
                .from(Produto.class)
                .join("categoriaProduto")
                .join("estoque");

        if (ativos != null) {
            q.where("ativo", Condicao.EQUAL, ativos);
        }

        return q.orderBy("nome", true).list();
    }

    @Transactional(readOnly = true)
    public Produto buscarPorId(Long id) {
        try {
            return dao.select()
                    .from(Produto.class)
                    .join("categoriaProduto")
                    .join("estoque")
                    .id(id);
        } catch (Exception e) {
            throw new NotFoundException("Produto não encontrado: " + id);
        }
    }

    // ===================== CRIAR PRODUTO =====================

    @Transactional
    public Produto criarProduto(String nome, String codigo, String descricao, Long categoriaId, int estoqueMinimo) throws Exception {

        validarTexto(nome, "nome");
        validarTexto(codigo, "codigo");

        if (categoriaId == null) {
            throw new BusinessException("Campo obrigatório: categoriaId");
        }

        CategoriaProduto categoria = dao
                .select()
                .from(CategoriaProduto.class)
                .id(categoriaId);

        try {
            dao
                .select()
                .from(Produto.class)
                .where("codigo", Condicao.EQUAL, codigo.trim())
                .one();

            throw new BusinessException("Já existe produto com esse código.");

        } catch (NotFoundException not) {
            // ok
        }

        Produto produto = new Produto();
        produto.setNome(nome.trim());
        produto.setCodigo(codigo.trim());
        produto.setDescricao(descricao);
        produto.setValorCusto(BigDecimal.ZERO);
        produto.setValorVenda(BigDecimal.ZERO);
        produto.setCategoriaProduto(categoria);
        produto.setAtivo(true);

        Produto salvo = dao.insert(produto);

        // cria estoque 1–1
        Estoque estoque = new Estoque();
        estoque.setProduto(salvo);
        estoque.setQuantidadeAtual(0);
        estoque.setEstoqueMinimo(Math.max(estoqueMinimo, 0));

        dao.insert(estoque);

        return salvo;
    }

    // ===================== EDITAR PRODUTO =====================

    @Transactional
    public Produto editarProduto(
            Long produtoId,
            String nome,
            String codigo,
            String descricao,
            Long categoriaId,
            Boolean ativo
    ) throws Exception {
        if (produtoId == null) {
            throw new BusinessException("Campo obrigatório: produtoId");
        }

        Produto produto = dao
                .select()
                .from(Produto.class)
                .id(produtoId);

        if (produto == null) {
            throw new NotFoundException("Produto não encontrado: " + produtoId);
        }

        if (nome != null) {
            validarTexto(nome, "nome");
            produto.setNome(nome.trim());
        }

        if (codigo != null) {
            validarTexto(codigo, "codigo");
            String novoCodigo = codigo.trim();

            if (!novoCodigo.equals(produto.getCodigo())) {
                try {
                    Produto outro = dao
                            .select()
                            .from(Produto.class)
                            .where("codigo", Condicao.EQUAL, novoCodigo)
                            .one();

                    if (outro != null && !outro.getId().equals(produtoId)) {
                        throw new BusinessException("Já existe produto com esse código.");
                    }
                } catch (NotFoundException not) {
                    // ok
                }
                produto.setCodigo(novoCodigo);
            }
        }

        if (descricao != null) {
            produto.setDescricao(descricao);
        }

        if (categoriaId != null) {
            CategoriaProduto categoria = dao
                    .select()
                    .from(CategoriaProduto.class)
                    .id(categoriaId);
            produto.setCategoriaProduto(categoria);
        }

        if (ativo != null) {
            produto.setAtivo(ativo);
        }

        return dao.update(produto);
    }

    // ===================== APAGAR (DESATIVAR) PRODUTO =====================

    @Transactional
    public void desativarProduto(Long produtoId, String motivo) throws Exception {
        Produto produto = dao
                .select()
                .from(Produto.class)
                .id(produtoId);

        if (produto == null) {
            throw new NotFoundException("Produto não encontrado: " + produtoId);
        }

        if (produto.isAtivo() == false) {
            return; // idempotente
        }

        produto.setAtivo(false);
        dao.update(produto);

        // auditoria opcional: registra AJUSTE 0 só pra deixar rastro
        registrarAuditoria(produtoId, motivo != null ? motivo : "Produto desativado.");
    }

    // ===================== REATIVAR PRODUTO =====================

    @Transactional
    public void ativarProduto(Long produtoId, String motivo) throws Exception {
        Produto produto = dao
                .select()
                .from(Produto.class)
                .id(produtoId);

        if (produto == null) {
            throw new NotFoundException("Produto não encontrado: " + produtoId);
        }

        if (produto.isAtivo()) {
            return;
        }

        produto.setAtivo(true);
        dao.update(produto);

        registrarAuditoria(produtoId, motivo != null ? motivo : "Produto reativado.");
    }

    // ===================== DELETE FÍSICO (SÓ SE NUNCA FOI USADO) =====================
    // Use apenas se você realmente precisar remover do banco.
    // Se já existir movimentação/compra/venda, bloqueia.

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
               .where("produto.id", Condicao.EQUAL, produtoId)
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

        Usuario usuarioRef = new Usuario();
        usuarioRef.setId(UserContext.getIdUsuario());
        mov.setUsuario(usuarioRef);

        dao.insert(mov);
    }

    // ===================== AUDITORIA HELPERS =====================

    private void registrarAuditoria(Long produtoId, String observacao) {
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
}

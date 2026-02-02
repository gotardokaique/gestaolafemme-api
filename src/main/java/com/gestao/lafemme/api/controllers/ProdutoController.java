package com.gestao.lafemme.api.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gestao.lafemme.api.controllers.dto.ProdutoAjusteEstoqueDTO;
import com.gestao.lafemme.api.controllers.dto.ProdutoMotivoDTO;
import com.gestao.lafemme.api.controllers.dto.ProdutoRequestDTO;
import com.gestao.lafemme.api.controllers.dto.ProdutoResponseDTO;
import com.gestao.lafemme.api.entity.Produto;
import com.gestao.lafemme.api.services.ProdutoService;

@RestController
@RequestMapping("/api/v1/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @PostMapping
    public ResponseEntity<ProdutoResponseDTO> criar(@RequestBody ProdutoRequestDTO dto) throws Exception {
        Produto criado = produtoService.criarProduto(
                dto.nome(),
                dto.codigo(),
                dto.descricao(),
                dto.categoriaId(),
                dto.estoqueMinimo() != null ? dto.estoqueMinimo() : 0
        );

        Produto completo = produtoService.buscarPorId(criado.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ProdutoResponseDTO.from(completo));
    }

    @GetMapping
    public ResponseEntity<List<ProdutoResponseDTO>> listar(
            @RequestParam(name = "ativos", required = false) Boolean ativos
    ) {
        List<Produto> lista = produtoService.listarProdutos(ativos);
        return ResponseEntity.ok(lista.stream().map(ProdutoResponseDTO::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> buscarPorId(@PathVariable Long id) {
        Produto p = produtoService.buscarPorId(id);
        return ResponseEntity.ok(ProdutoResponseDTO.from(p));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> atualizar(
            @PathVariable Long id,
            @RequestBody ProdutoRequestDTO dto
    ) throws Exception {
        Produto atualizado = produtoService.editarProduto(
                id,
                dto.nome(),
                dto.codigo(),
                dto.descricao(),
                dto.categoriaId(),
                dto.ativo()
        );

        Produto completo = produtoService.buscarPorId(atualizado.getId());
        return ResponseEntity.ok(ProdutoResponseDTO.from(completo));
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<Void> desativar(@PathVariable Long id, @RequestBody(required = false) ProdutoMotivoDTO dto) throws Exception {
        produtoService.desativarProduto(id, dto != null ? dto.motivo() : null);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<Void> ativar(@PathVariable Long id, @RequestBody(required = false) ProdutoMotivoDTO dto) throws Exception {
        produtoService.ativarProduto(id, dto != null ? dto.motivo() : null);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/estoque-inicial")
    public ResponseEntity<Void> ajustarEstoqueInicial(
            @PathVariable Long id,
            @RequestBody ProdutoAjusteEstoqueDTO dto
    ) throws Exception {
        produtoService.ajustarEstoqueInicial(id, dto.novaQuantidade(), dto.observacao());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirFisico(@PathVariable Long id) throws Exception {
        produtoService.excluirProdutoFisico(id);
        return ResponseEntity.noContent().build();
    }
}

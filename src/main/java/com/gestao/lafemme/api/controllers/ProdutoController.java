package com.gestao.lafemme.api.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gestao.lafemme.api.controllers.dto.ProdutoAjusteEstoqueDTO;
import com.gestao.lafemme.api.controllers.dto.ProdutoRequestDTO;
import com.gestao.lafemme.api.controllers.dto.ProdutoResponseDTO;
import com.gestao.lafemme.api.services.ProdutoService;

@RestController
@RequestMapping("/api/v1/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @PostMapping
    public ResponseEntity<String> criar(@RequestBody ProdutoRequestDTO dto) throws Exception {
        produtoService.criarProduto(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Produto criado com sucesso!");
    }

    @GetMapping
    public ResponseEntity<List<ProdutoResponseDTO>> listar(@RequestParam(name = "ativos", required = false) Boolean ativo) {
        return ResponseEntity.ok(produtoService.listarProdutos(ativo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> buscarPorId(@PathVariable Long id) throws Exception {
        return ResponseEntity.ok(produtoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> atualizar(@PathVariable Long id, @RequestBody ProdutoRequestDTO dto) throws Exception {
        produtoService.editarProduto(id, dto);
        return ResponseEntity.ok("Produto atualizado com sucesso!");
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<String> alterarStatus(@PathVariable Long id) throws Exception {
        produtoService.ativarInativarProduto(id);
        return ResponseEntity.ok("Status do produto alterado com sucesso!");
    }

    @PatchMapping("/{id}/estoque")
    public ResponseEntity<String> ajustarEstoque(
            @PathVariable Long id,
            @RequestBody ProdutoAjusteEstoqueDTO dto
    ) throws Exception {
        produtoService.ajustarEstoque(id, dto.novaQuantidade(), dto.observacao());
        return ResponseEntity.ok("Estoque ajustado com sucesso!");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> excluir(@PathVariable Long id) throws Exception {
        produtoService.excluirProdutoFisico(id);
        return ResponseEntity.ok("Produto excluído com sucesso!");
    }
}


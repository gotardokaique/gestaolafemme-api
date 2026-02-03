package com.gestao.lafemme.api.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gestao.lafemme.api.entity.CategoriaProduto;
import com.gestao.lafemme.api.services.CategoriaProdutoService;

@RestController
@RequestMapping("/api/v1/categorias-produto")
public class CategoriaProdutoController {

    private final CategoriaProdutoService categoriaService;

    public CategoriaProdutoController(CategoriaProdutoService categoriaService) {
        this.categoriaService = categoriaService;
    }

    // ===================== CRIAR =====================

    @PostMapping
    public ResponseEntity<CategoriaProduto> criar(@RequestBody CategoriaProdutoRequestDTO dto) throws Exception {
        CategoriaProduto created = categoriaService.criarCategoria(dto.nome(), dto.descricao());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }


    @PutMapping("/{id}")
    public ResponseEntity<CategoriaProduto> editar(@PathVariable("id") Long id,
                                                  @RequestBody CategoriaProdutoRequestDTO dto) throws Exception {
        CategoriaProduto updated = categoriaService.editarCategoria(id, dto.nome(), dto.descricao());
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> alterarStatus(@PathVariable("id") Long id,
                                              @RequestParam(name = "ativo") boolean ativo) throws Exception {
        categoriaService.alterarStatusCategoria(id, ativo);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirFisico(@PathVariable("id") Long id) throws Exception {
        categoriaService.excluirCategoriaFisico(id);
        return ResponseEntity.noContent().build();
    }

    public record CategoriaProdutoRequestDTO(String nome, String descricao) {}
}

package com.gestao.lafemme.api.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gestao.lafemme.api.controllers.dto.CategoriaProdutoRequestDTO;
import com.gestao.lafemme.api.controllers.dto.CategoriaProdutoResponseDTO;
import com.gestao.lafemme.api.dev.FilterQuery;
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
    public ResponseEntity<String> criar(@RequestBody CategoriaProdutoRequestDTO dto) throws Exception {
        categoriaService.criarCategoria(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Categoria criada com sucesso!");
    }

    // ===================== LISTAR =====================

    @GetMapping
    public ResponseEntity<List<CategoriaProdutoResponseDTO>> listar(
            @RequestParam(name = "ativo", required = false) Boolean ativo, FilterQuery filter
    ) {
        return ResponseEntity.ok(categoriaService.listar(ativo, filter));
    }

    // ===================== BUSCAR POR ID =====================

    @GetMapping("/{id}")
    public ResponseEntity<CategoriaProdutoResponseDTO> buscarPorId(@PathVariable Long id) {
        CategoriaProduto cat = categoriaService.buscarPorId(id);
        return ResponseEntity.ok(CategoriaProdutoResponseDTO.refactor(cat));
    }

    // ===================== EDITAR =====================

    @PutMapping("/{id}")
    public ResponseEntity<String> editar(@PathVariable("id") Long id,
                                       @RequestBody CategoriaProdutoRequestDTO dto) throws Exception {
        categoriaService.editarCategoria(id, dto);
        return ResponseEntity.ok("Categoria atualizada com sucesso!");
    }

    // ===================== STATUS =====================

    @PatchMapping("/{id}/status")
    public ResponseEntity<String> alterarStatus(@PathVariable("id") Long id,
                                              @RequestParam(name = "ativo") boolean ativo) throws Exception {
        categoriaService.alterarStatusCategoria(id, ativo);
        return ResponseEntity.ok("Status da categoria alterado com sucesso!");
    }

    // ===================== DELETE =====================

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirFisico(@PathVariable("id") Long id) throws Exception {
        categoriaService.excluirCategoriaFisico(id);
        return ResponseEntity.noContent().build();
    }
}


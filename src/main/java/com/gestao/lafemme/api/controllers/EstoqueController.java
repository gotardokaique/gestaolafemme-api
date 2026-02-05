package com.gestao.lafemme.api.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gestao.lafemme.api.controllers.dto.EstoqueResponseDTO;
import com.gestao.lafemme.api.controllers.dto.ProdutoAjusteEstoqueDTO;
import com.gestao.lafemme.api.services.EstoqueService;
import com.gestao.lafemme.api.services.ProdutoService;

@RestController
@RequestMapping("/api/v1/estoque")
public class EstoqueController {

    private final EstoqueService estoqueService;
    private final ProdutoService produtoService;

    public EstoqueController(EstoqueService estoqueService, ProdutoService produtoService) {
        this.estoqueService = estoqueService;
        this.produtoService = produtoService;
    }

    @GetMapping
    public ResponseEntity<List<EstoqueResponseDTO>> listar() {
        return ResponseEntity.ok(estoqueService.listarEstoque());
    }

    @PatchMapping("/ajuste/{produtoId}")
    public ResponseEntity<String> ajustar(
            @PathVariable Long produtoId,
            @RequestBody ProdutoAjusteEstoqueDTO dto
    ) throws Exception {
        produtoService.ajustarEstoque(produtoId, dto.novaQuantidade(), dto.observacao());
        return ResponseEntity.ok("Estoque ajustado com sucesso!");
    }
}

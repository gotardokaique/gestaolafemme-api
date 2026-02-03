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
    public ResponseEntity<String> criarProd(@RequestBody ProdutoRequestDTO dto) throws Exception {
    	
    	produtoService.criarProduto(dto);
       
        return ResponseEntity.status(HttpStatus.CREATED).body("Produto criado com sucesso !");
    }

    @GetMapping
    public ResponseEntity<List<ProdutoResponseDTO>> listarProd(@RequestParam(name = "ativos", required = false) Boolean ativo) {
        
        return ResponseEntity.ok(produtoService.listarProdutos(ativo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> buscarPorId(@PathVariable Long id) throws Exception {

        return ResponseEntity.ok(produtoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> atualizarProd(@PathVariable Integer id, @RequestBody ProdutoRequestDTO dto) throws Exception {
    	
    	produtoService.editarProduto(id, dto);
    
        return ResponseEntity.status(HttpStatus.CREATED).body("Produto criado com sucesso !");
    }

    @PatchMapping("/{id}/ativar-inativar")
    public ResponseEntity<Void> ativarDesativar(@PathVariable Integer id, @RequestBody(required = false) ProdutoMotivoDTO dto) throws Exception {
        produtoService.ativarInativarProduto(id, dto != null ? dto.motivo() : null);
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

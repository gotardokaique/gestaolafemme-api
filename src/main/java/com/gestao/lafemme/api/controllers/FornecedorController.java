package com.gestao.lafemme.api.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.gestao.lafemme.api.controllers.dto.FornecedorRequestDTO;
import com.gestao.lafemme.api.controllers.dto.FornecedorResponseDTO;
import com.gestao.lafemme.api.entity.Fornecedor;
import com.gestao.lafemme.api.services.FornecedorService;

@RestController
@RequestMapping("/api/v1/fornecedores")
public class FornecedorController {

    private final FornecedorService fornecedorService;

    public FornecedorController(FornecedorService fornecedorService) {
        this.fornecedorService = fornecedorService;
    }

    // ===================== CRIAR =====================

    @PostMapping
    public ResponseEntity<String> criarFornecedor(@RequestBody FornecedorRequestDTO dto) {
        fornecedorService.criarFornecedor(dto);
        
        return ResponseEntity.status(HttpStatus.CREATED).body("Fornecedor criado com sucesso!");
    }

    // ===================== LISTAR =====================

    @GetMapping
    public ResponseEntity<List<FornecedorResponseDTO>> listar() {
      

        return ResponseEntity.ok(fornecedorService.listarTodos());
    }

    // ===================== BUSCAR POR ID =====================

    @GetMapping("/{id}")
    public ResponseEntity<FornecedorResponseDTO> buscarPorId(@PathVariable Integer id) {
        Fornecedor fornecedor = fornecedorService.buscarPorId(id);
        
        return ResponseEntity.ok(FornecedorResponseDTO.refactor(fornecedor));
    }

    // ===================== ATUALIZAR =====================

    @PutMapping("/{id}")
    public ResponseEntity<String> atualizar(@PathVariable Integer id, @RequestBody FornecedorRequestDTO dto) {
        fornecedorService.atualizar(id, dto);
        return ResponseEntity.ok("Fornecedor atualizado com sucesso!");
    }

    // ===================== ATIVAR / DESATIVAR =====================

    @PatchMapping("/{id}/ativar-inativar")
    public ResponseEntity<String> desativar(@PathVariable Integer id) {
        fornecedorService.ativarDesativar(id);
        return ResponseEntity.ok("Fornecedor ativado/inativado com sucesso!");
    }

}

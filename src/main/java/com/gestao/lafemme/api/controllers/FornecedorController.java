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
    public ResponseEntity<FornecedorResponseDTO> criar(@RequestBody FornecedorRequestDTO dto) {
        Fornecedor f = fornecedorService.criar(dto.nome(), dto.telefone(), dto.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(FornecedorResponseDTO.from(f));
    }

    // ===================== LISTAR =====================

    @GetMapping
    public ResponseEntity<List<FornecedorResponseDTO>> listar(
            @RequestParam(name = "ativos", required = false, defaultValue = "true") boolean ativos
    ) {
        List<Fornecedor> lista = ativos
                ? fornecedorService.listarAtivos()
                : fornecedorService.listarTodos();

        return ResponseEntity.ok(lista.stream().map(FornecedorResponseDTO::from).toList());
    }

    // ===================== BUSCAR POR ID =====================

    @GetMapping("/{id}")
    public ResponseEntity<FornecedorResponseDTO> buscarPorId(@PathVariable Long id) {
        Fornecedor f = fornecedorService.buscarPorId(id);
        return ResponseEntity.ok(FornecedorResponseDTO.from(f));
    }

    // ===================== ATUALIZAR =====================

    @PutMapping("/{id}")
    public ResponseEntity<FornecedorResponseDTO> atualizar(
            @PathVariable Long id,
            @RequestBody FornecedorRequestDTO dto
    ) {
        Fornecedor f = fornecedorService.atualizar(id, dto.nome(), dto.telefone(), dto.email(), dto.ativo());
        return ResponseEntity.ok(FornecedorResponseDTO.from(f));
    }

    // ===================== ATIVAR / DESATIVAR =====================

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        fornecedorService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/ativar")
    public ResponseEntity<Void> ativar(@PathVariable Long id) {
        // Reaproveita atualizar com ativo=true, sem mexer no resto
        fornecedorService.atualizar(id, null, null, null, true);
        return ResponseEntity.noContent().build();
    }
}

package com.gestao.lafemme.api.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestao.lafemme.api.controllers.dto.MovimentacaoEstoqueResponseDTO;
import com.gestao.lafemme.api.services.MovimentacaoEstoqueService;

@RestController
@RequestMapping("/api/v1/movimentacoes-estoque")
public class MovimentacaoEstoqueController {

    private final MovimentacaoEstoqueService service;

    public MovimentacaoEstoqueController(MovimentacaoEstoqueService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<MovimentacaoEstoqueResponseDTO>> listar() {
        return ResponseEntity.ok(service.listarMovimentacoes());
    }
}

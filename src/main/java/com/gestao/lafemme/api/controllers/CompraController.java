package com.gestao.lafemme.api.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gestao.lafemme.api.controllers.dto.CompraRequestDTO;
import com.gestao.lafemme.api.controllers.dto.CompraResponseDTO;
import com.gestao.lafemme.api.entity.Compra;
import com.gestao.lafemme.api.services.CompraService;

@RestController
@RequestMapping("/api/v1/compras")
public class CompraController {

    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @PostMapping
    public ResponseEntity<String> criar(@RequestBody CompraRequestDTO dto) throws Exception {
        compraService.criarCompra(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Compra realizada com sucesso!");
    }

    @GetMapping
    public ResponseEntity<List<CompraResponseDTO>> listar() {
        return ResponseEntity.ok(compraService.listarCompras());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompraResponseDTO> buscarPorId(@PathVariable Long id) {
        Compra compra = compraService.buscarPorId(id);
        return ResponseEntity.ok(CompraResponseDTO.refactor(compra));
    }
}


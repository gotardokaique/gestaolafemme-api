package com.gestao.lafemme.api.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.gestao.lafemme.api.controllers.dto.VendaRequestDTO;
import com.gestao.lafemme.api.controllers.dto.VendaResponseDTO;
import com.gestao.lafemme.api.services.VendaService;

@RestController
@RequestMapping("/api/v1/vendas")
public class VendaController {

    private final VendaService vendaService;

    public VendaController(VendaService vendaService) {
        this.vendaService = vendaService;
    }

    @PostMapping
    public ResponseEntity<String> criar(@RequestBody VendaRequestDTO dto) throws Exception {
        vendaService.criarVenda(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Venda realizada com sucesso!");
    }

    @GetMapping
    public ResponseEntity<List<VendaResponseDTO>> listar() {
        return ResponseEntity.ok(vendaService.listarVendas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendaResponseDTO> buscarPorId(@PathVariable Long id) throws Exception {
        return ResponseEntity.ok(vendaService.buscarPorId(id));
    }
}

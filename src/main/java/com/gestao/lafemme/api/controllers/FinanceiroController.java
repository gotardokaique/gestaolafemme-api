package com.gestao.lafemme.api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestao.lafemme.api.controllers.dto.ApiResponse;
import com.gestao.lafemme.api.controllers.dto.FinanceiroResumoDTO;
import com.gestao.lafemme.api.controllers.dto.LancamentoFinanceiroRequestDTO;
import com.gestao.lafemme.api.services.FinanceiroService;

@RestController
@RequestMapping("/api/v1/financeiro")
public class FinanceiroController {

    private final FinanceiroService service;

    public FinanceiroController(FinanceiroService service) {
        this.service = service;
    }

    @GetMapping("/resumo")
    public ResponseEntity<ApiResponse<FinanceiroResumoDTO>> obterResumo() {
        FinanceiroResumoDTO resumo = service.obterResumoFinanceiro();
        return ResponseEntity.ok(new ApiResponse<>(true, "Resumo financeiro carregado.", resumo));
    }

    @org.springframework.web.bind.annotation.PostMapping("/lancamento")
    public ResponseEntity<ApiResponse<String>> criarLancamento(@org.springframework.web.bind.annotation.RequestBody LancamentoFinanceiroRequestDTO dto) {
        service.criarLancamento(dto);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lançamento realizado com sucesso!", null));
    }
}

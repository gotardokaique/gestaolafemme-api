package com.gestao.lafemme.api.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestao.lafemme.api.context.UserContext;
import com.gestao.lafemme.api.controllers.dto.ApiResponse;
import com.gestao.lafemme.api.controllers.dto.CriarUnidadeRequestDTO;
import com.gestao.lafemme.api.controllers.dto.CriarUnidadeResponseDTO;
import com.gestao.lafemme.api.services.AdminUnitService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("api/v1/admin")
public class AdminUnitController {

    private final AdminUnitService adminUnitService;

    public AdminUnitController(AdminUnitService adminUnitService) {
        this.adminUnitService = adminUnitService;
    }

    @PostMapping("/unidade")
    public ResponseEntity<ApiResponse<CriarUnidadeResponseDTO>> criarUnidade(
            @Valid @RequestBody CriarUnidadeRequestDTO request) {
        
        String authEmail = UserContext.getUsuario().getEmail();
        if (authEmail.equalsIgnoreCase("kaiquecgotardo@gmail.com") == false) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse<>(false, "Acesso negado", null));
        }

        try {
            CriarUnidadeResponseDTO response = adminUnitService.criarUnidade(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(true, "Unidade criada com sucesso", response));
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Erro ao criar unidade: " + e.getMessage(), null));
        }
    }
}

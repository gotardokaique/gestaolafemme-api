package com.gestao.lafemme.api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestao.lafemme.api.controllers.dto.ApiResponse;
import com.gestao.lafemme.api.controllers.dto.DashboardDTO;
import com.gestao.lafemme.api.services.DashboardService;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardDTO>> obterDashboard() {
        DashboardDTO dashboard = service.obterDashboard();
        return ResponseEntity.ok(new ApiResponse<>(true, "Dashboard carregado.", dashboard));
    }
}

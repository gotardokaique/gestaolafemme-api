package com.gestao.lafemme.api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestao.lafemme.api.context.UserContext;
import com.gestao.lafemme.api.controllers.dto.ConfiguracaoTokenDTO;
import com.gestao.lafemme.api.controllers.dto.EmailConfigRequestDTO;
import com.gestao.lafemme.api.controllers.dto.EmailConfigResponseDTO;
import com.gestao.lafemme.api.services.ConfiguracaoService;

@RestController
@RequestMapping("/api/v1/configuracao")
public class ConfiguracaoController {

    private final ConfiguracaoService configuracaoService;

    public ConfiguracaoController(ConfiguracaoService configuracaoService) {
        this.configuracaoService = configuracaoService;
    }

    @GetMapping("/token")
    public ResponseEntity<ConfiguracaoTokenDTO> getToken() {
        Long userId = UserContext.getIdUsuario();
        String token = configuracaoService.buscarTokenAtivo(userId);
        return ResponseEntity.ok(new ConfiguracaoTokenDTO(token));
    }

    @PostMapping("/token")
    public ResponseEntity<ConfiguracaoTokenDTO> gerarToken() throws Exception {
        String token = configuracaoService.gerarToken();
        return ResponseEntity.ok(new ConfiguracaoTokenDTO(token));
    }

    @DeleteMapping("/token")
    public ResponseEntity<String> revogarToken() throws Exception {
        configuracaoService.revogarToken();
        return ResponseEntity.ok("Token revogado com sucesso!");
    }

    @GetMapping("/email")
    public ResponseEntity<EmailConfigResponseDTO> getEmailConfig() {
        EmailConfigResponseDTO response = configuracaoService.buscarEmailConfig();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/email")
    public ResponseEntity<EmailConfigResponseDTO> salvarEmailConfig(
            @RequestBody EmailConfigRequestDTO request) throws Exception {
        EmailConfigResponseDTO response = configuracaoService.salvarEmailConfig(request);
        return ResponseEntity.ok(response);
    }
}

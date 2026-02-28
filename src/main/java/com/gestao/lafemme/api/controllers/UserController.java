package com.gestao.lafemme.api.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestao.lafemme.api.controllers.dto.ApiResponse;
import com.gestao.lafemme.api.controllers.dto.CheckTrocarSenhaResponseDTO;
import com.gestao.lafemme.api.controllers.dto.CriarNovoUsuarioRequestDTO;
import com.gestao.lafemme.api.controllers.dto.CriarNovoUsuarioResponseDTO;
import com.gestao.lafemme.api.controllers.dto.TrocarSenhaRequestDTO;
import com.gestao.lafemme.api.controllers.dto.UserMeResponseDTO;
import com.gestao.lafemme.api.controllers.dto.UsuarioUnidadeDTO;
import com.gestao.lafemme.api.services.UserService;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserMeResponseDTO>> getMe() {
        UserMeResponseDTO me = userService.getMe();
        return ResponseEntity.ok(new ApiResponse<>(true, "Dados do usuário carregados.", me));
    }

    /**
     * Endpoint para admin criar novo usuário com senha temporária.
     * Requer autenticação (não está na rota /auth).
     * 
     * @param request DTO com nome e email do novo usuário
     * @return Response com email e senha temporária gerada
     */
    @PostMapping("/criar")
    public ResponseEntity<ApiResponse<CriarNovoUsuarioResponseDTO>> criarNovoUsuario(
            @RequestBody @Valid CriarNovoUsuarioRequestDTO request) {
        try {
            CriarNovoUsuarioResponseDTO response = userService.criarNovoUsuario(request);
            // Headers anti-cache: senha temporária NÃO pode ser cacheada por proxies/CDN
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("Cache-Control", "no-store, no-cache, must-revalidate")
                    .header("Pragma", "no-cache")
                    .header("Expires", "0")
                    .body(new ApiResponse<>(true, "Usuário criado com sucesso!", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Erro ao criar usuário: " + e.getMessage(), null));
        }
    }

    /**
     * Endpoint para verificar se o usuário logado precisa trocar a senha.
     * Chamado após login bem-sucedido.
     * 
     * @return Response indicando se precisa trocar senha
     */
    @GetMapping("/check-trocar-senha")
    public ResponseEntity<ApiResponse<CheckTrocarSenhaResponseDTO>> checkTrocarSenha() {
        try {
            CheckTrocarSenhaResponseDTO response = userService.checkTrocarSenha();
            return ResponseEntity.ok(new ApiResponse<>(true, "Verificação realizada.", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Erro ao verificar: " + e.getMessage(), null));
        }
    }

    /**
     * Endpoint para trocar senha de forma obrigatória.
     * Valida senha atual e força senha forte.
     * 
     * @param request DTO com senha atual, nova senha e confirmação
     * @return Response de sucesso ou erro
     */
    @PostMapping("/trocar-senha")
    public ResponseEntity<ApiResponse<Void>> trocarSenha(@RequestBody TrocarSenhaRequestDTO request) {
        try {
            userService.trocarSenhaObrigatoria(request);
            return ResponseEntity.ok(new ApiResponse<>(true, "Senha alterada com sucesso!", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Erro ao trocar senha: " + e.getMessage(), null));
        }
    }

    /**
     * Endpoint para listar todos os usuários da mesma unidade do usuário logado.
     * 
     * @return Lista de usuários da unidade
     */
    @GetMapping("/usuarios-unidade")
    public ResponseEntity<ApiResponse<List<UsuarioUnidadeDTO>>> listarUsuariosDaUnidade() {
        try {
            List<UsuarioUnidadeDTO> usuarios = userService.listarUsuariosDaUnidade();
            return ResponseEntity.ok(new ApiResponse<>(true, "Usuários carregados.", usuarios));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Erro ao carregar usuários: " + e.getMessage(), null));
        }
    }
}

package com.gestao.lafemme.api.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestao.lafemme.api.controllers.dto.ApiResponse;
import com.gestao.lafemme.api.controllers.dto.UserMeResponseDTO;
import com.gestao.lafemme.api.services.UserService;

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
}

package com.gestao.lafemme.api.security.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestao.lafemme.api.entity.Usuario;
import com.gestao.lafemme.api.security.controller.DTOs.LoginRequestDTO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    @Autowired private SessionService sessionService;
    @Autowired private RegisterUserBO registerBO;


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequestDTO dto, HttpServletRequest request) {
        return registerBO.processarLogin(dto.email(), dto.senha(), request);
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Usuario user) {
            sessionService.removeToken(user.getId());
            return ResponseEntity.ok("Logout executado.");
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuário não autenticado.");
    }
}

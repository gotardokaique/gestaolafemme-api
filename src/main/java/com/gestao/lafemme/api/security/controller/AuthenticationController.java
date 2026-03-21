package com.gestao.lafemme.api.security.controller;

import com.gestao.lafemme.api.security.JwtAuthenticationFilter;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gestao.lafemme.api.security.controller.DTOs.LoginRequestDTO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RegisterUserBO registerBO;

    AuthenticationController(JwtAuthenticationFilter jwtAuthenticationFilter,
            RegisterUserBO registerBO) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.registerBO = registerBO;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequestDTO dto, HttpServletRequest request) {
        return registerBO.processarLogin(dto.email(), dto.senha(), request);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        jwtAuthenticationFilter.invalidarCacheUsuario(email);

        ResponseCookie cookie = ResponseCookie.from("auth_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(Map.of("success", true, "message", "Logout realizado"));
    }
}
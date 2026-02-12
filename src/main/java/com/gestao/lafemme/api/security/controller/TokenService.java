package com.gestao.lafemme.api.security.controller;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

import com.gestao.lafemme.api.entity.Usuario;
import com.gestao.lafemme.api.security.JwtTokenProvider;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class TokenService {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final int MAX_TOKEN_LENGTH = 2048;

    private final JwtTokenProvider tokenProvider;

    public TokenService(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    public String generateToken(Usuario user) {
        if (user == null || user.getEmail() == null) {
            throw new IllegalArgumentException("Usuário inválido para geração de token");
        }
        return tokenProvider.generateTokenFromUsername(user.getEmail());
    }

    public String validateToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        if (token.length() > MAX_TOKEN_LENGTH) {
            return null;
        }

        if (!tokenProvider.validateToken(token)) {
            return null;
        }

        return tokenProvider.getUsernameFromJWT(token);
    }

    public String getTokenFromRequest(HttpServletRequest request) {
        if (request == null) return null;

        Cookie cookie = WebUtils.getCookie(request, "auth_token");
        if (cookie != null && StringUtils.hasText(cookie.getValue())) {
            String token = cookie.getValue();
            return token.length() <= MAX_TOKEN_LENGTH ? token : null;
        }
        return null;
    }
}

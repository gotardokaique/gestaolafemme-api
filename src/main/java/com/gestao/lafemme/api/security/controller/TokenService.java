package com.gestao.lafemme.api.security.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.gestao.lafemme.api.entity.Unidade;
import com.gestao.lafemme.api.entity.Usuario;
import com.gestao.lafemme.api.security.JwtTokenProvider;
import com.gestao.lafemme.api.services.exceptions.BusinessException;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class TokenService {

    @Autowired
    private JwtTokenProvider tokenProvider;

    public String generateToken(Usuario user) {
        if (user == null) throw new BusinessException("Usuário inválido.");
        if (user.getId() == null) throw new BusinessException("Usuário sem id.");
        if (user.getUnidadeAtiva() == null || user.getUnidadeAtiva().getId() == null) {
            throw new BusinessException("Usuário sem unidade ativa.");
        }

        Map<String, Object> claims = Map.of(
                "uid", user.getId(),
                "uni_id", user.getUnidadeAtiva().getId()
        );

        return tokenProvider.generateToken(user.getEmail(), claims);
    }

    public TokenPayload validateToken(String token) {
        if (token == null || token.isBlank()) return null;

        if (!tokenProvider.validateToken(token)) return null;

        String email = tokenProvider.getUsernameFromJWT(token);
        Long uid = tokenProvider.getLongClaim(token, "uid");
        Long unidadeId = tokenProvider.getLongClaim(token, "uni_id");

        if (uid == null || unidadeId == null) return null;

        return new TokenPayload(email, uid, unidadeId);
    }

    public String getTokenFromRequest(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) return null;
        if (!authHeader.startsWith("Bearer ")) return null;
        return authHeader.substring(7);
    }

    public record TokenPayload(String email, Long userId, Long unidadeId) {
        public Unidade unidadeRef() {
            Unidade u = new Unidade();
            u.setId(unidadeId);
            return u;
        }
    }
}

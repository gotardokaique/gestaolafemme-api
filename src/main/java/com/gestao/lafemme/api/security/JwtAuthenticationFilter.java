package com.gestao.lafemme.api.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.gestao.lafemme.api.db.Condicao;
import com.gestao.lafemme.api.db.DAOController;
import com.gestao.lafemme.api.entity.Usuario;
import com.gestao.lafemme.api.entity.UsuarioUnidade;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final String BEARER_PREFIX = "Bearer ";
    private static final int MAX_TOKEN_LENGTH = 2048;

    private final JwtTokenProvider tokenProvider;
    private final DAOController daoController;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
                                   DAOController daoController) {
        this.tokenProvider = tokenProvider;
        this.daoController = daoController;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 1️⃣ Bypass rotas públicas
        if (isPublicAuthPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            // 2️⃣ Se já existe auth no contexto, não sobrescreve
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            // 3️⃣ Extrai token
            String jwt = getJwtFromRequest(request);
            if (!StringUtils.hasText(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 4️⃣ Limite defensivo contra header gigante
            if (jwt.length() > MAX_TOKEN_LENGTH) {
                logger.warn("JWT com tamanho inválido ({} chars)", jwt.length());
                filterChain.doFilter(request, response);
                return;
            }

            // 5️⃣ Valida assinatura / expiração
            if (!tokenProvider.validateToken(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }

            String username = tokenProvider.getUsernameFromJWT(jwt);
            if (!StringUtils.hasText(username)) {
                filterChain.doFilter(request, response);
                return;
            }

            // 6️⃣ Carrega usuário
            Usuario usuario = carregarUsuarioComUnidade(username);
            if (usuario == null) {
                logger.warn("JWT válido mas usuário não encontrado: {}", username);
                filterChain.doFilter(request, response);
                return;
            }

            // 7️⃣ Define autenticação
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            usuario,
                            null,
                            usuario.getAuthorities()
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception ex) {
            logger.error("Erro ao processar autenticação JWT", ex);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (!StringUtils.hasText(authHeader)) {
            return null;
        }

        authHeader = authHeader.trim();

        if (authHeader.length() < BEARER_PREFIX.length()) {
            return null;
        }

        if (!authHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        return StringUtils.hasText(token) ? token : null;
    }

    private Usuario carregarUsuarioComUnidade(String username) {
        try {
            Usuario usuario = daoController
                    .select()
                    .from(Usuario.class)
                    .where("email", Condicao.EQUAL, username.toLowerCase().trim())
                    .limit(1)
                    .one();

            if (usuario == null) return null;

            // resolve unidade ativa
            UsuarioUnidade uu = daoController
                    .select()
                    .from(UsuarioUnidade.class)
                    .join("usuario")
                    .join("unidade")
                    .where("usuario.id", Condicao.EQUAL, usuario.getId())
                    .where("unidade.ativo", Condicao.EQUAL, true)
                    .limit(1)
                    .one();

            if (uu != null && uu.getUnidade() != null) {
                usuario.setUnidadeAtiva(uu.getUnidade());
            }

            return usuario;

        } catch (Exception e) {
            logger.warn("Erro ao carregar usuário '{}' para autenticação JWT: {}", username, e.getMessage());
            return null;
        }
    }

    private boolean isPublicAuthPath(String path) {
        return path.equals("/api/v1/auth/login")
            || path.equals("/api/v1/auth/register")
            || path.equals("/api/v1/auth/refresh");
    }
}

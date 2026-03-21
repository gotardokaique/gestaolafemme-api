package com.gestao.lafemme.api.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestao.lafemme.api.db.Condicao;
import com.gestao.lafemme.api.db.DAOController;
import com.gestao.lafemme.api.entity.Usuario;
import com.gestao.lafemme.api.entity.UsuarioUnidade;
import com.gestao.lafemme.api.utils.HttpUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final int MAX_TOKEN_LENGTH = 2048;
    private static final String CACHE_PREFIX = "lf:auth:usr:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/mp/webhook",
            "/mp/oauth/callback",
            "/public",
            "/actuator/health",
            "/favicon.ico");

    private final JwtTokenProvider tokenProvider;
    private final DAOController daoController;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider,
            DAOController daoController,
            StringRedisTemplate redis,
            ObjectMapper objectMapper) {
        this.tokenProvider = tokenProvider;
        this.daoController = daoController;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = HttpUtils.getRequestPath(request);
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        try {
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            String jwt = HttpUtils.getCookieValue(request, "auth_token").orElse(null);

            if (!StringUtils.hasText(jwt) || jwt.length() > MAX_TOKEN_LENGTH) {
                filterChain.doFilter(request, response);
                return;
            }

            if (!tokenProvider.validateToken(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }

            String username = tokenProvider.getUsernameFromJWT(jwt);
            if (!StringUtils.hasText(username)) {
                filterChain.doFilter(request, response);
                return;
            }

            Usuario usuario = carregarUsuarioComCache(username);
            if (usuario == null) {
                HttpUtils.logSecurityEvent("JWT_USER_NOT_FOUND", request, "token válido, usuário ausente");
                filterChain.doFilter(request, response);
                return;
            }

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    usuario, null, usuario.getAuthorities());
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception ex) {
            HttpUtils.logSecurityEvent("JWT_AUTH_ERROR", request, ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private Usuario carregarUsuarioComCache(String username) {
        String key = CACHE_PREFIX + username.toLowerCase().trim();
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                return objectMapper.readValue(cached, Usuario.class);
            }
        } catch (Exception e) {
            log.debug("[JWT] Cache miss — buscando no banco.");
        }

        try {
            Usuario usuario = daoController
                    .select()
                    .from(Usuario.class)
                    .where("email", Condicao.EQUAL, username.toLowerCase().trim())
                    .limit(1)
                    .one();

            if (usuario == null)
                return null;

            UsuarioUnidade uu = daoController
                    .select()
                    .from(UsuarioUnidade.class)
                    .join("usuario")
                    .join("unidade")
                    .where("usuario.id", Condicao.EQUAL, usuario.getId())
                    .where("unidade.ativo", Condicao.EQUAL, true)
                    .one();

            if (uu != null && uu.getUnidade() != null) {
                usuario.setUnidadeAtiva(uu.getUnidade());
            }

            try {
                redis.opsForValue().set(key, objectMapper.writeValueAsString(usuario), CACHE_TTL);
            } catch (Exception e) {
                log.debug("[JWT] Falha ao gravar no cache: {}", e.getMessage());
            }

            return usuario;

        } catch (Exception e) {
            log.warn("[JWT] Erro ao carregar usuário do banco: {}", e.getMessage());
            return null;
        }
    }

    public void invalidarCacheUsuario(String email) {
        try {
            redis.delete(CACHE_PREFIX + email.toLowerCase().trim());
        } catch (Exception e) {
            log.warn("[JWT] Falha ao invalidar cache: {}", e.getMessage());
        }
    }
}
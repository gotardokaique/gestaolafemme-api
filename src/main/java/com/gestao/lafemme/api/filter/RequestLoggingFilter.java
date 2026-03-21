package com.gestao.lafemme.api.filter;

import com.gestao.lafemme.api.security.controller.TokenService;
import com.gestao.lafemme.api.utils.HttpUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
@Order(1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String ANON = "anônimo";
    private static final Set<String> SILENT_PATHS = Set.of("/actuator/health", "/favicon.ico");

    private final TokenService tokenService;

    public RequestLoggingFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return SILENT_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String fullUrl = request.getQueryString() != null
                ? request.getRequestURI() + "?" + request.getQueryString()
                : request.getRequestURI();

        chain.doFilter(request, response);

        log.info("[{}] {} | IP: {} | Usuário: {} → endpoint: {}",
                request.getMethod(),
                response.getStatus(),
                HttpUtils.getClientIp(request), // ← reutilizando
                resolveEmail(request),
                fullUrl);
    }

    private String resolveEmail(HttpServletRequest request) {
        try {
            String token = tokenService.getTokenFromRequest(request);
            if (token == null)
                return ANON;
            String email = tokenService.validateToken(token);
            return (email != null && !email.isBlank()) ? email : ANON;
        } catch (Exception e) {
            return ANON;
        }
    }
}
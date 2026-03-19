package com.gestao.lafemme.api.filter;

import com.gestao.lafemme.api.security.controller.TokenService;
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

    private static final Set<String> SILENT_PATHS = Set.of(
            "/actuator/health",
            "/favicon.ico");

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

        String method = request.getMethod();
        String ip = getClientIp(request);
        String email = resolveEmail(request);
        String url = request.getRequestURI();
        String query = request.getQueryString();
        String fullUrl = query != null ? url + "?" + query : url;

        chain.doFilter(request, response);

        log.info("[{}] {} | IP: {} | Usuário: {} → endpoint: {}",
                method, response.getStatus(), ip, email, fullUrl);
    }

    // -------------------------------------------------------------------------

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

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("CF-Connecting-IP");
        if (isValidIp(ip))
            return ip.trim();

        ip = request.getHeader("X-Forwarded-For");
        if (isValidIp(ip))
            return ip.split(",")[0].trim();

        ip = request.getHeader("X-Real-IP");
        if (isValidIp(ip))
            return ip.trim();

        return request.getRemoteAddr();
    }

    private boolean isValidIp(String ip) {
        return ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip);
    }
}
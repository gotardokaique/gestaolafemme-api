package com.gestao.lafemme.api.filter;

import com.gestao.lafemme.api.utils.SecuritySanitizer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import java.io.IOException;

@Component
@Order(2)
public class BodySanitizingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BodySanitizingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String contentType = request.getContentType();

        try {
            if (isMultipart(contentType)) {
                sanitizarMultipart(request);
                chain.doFilter(request, response);

            } else if (isJson(contentType) || isForm(contentType)) {
                CachedBodyRequestWrapper wrapped = new CachedBodyRequestWrapper(request);
                sanitizarBody(wrapped.getBodyAsString());
                chain.doFilter(wrapped, response);

            } else {
                chain.doFilter(request, response);
            }

        } catch (SecuritySanitizer.InputMaliciosoException ex) {
            log.warn("[SECURITY] Body malicioso bloqueado | URI: {} | Motivo: {}",
                    request.getRequestURI(), ex.getMessage());
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    private void sanitizarBody(String body) {
        if (body == null || body.isBlank()) return;
        SecuritySanitizer.assertSafe("body", body);
    }

    private void sanitizarMultipart(HttpServletRequest request) throws IOException, ServletException {
        StandardMultipartHttpServletRequest multipart = new StandardMultipartHttpServletRequest(request);

        multipart.getParameterMap().forEach((campo, valores) -> {
            for (String valor : valores) {
                SecuritySanitizer.assertSafe(campo, valor);
            }
        });

        for (MultipartFile file : multipart.getFileMap().values()) {
            if (!file.isEmpty()) {
                SecuritySanitizer.assertImagemSegura(file);
            }
        }
    }

    private boolean isMultipart(String ct) {
        return ct != null && ct.startsWith("multipart/");
    }

    private boolean isJson(String ct) {
        return ct != null && ct.contains("application/json");
    }

    private boolean isForm(String ct) {
        return ct != null && ct.contains("application/x-www-form-urlencoded");
    }
}
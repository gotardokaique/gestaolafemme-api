package com.gestao.lafemme.api.filter;

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
import java.util.Base64;

@Component
@Order(1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static final String CLAIM_EMAIL = "sub";
    private static final String ANON = "anônimo";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String method = request.getMethod();
        String ip = HttpUtils.getClientIp(request);
        String email = resolveEmail(request);
        String url = request.getRequestURI();

        String query = request.getQueryString();
        String fullUrl = query != null ? url + "?" + query : url;

        log.info("[{}] {} | IP: {} | Usuário: {} → {}",
                method, response.getStatus(), ip, email, fullUrl);

        chain.doFilter(request, response);
    }

    private String resolveEmail(HttpServletRequest request) {
        try {
            return HttpUtils.getCookieValue(request, "token")
                    .map(RequestLoggingFilter::decodeEmailFromJwt)
                    .orElseGet(() -> HttpUtils.extractBearerToken(request)
                            .map(RequestLoggingFilter::decodeEmailFromJwt)
                            .orElse(ANON));
        } catch (Exception e) {
            return ANON;
        }
    }

    private static String decodeEmailFromJwt(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2)
                return ANON;

            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            String payload = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);

            int subIdx = payload.indexOf("\"" + CLAIM_EMAIL + "\"");
            if (subIdx == -1)
                return ANON;

            int colonIdx = payload.indexOf(":", subIdx);
            int startIdx = payload.indexOf("\"", colonIdx) + 1;
            int endIdx = payload.indexOf("\"", startIdx);

            if (startIdx <= 0 || endIdx <= startIdx)
                return ANON;

            return payload.substring(startIdx, endIdx);
        } catch (Exception e) {
            return ANON;
        }
    }
}
package com.gestao.lafemme.api.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public final class HttpUtils {

    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";
    private static final String HEADER_CF_CONNECTING_IP = "CF-Connecting-IP";
    private static final String HEADER_USER_AGENT = "User-Agent";
    private static final String UNKNOWN_IP = "unknown";

    private HttpUtils() {
    }

    public static Optional<HttpServletRequest> getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return Optional.ofNullable(attrs).map(ServletRequestAttributes::getRequest);
    }

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader(HEADER_CF_CONNECTING_IP);
        if (isValidIp(ip))
            return ip.trim();

        ip = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (isValidIp(ip))
            return ip.split(",")[0].trim();

        ip = request.getHeader(HEADER_X_REAL_IP);
        if (isValidIp(ip))
            return ip.trim();

        return request.getRemoteAddr();
    }

    public static String getClientIp() {
        return getCurrentRequest()
                .map(HttpUtils::getClientIp)
                .orElse(UNKNOWN_IP);
    }

    public static String getUserAgent(HttpServletRequest request) {
        String ua = request.getHeader(HEADER_USER_AGENT);
        return ua != null ? ua : "unknown";
    }

    public static Optional<String> extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER_AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return Optional.of(header.substring(BEARER_PREFIX.length()).trim());
        }
        return Optional.empty();
    }

    public static String getRequestPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    public static String getHttpMethod(HttpServletRequest request) {
        return request.getMethod();
    }

    public static Map<String, String> getAllHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        if (names != null) {
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                headers.put(name, request.getHeader(name));
            }
        }
        return Collections.unmodifiableMap(headers);
    }

    public static Optional<String> getHeader(HttpServletRequest request, String headerName) {
        return Optional.ofNullable(request.getHeader(headerName));
    }

    public static boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"));
    }

    public static boolean isSecure(HttpServletRequest request) {
        return request.isSecure()
                || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null)
            return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> c.getName().equals(name))
                .findFirst();
    }

    public static Optional<String> getCookieValue(HttpServletRequest request, String name) {
        return getCookie(request, name).map(Cookie::getValue);
    }

    /**
     * Cria cookie seguro (HttpOnly, Secure, SameSite=Strict) conforme padrão
     * 
     * @param name     nome do cookie
     * @param value    valor do cookie
     * @param maxAge   tempo de vida em segundos (negativo = sessão)
     * @param response HttpServletResponse para adicionar o Set-Cookie header
     *                 manualmente
     */
    public static void addSecureCookie(HttpServletResponse response,
            String name,
            String value,
            int maxAge) {
        // Cookie com SameSite=Strict precisa ser setado via Set-Cookie header
        // manualmente
        // pois a API Cookie do Servlet não suporta SameSite diretamente
        String cookieValue = name + "=" + value
                + "; Max-Age=" + maxAge
                + "; Path=/"
                + "; HttpOnly"
                + "; Secure"
                + "; SameSite=Strict";
        response.addHeader("Set-Cookie", cookieValue);
        log.info("Cookie seguro '{}' adicionado à resposta", name);
    }

    public static void removeCookie(HttpServletResponse response, String name) {
        String cookieValue = name + "=; Max-Age=0; Path=/; HttpOnly; Secure; SameSite=Strict";
        response.addHeader("Set-Cookie", cookieValue);
        log.info("Cookie '{}' removido da resposta", name);
    }

    public static <T> ResponseEntity<T> ok(T body) {
        return ResponseEntity.ok(body);
    }

    public static ResponseEntity<Void> ok() {
        return ResponseEntity.ok().build();
    }

    public static <T> ResponseEntity<T> created(T body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    public static ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }

    public static ResponseEntity<Map<String, String>> badRequest(String mensagem) {
        return ResponseEntity.badRequest().body(erroMap(mensagem));
    }

    public static ResponseEntity<Map<String, String>> unauthorized(String mensagem) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(erroMap(mensagem));
    }

    public static ResponseEntity<Map<String, String>> forbidden(String mensagem) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(erroMap(mensagem));
    }

    public static ResponseEntity<Map<String, String>> notFound(String mensagem) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erroMap(mensagem));
    }

    public static ResponseEntity<Map<String, String>> conflict(String mensagem) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(erroMap(mensagem));
    }

    public static ResponseEntity<Map<String, String>> unprocessable(String mensagem) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(erroMap(mensagem));
    }

    public static ResponseEntity<Map<String, String>> internalError(String mensagem) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erroMap(mensagem));
    }

    public static <T> ResponseEntity<T> of(HttpStatus status, T body) {
        return ResponseEntity.status(status).body(body);
    }

    public static ResponseEntity<Map<String, String>> sucesso(String mensagem) {
        return ResponseEntity.ok(Map.of("mensagem", mensagem));
    }

    // =======================
    // UTILITÁRIOS GERAIS
    // =======================

    public static Map<String, String> erroMap(String mensagem) {
        return Map.of("erro", mensagem);
    }

    private static boolean isValidIp(String ip) {
        return ip != null && !ip.isBlank() && !UNKNOWN_IP.equalsIgnoreCase(ip);
    }

    public static String getLocalHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.warn("Não foi possível obter o hostname do servidor: {}", e.getMessage());
            return "unknown-host";
        }
    }

    /**
     * Verifica se o IP da requisição está na lista de IPs permitidos (whitelist).
     *
     * @param request    requisição atual
     * @param allowedIps lista de IPs permitidos
     * @return true se o IP do cliente estiver na lista
     */
    public static boolean isIpAllowed(HttpServletRequest request, List<String> allowedIps) {
        String clientIp = getClientIp(request);
        boolean allowed = allowedIps.contains(clientIp);
        if (allowed == false) {
            log.warn("Acesso bloqueado para IP não autorizado: {}", clientIp);
        }
        return allowed;
    }

    /**
     * Gera um log para auditoria.
     * Formato: METHOD PATH | IP: xxx | UA: xxx
     */
    public static void logAcesso(HttpServletRequest request) {
        log.info("{} {} | IP: {} | UA: {}",
                getHttpMethod(request),
                getRequestPath(request),
                getClientIp(request),
                getUserAgent(request));
    }

    /**
     * Verifica se Content-Type é application/json.
     */
    public static boolean isJsonRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.contains("application/json");
    }

    public static Map<String, String> getQueryParams(HttpServletRequest request) {
        Map<String, String> params = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });
        return Collections.unmodifiableMap(params);
    }

    public static boolean hasQueryParam(HttpServletRequest request, String param) {
        return request.getParameter(param) != null;
    }

    public static Optional<String> getOrigin(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("Origin"));
    }

    public static Optional<String> getReferer(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("Referer"));
    }
}
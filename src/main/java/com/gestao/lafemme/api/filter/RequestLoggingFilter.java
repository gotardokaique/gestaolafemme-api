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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@Order(1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static final String ANON = "anônimo";

    private static final Set<String> SILENT_PATHS = Set.of(
            "/actuator/health",
            "/favicon.ico");

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/mp/webhook/mercadopago",
            "/mp/callback",
            "/mp/autorizar");

    private static final List<String> PUBLIC_PREFIXES = List.of();

    private static final List<Pattern> MALICIOUS_PATTERNS = List.of(

            // ── Path Traversal ──────────────────────────────────────────────────────
            Pattern.compile("\\.\\./|%2e%2e%2f|%2e%2e/|\\.%2e/|%2e\\./|\\.\\.%5c|%252e%252e",
                    Pattern.CASE_INSENSITIVE),

            // ── SQL Injection ────────────────────────────────────────────────────────
            // Keywords com espaço/comentário após
            Pattern.compile(
                    "\\b(select|union|insert|drop|update|delete|truncate|exec|execute|declare|cast|convert|char|nchar|varchar)\\b[\\s+/*]",
                    Pattern.CASE_INSENSITIVE),
            // Comentários SQL clássicos
            Pattern.compile("(--|#|/\\*|\\*/|;--)",
                    Pattern.CASE_INSENSITIVE),
            // Tautologias (1=1, 'a'='a')
            Pattern.compile("('\\s*(or|and)\\s*'?\\d)|((\\d+)\\s*=\\s*\\2)|(''\\s*=\\s*'')",
                    Pattern.CASE_INSENSITIVE),
            // Encoded SQLi
            Pattern.compile("%27|%22|%3b|%2d%2d|%23",
                    Pattern.CASE_INSENSITIVE),

            // ── XSS ─────────────────────────────────────────────────────────────────
            Pattern.compile("<\\s*script|</\\s*script|javascript\\s*:|vbscript\\s*:|data\\s*:text/html",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("on(error|load|click|mouseover|focus|blur|submit|input|change|keydown|keyup)\\s*=",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("(%3c|%3e|%22|%27|&#x?[0-9a-f]+;)",
                    Pattern.CASE_INSENSITIVE),
            // SVG/IMG vetores
            Pattern.compile("<\\s*(img|svg|iframe|object|embed|link|meta)[^>]*(src|href|action)\\s*=",
                    Pattern.CASE_INSENSITIVE),

            // ── Template/Expression Injection (SSTI, EL) ─────────────────────────────
            Pattern.compile("\\$\\{.+\\}|#\\{.+\\}|\\{\\{.+\\}\\}|<%.*%>|\\[\\[.+\\]\\]",
                    Pattern.CASE_INSENSITIVE),
            // Spring EL, OGNL
            Pattern.compile("(Runtime|ProcessBuilder|getRuntime|exec\\s*\\(|forName\\s*\\()",
                    Pattern.CASE_INSENSITIVE),

            // ── LFI / RFI ────────────────────────────────────────────────────────────
            Pattern.compile(
                    "(etc/passwd|etc/shadow|etc/hosts|proc/self/environ|proc/version|windows/win\\.ini|boot\\.ini|system32/drivers)",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("(file://|php://|zip://|data://|expect://|phar://)",
                    Pattern.CASE_INSENSITIVE),

            // ── Command Injection ────────────────────────────────────────────────────
            Pattern.compile("[;&|`]\\s*(ls|cat|id|whoami|uname|curl|wget|bash|sh|cmd|powershell|nc|ncat|ping|nslookup)",
                    Pattern.CASE_INSENSITIVE),
            Pattern.compile("(%7c|%26|%3b|%60)\\s*(ls|cat|id|whoami|curl|wget)",
                    Pattern.CASE_INSENSITIVE),

            // ── SSRF ─────────────────────────────────────────────────────────────────
            Pattern.compile("(127\\.0\\.0\\.1|localhost|0\\.0\\.0\\.0|169\\.254\\.169\\.254|::1|0x7f000001)",
                    Pattern.CASE_INSENSITIVE),
            // Metadata clouds (AWS, GCP, Azure)
            Pattern.compile("(169\\.254\\.169\\.254|metadata\\.google|metadata\\.azure)",
                    Pattern.CASE_INSENSITIVE),

            // ── XXE / XML ────────────────────────────────────────────────────────────
            Pattern.compile("<!\\s*entity|<!\\s*doctype|SYSTEM\\s+['\"]|PUBLIC\\s+['\"]",
                    Pattern.CASE_INSENSITIVE),

            // ── Null byte / encoding tricks ──────────────────────────────────────────
            Pattern.compile("%00|\\\\x00|\\\\u0000"),

            // ── HTTP Response Splitting ──────────────────────────────────────────────
            Pattern.compile("%0d%0a|%0a%0d|\\r\\n|%0d|%0a",
                    Pattern.CASE_INSENSITIVE),

            // ── Log4Shell e variantes ────────────────────────────────────────────────
            Pattern.compile("\\$\\{(jndi|lower|upper|:-|:+)[^}]*\\}",
                    Pattern.CASE_INSENSITIVE),

            // ── Open Redirect ────────────────────────────────────────────────────────
            Pattern.compile("(next|redirect|url|return|goto|target|dest)=https?://(?!gestaolafemme\\.com\\.br)",
                    Pattern.CASE_INSENSITIVE));

    private static final List<String> SCANNER_AGENTS = List.of(
            "sqlmap", "nikto", "nmap", "masscan", "zgrab",
            "nuclei", "dirbuster", "gobuster", "wfuzz", "burpsuite");

    private static final int MAX_URI_LENGTH = 512;

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

        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullUrl = queryString != null ? uri + "?" + queryString : uri;
        String ip = HttpUtils.getClientIp(request);
        String method = request.getMethod();

        boolean isPublic = isPublicRoute(uri);
        String token = resolveToken(request);
        String usuario = resolveEmail(token);

        MaliciousReason reason = detectMalicious(request, uri, queryString);
        if (reason != null) {
            log.warn("[SECURITY] Requisição suspeita bloqueada | Motivo: {} | IP: {} | {} {} | Usuário: {}",
                    reason, ip, method, fullUrl, usuario);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        chain.doFilter(request, response);

        log.info("[{}][{}] {} | IP: {} | Usuário: {} | Status: {}",
                isPublic ? "PÚBLICO" : "PRIVADO",
                method,
                fullUrl,
                ip,
                usuario,
                response.getStatus());
    }

    private String resolveEmail(String token) {
        if (token == null)
            return ANON;
        try {
            String email = tokenService.validateToken(token);
            return (email != null && !email.isBlank()) ? email : ANON;
        } catch (Exception e) {
            return ANON;
        }
    }

    private boolean isPublicRoute(String uri) {
        if (PUBLIC_PATHS.contains(uri))
            return true;
        return PUBLIC_PREFIXES.stream().anyMatch(uri::startsWith);
    }

    private MaliciousReason detectMalicious(HttpServletRequest request, String uri, String query) {

        if (uri.length() > MAX_URI_LENGTH)
            return MaliciousReason.URI_TOO_LONG;

        String ua = request.getHeader("User-Agent");
        if (ua != null && SCANNER_AGENTS.stream().anyMatch(ua.toLowerCase()::contains))
            return MaliciousReason.SCANNER_USER_AGENT;

        String raw = uri + (query != null ? "?" + query : "");
        String decoded;
        try {
            decoded = URLDecoder.decode(raw, StandardCharsets.UTF_8);
        } catch (Exception e) {
            decoded = raw;
        }

        // double-decode
        String doubleDecoded;
        try {
            doubleDecoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            doubleDecoded = decoded;
        }

        // checa nas camadas
        String combined = raw + " " + decoded + " " + doubleDecoded;

        for (Pattern pattern : MALICIOUS_PATTERNS) {
            if (pattern.matcher(combined).find())
                return MaliciousReason.MALICIOUS_PATTERN;
        }

        return null;
    }

    private String resolveToken(HttpServletRequest request) {
        try {
            return tokenService.getTokenFromRequest(request);
        } catch (Exception e) {
            return null;
        }
    }

    private enum MaliciousReason {
        URI_TOO_LONG,
        SCANNER_USER_AGENT,
        MALICIOUS_PATTERN
    }
}
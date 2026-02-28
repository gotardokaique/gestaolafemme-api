package com.gestao.lafemme.api.security.controller;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.gestao.lafemme.api.db.Condicao;
import com.gestao.lafemme.api.db.DAOController;
import com.gestao.lafemme.api.db.TransactionDB;
import com.gestao.lafemme.api.entity.PerfilUsuario;
import com.gestao.lafemme.api.entity.Unidade;
import com.gestao.lafemme.api.entity.Usuario;
import com.gestao.lafemme.api.entity.UsuarioUnidade;
import com.gestao.lafemme.api.services.exceptions.BusinessException;
import com.gestao.lafemme.api.services.exceptions.NotFoundException;

@Component
public class RegisterUserBO {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService tokenService;

    private static final int MAX_TENTATIVAS_LOGIN = 5;

    private static final List<Long> BLOQUEIO_PROGRESSIVO_MINUTOS = List.of(2L, 15L, 60L, 1440L);

    @Value("${api.security.jwt.expiration-ms}")
    private Long jwtExpirationMs;

    @Value("${app.security.require-https:true}")
    private boolean requireHttps;

    @Value("${app.security.trusted-proxies:}")
    private String trustedProxiesCsv;

    private static final long TENTATIVA_TTL_MINUTOS = 30;
    private static final long BLOQUEIO_TTL_MINUTOS = 1500;

    private static final java.util.regex.Pattern REGEX_EMAIL = java.util.regex.Pattern
            .compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final UsuarioServiceValidacao usuarioServiceValidacao;
    private final TransactionDB trans;
    private final DAOController dao;

    public RegisterUserBO(UsuarioServiceValidacao usuarioServiceValidacao, TransactionDB trans, DAOController dao) {
        this.usuarioServiceValidacao = usuarioServiceValidacao;
        this.trans = trans;
        this.dao = dao;
    }

    public Boolean validarSenhaForte(String senha) {
        if (senha == null) return false;
        if (senha.length() < 8) return false;

        boolean temMaiuscula = senha.matches(".*[A-Z].*");
        boolean temMinuscula = senha.matches(".*[a-z].*");
        boolean temNumero = senha.matches(".*[0-9].*");

        if (!temMaiuscula || !temMinuscula || !temNumero) return false;

        // evita repetições tipo "aaa" / "111"
        if (senha.matches(".*(.)\\1{2,}.*")) return false;

        return true;
    }

    public Boolean isEmailJaRegistrado(String email) {
        return usuarioServiceValidacao.validarEmailJaCadastrado(email);
    }

    public Boolean cadastrarUsuario(String nome, String email, String hashed, Long perfilUsuarioId) {
        boolean isUserCadastrado;

        PerfilUsuario perfil = trans.selectById(PerfilUsuario.class, perfilUsuarioId);
        var usuario = new Usuario(nome, email, hashed, perfil);

        try {
            trans.insert(usuario);
            isUserCadastrado = true;
        } catch (Exception e) {
            isUserCadastrado = false;
        }

        return isUserCadastrado;
    }

    public ResponseEntity<?> processarLogin(String emailRaw, String senha, jakarta.servlet.http.HttpServletRequest request) {

        String email = (emailRaw == null ? "" : emailRaw).trim().toLowerCase();

        if (!REGEX_EMAIL.matcher(email).matches()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Formato de e-mail inválido"));
        }

        String ip = extrairIpCliente(request);

        String keyEmail = "login:attempt:email:" + email;
        String keyIpEmail = "login:attempt:ip_email:" + ip + ":" + email;
        String keyBloqueioEmail = "login:block:email:" + email;
        String keyBloqueioIpEmail = "login:block:ip_email:" + ip + ":" + email;
        String keyBlockCountEmail = "login:blockcount:email:" + email;
        String keyBlockCountIpEmail = "login:blockcount:ip_email:" + ip + ":" + email;

        boolean bloqueadoEmail = Boolean.TRUE.equals(redisTemplate.hasKey(keyBloqueioEmail));
        boolean bloqueadoIpEmail = Boolean.TRUE.equals(redisTemplate.hasKey(keyBloqueioIpEmail));

        if (bloqueadoEmail || bloqueadoIpEmail) {
            logger.warn("Login bloqueado para {} (IP: {})", email, ip);

            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Muitas tentativas. Aguarde e tente novamente."));
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, senha)
            );

            Usuario user = (Usuario) auth.getPrincipal();

            Unidade unidadeAtiva = resolverUnidadeAtiva(user.getId());
            user.setUnidadeAtiva(unidadeAtiva);

            // Sucesso → limpa
            redisTemplate.delete(keyEmail);
            redisTemplate.delete(keyIpEmail);
            redisTemplate.delete(keyBloqueioEmail);
            redisTemplate.delete(keyBloqueioIpEmail);
            redisTemplate.delete(keyBlockCountEmail);
            redisTemplate.delete(keyBlockCountIpEmail);

            String jwt = tokenService.generateToken(user);
            ResponseCookie cookie = ResponseCookie.from("auth_token", jwt)
                    .httpOnly(true)
                    .secure(requireHttps)
                    .sameSite(requireHttps ? "Strict" : "Lax")
                    .path("/")
                    .maxAge(Duration.ofMillis(jwtExpirationMs))
                    .build();

            logger.info("Login bem-sucedido para {} (IP: {})", email, ip);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(Map.of("success", true, "message", "Login realizado"));

        } catch (BadCredentialsException | UsernameNotFoundException e) {

            Long tentEmail = incrementarAtomico(keyEmail, TENTATIVA_TTL_MINUTOS);
            Long tentIpEmail = incrementarAtomico(keyIpEmail, TENTATIVA_TTL_MINUTOS);

            if (tentEmail >= MAX_TENTATIVAS_LOGIN || tentIpEmail >= MAX_TENTATIVAS_LOGIN) {
                aplicarBloqueioProgressivo(keyBloqueioEmail, keyBlockCountEmail);
                aplicarBloqueioProgressivo(keyBloqueioIpEmail, keyBlockCountIpEmail);

                redisTemplate.delete(keyEmail);
                redisTemplate.delete(keyIpEmail);
            }

            logger.warn("Credenciais inválidas para {} (IP: {}) tentEmail={} tentIpEmail={}",
                    email, ip, tentEmail, tentIpEmail);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Usuário ou senha inválidos"));

        } catch (Exception e) {
            logger.error("Erro inesperado ao autenticar {} (IP: {}): {}", email, ip, e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erro interno ao tentar autenticar."));
        }
    }

    private Long incrementarAtomico(String key, long ttlMinutos) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, ttlMinutos, TimeUnit.MINUTES);
        }
        return count != null ? count : 0L;
    }

    private void aplicarBloqueioProgressivo(String keyBloqueio, String keyBlockCount) {
        Long blockCount = redisTemplate.opsForValue().increment(keyBlockCount);
        if (blockCount != null && blockCount == 1L) {
            redisTemplate.expire(keyBlockCount, BLOQUEIO_TTL_MINUTOS, TimeUnit.MINUTES);
        }

        int index = (blockCount != null)
                ? Math.min((int) (blockCount - 1), BLOQUEIO_PROGRESSIVO_MINUTOS.size() - 1)
                : 0;
        long duracaoMinutos = BLOQUEIO_PROGRESSIVO_MINUTOS.get(index);

        redisTemplate.opsForValue().set(keyBloqueio, "1", Duration.ofMinutes(duracaoMinutos));

        logger.warn("Bloqueio aplicado: key={} duração={}min (bloqueio #{})", keyBloqueio, duracaoMinutos, blockCount);
    }

    private Unidade resolverUnidadeAtiva(Long userId) {
        try {
            UsuarioUnidade uu = dao.select()
                    .from(UsuarioUnidade.class)
                    .join("usuario")
                    .join("unidade")
                    .where("usuario.id", Condicao.EQUAL, userId)
                    .where("unidade.ativo", Condicao.EQUAL, true)
                    .limit(1)
                    .one();

            if (uu == null || uu.getUnidade() == null) {
                throw new NotFoundException("Usuário não possui unidade vinculada.");
            }

            return uu.getUnidade();

        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Erro ao resolver unidade do usuário.");
        }
    }

    private String extrairIpCliente(jakarta.servlet.http.HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        Set<String> trustedProxies = parseTrustedProxies();

        logger.warn("[PROXY-DEBUG] remoteAddr={} | X-Forwarded-For={} | trustedProxies={} | isTrusted={}",
                remoteAddr, xForwardedFor, trustedProxies, trustedProxies.contains(remoteAddr));

        if (trustedProxies.contains(remoteAddr)) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String[] parts = forwarded.split(",");
                if (parts.length > 0) {
                    return parts[0].trim();
                }
            }

            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
        }

        return remoteAddr;
    }

    private Set<String> parseTrustedProxies() {
        if (trustedProxiesCsv == null || trustedProxiesCsv.isBlank()) {
            return Set.of();
        }

        String[] parts = trustedProxiesCsv.split(",");
        java.util.Set<String> set = new java.util.HashSet<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                set.add(trimmed);
            }
        }
        return set;
    }
}


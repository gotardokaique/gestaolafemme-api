package com.gestao.lafemme.api.security.controller;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
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

    @Autowired
    private SessionService sessionService;

    private static final int MAX_TENTATIVAS_LOGIN = 5;
    private static final long BLOQUEIO_MINUTOS = 2;
    
    @Value("${api.security.jwt.expiration-ms}")
    private Long jwtExpirationMs;
    
    @Value("${app.security.require-https:true}")
    private boolean requireHttps;

    // TTL pra não deixar lixo infinito no Redis
    private static final long TENTATIVA_TTL_MINUTOS = 30;

    private static final java.util.regex.Pattern REGEX_EMAIL = java.util.regex.Pattern
            .compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    // Mantive a classe (você pediu pra não remover)
    private static class TentativaLogin {
        int tentativas;
        LocalDateTime bloqueadoAte;
        LocalDateTime ultimaTentativa;

        TentativaLogin() {
            this.tentativas = 0;
            this.bloqueadoAte = null;
            this.ultimaTentativa = LocalDateTime.now();
        }
    }

    // Chaves no Redis
    private static final String LOGIN_ATTEMPT_EMAIL_PREFIX = "login:attempt:email:";

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
        // boolean temEspecial = senha.matches(".*[^a-zA-Z0-9].*");

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

    // ===================== LOGIN (AGORA FUNCIONAL + REDIS) =====================

    public ResponseEntity<?> processarLogin(String emailRaw, String senha, jakarta.servlet.http.HttpServletRequest request) {

        String email = (emailRaw == null ? "" : emailRaw).trim().toLowerCase();

        if (!REGEX_EMAIL.matcher(email).matches()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Formato de e-mail inválido"));
        }

        String ip = extrairIpCliente(request);
        LocalDateTime agora = LocalDateTime.now();

        String keyEmail = "login:attempt:email:" + email;
        String keyIpEmail = "login:attempt:ip_email:" + ip + ":" + email;

        TentativaLogin tentativaEmail = carregarTentativa(keyEmail);
        TentativaLogin tentativaIpEmail = carregarTentativa(keyIpEmail);

        tentativaEmail.ultimaTentativa = agora;
        tentativaIpEmail.ultimaTentativa = agora;

        boolean bloqueadoEmail =
                tentativaEmail.bloqueadoAte != null &&
                tentativaEmail.bloqueadoAte.isAfter(agora);

        boolean bloqueadoIpEmail =
                tentativaIpEmail.bloqueadoAte != null &&
                tentativaIpEmail.bloqueadoAte.isAfter(agora);

        if (bloqueadoEmail || bloqueadoIpEmail) {
            salvarTentativa(keyEmail, tentativaEmail);
            salvarTentativa(keyIpEmail, tentativaIpEmail);

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

            // sucesso -> limpa tentativas
            redisTemplate.delete(keyEmail);
            redisTemplate.delete(keyIpEmail);

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

            tentativaEmail.tentativas++;
            tentativaIpEmail.tentativas++;

            if (tentativaEmail.tentativas >= MAX_TENTATIVAS_LOGIN ||
                tentativaIpEmail.tentativas >= MAX_TENTATIVAS_LOGIN) {

                LocalDateTime bloqueio = agora.plusMinutes(BLOQUEIO_MINUTOS);
                tentativaEmail.bloqueadoAte = bloqueio;
                tentativaIpEmail.bloqueadoAte = bloqueio;
            }

            salvarTentativa(keyEmail, tentativaEmail);
            salvarTentativa(keyIpEmail, tentativaIpEmail);

            logger.warn("Credenciais inválidas para {} (IP: {}) tentEmail={} tentIpEmail={}",
                    email, ip,
                    tentativaEmail.tentativas,
                    tentativaIpEmail.tentativas);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Usuário ou senha inválidos"));

        } catch (Exception e) {
            logger.error("Erro inesperado ao autenticar {} (IP: {}): {}", email, ip, e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erro interno ao tentar autenticar."));
        }
    }

    private String keyTentativaEmail(String email) {
        return LOGIN_ATTEMPT_EMAIL_PREFIX + email;
    }

    private TentativaLogin carregarTentativa(String key) {
        Map<Object, Object> map = redisTemplate.opsForHash().entries(key);

        TentativaLogin t = new TentativaLogin();

        if (!map.isEmpty()) {
            String tentStr = (String) map.get("tentativas");
            String bloqueadoStr = (String) map.get("bloqueadoAte");
            String ultimaStr = (String) map.get("ultimaTentativa");

            if (tentStr != null) {
                try { t.tentativas = Integer.parseInt(tentStr); } catch (Exception ignored) { }
            }

            if (bloqueadoStr != null && !bloqueadoStr.isBlank()) {
                try { t.bloqueadoAte = LocalDateTime.parse(bloqueadoStr); } catch (Exception ignored) { }
            }

            if (ultimaStr != null && !ultimaStr.isBlank()) {
                try { t.ultimaTentativa = LocalDateTime.parse(ultimaStr); } catch (Exception ignored) { }
            }
        }

        return t;
    }

    private void salvarTentativa(String key, TentativaLogin tentativa) {
        redisTemplate.opsForHash().put(key, "tentativas", String.valueOf(tentativa.tentativas));
        redisTemplate.opsForHash().put(key, "ultimaTentativa", tentativa.ultimaTentativa.toString());

        if (tentativa.bloqueadoAte != null) {
            redisTemplate.opsForHash().put(key, "bloqueadoAte", tentativa.bloqueadoAte.toString());
        } else {
            redisTemplate.opsForHash().delete(key, "bloqueadoAte");
        }

        redisTemplate.expire(key, TENTATIVA_TTL_MINUTOS, TimeUnit.MINUTES);
    }

    private void resetTentativa(String key) {
        redisTemplate.delete(key);
    }

    // ===================== UNIDADE ATIVA =====================

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

        return request.getRemoteAddr();
    }

}

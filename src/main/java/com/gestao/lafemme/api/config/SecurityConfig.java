package com.gestao.lafemme.api.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.gestao.lafemme.api.db.Condicao;
import com.gestao.lafemme.api.db.DAOController;
import com.gestao.lafemme.api.entity.Usuario;
import com.gestao.lafemme.api.security.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final DAOController daoController;

    @Value("${app.cors.allowed-origins:}")
    private String allowedOriginsCsv;

    @Value("${app.security.require-https:true}")
    private boolean requireHttps;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, DAOController daoController) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.daoController = daoController;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationProvider authenticationProvider) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .rememberMe(AbstractHttpConfigurer::disable);

        if (requireHttps) {
            http.requiresChannel(channel ->
                channel.anyRequest().requiresSecure()
            );
        }


        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(401);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"message\":\"Não autorizado\"}");
                })
                .accessDeniedHandler((req, res, e) -> {
                    res.setStatus(403);
                    res.setContentType("application/json");
                    res.getWriter().write("{\"message\":\"Acesso negado\"}");
                })
            )

            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(Customizer.withDefaults())
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)
                )
            );

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        final String DUMMY_HASH = "$argon2id$v=19$m=65536,t=5,p=2$W6xM2Kftt/BSVv45LwZVfw$zMLxp6E1IhLbu/6x07H+8odCck+ZdOo5t6Jw45hPBM+Bs3/3h3lRGyl5FmUclwRj7+8";

        return new AuthenticationProvider() {

            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                String username = authentication.getName();
                String senhaRaw = authentication.getCredentials() != null
                        ? authentication.getCredentials().toString()
                        : "";

                if (StringUtils.hasText(username) == false) {
                    passwordEncoder.matches(senhaRaw, DUMMY_HASH);
                    throw new BadCredentialsException("Usuário ou senha inválidos.");
                }

                String email = username.trim().toLowerCase();

                if (email.length() > 120 || senhaRaw.length() > 120) {
                    passwordEncoder.matches(senhaRaw, DUMMY_HASH);
                    throw new BadCredentialsException("Usuário ou senha inválidos.");
                }

                Usuario usuario;
                try {
                    usuario = daoController
                            .select()
                            .from(Usuario.class)
                            .where("email", Condicao.EQUAL, email)
                            .limit(1)
                            .one();
                } catch (Exception e) {
                    passwordEncoder.matches(senhaRaw, DUMMY_HASH);
                    throw new BadCredentialsException("Usuário ou senha inválidos.");
                }

                if (passwordEncoder.matches(senhaRaw, usuario.getSenha()) == false) {
                    throw new BadCredentialsException("Usuário ou senha inválidos.");
                }

                if (!usuario.isAccountNonLocked() || !usuario.isEnabled()) {
                    throw new BadCredentialsException("Usuário ou senha inválidos.");
                }

                return new UsernamePasswordAuthenticationToken(
                        usuario,
                        null,
                        usuario.getAuthorities()
                );
            }

            @Override
            public boolean supports(Class<?> authentication) {
                return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
            }
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> allowedOrigins = parseAllowedOrigins(allowedOriginsCsv);

        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Accept", "Origin", "X-Requested-With"));
        config.setAllowCredentials(true);

        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private List<String> parseAllowedOrigins(String csv) {
        if (!StringUtils.hasText(csv)) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }    
}

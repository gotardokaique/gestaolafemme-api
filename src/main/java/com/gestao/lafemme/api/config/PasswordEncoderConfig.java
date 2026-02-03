package com.gestao.lafemme.api.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        int saltLength = 16;        // bytes
        int hashLength = 50;        // bytes
        int parallelism = 2;        // threads
        int memory = 65536;         // 64 MB
        int iterations = 5;         // custo de tempo

        return new Argon2PasswordEncoder(
                saltLength,
                hashLength,
                parallelism,
                memory,
                iterations
        );
    }
}

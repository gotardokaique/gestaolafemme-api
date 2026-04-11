package com.gestao.lafemme.api.config;

import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import com.gen.core.generator.CrudGenerator;
import com.gestao.lafemme.api.entity.Usuario;

public class Generator {

    public static void main(String[] args) {
        if (isDeveloper() == false) {
            throw new IllegalStateException("Sistema não está em modo desenvolvedor");
        }

        // hashUserSenha();
        CrudGenerator.run(Usuario.class);
        ;
    }

    private static boolean isDeveloper() {
        try (InputStream input = Generator.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                return false;
            }
            Properties prop = new Properties();
            prop.load(input);
            return "true".equalsIgnoreCase(prop.getProperty("system.developer", "false"));
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private static void hashUserSenha() {
        int saltLength = 16;
        int hashLength = 50;
        int parallelism = 2;
        int memory = 65536;
        int iterations = 5;

        Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(saltLength, hashLength, parallelism, memory,
                iterations);

        try (Scanner scan = new Scanner(System.in)) {
            System.out.println("Digite a senha: ");
            String hash = encoder.encode(scan.nextLine());
            System.out.println("Hash: " + hash);
        }
    }

}

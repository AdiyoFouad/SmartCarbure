package fr.miage.smartcarbure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Configure Spring Security : routes publiques, routes réservées aux utilisateurs connectés et filtre du jeton. */
@Configuration
public class SecurityConfig {

    /** Outil de hachage des mots de passe (BCrypt). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
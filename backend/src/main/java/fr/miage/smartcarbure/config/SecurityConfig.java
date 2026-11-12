package fr.miage.smartcarbure.config;

import fr.miage.smartcarbure.repository.UtilisateurRepository;
import fr.miage.smartcarbure.service.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/** Configure Spring Security : routes publiques, routes réservées aux utilisateurs connectés et filtre du jeton. */
@Configuration
public class SecurityConfig {

    /** Outil de hachage des mots de passe (BCrypt). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filtresDeSecurite(HttpSecurity http,
                                                 JwtService jwtService,
                                                 UtilisateurRepository utilisateurRepository,
                                                 ReponseErreurSecurite erreurSecurite) throws Exception {
        http
                // Pas de cookie de session : l'authentification passe par le jeton, donc pas de CSRF.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(routes -> routes
                        // Routes réservées aux utilisateurs connectés (voir contrat d'API)
                        .requestMatchers("/api/favoris", "/api/favoris/**",
                                "/api/preferences", "/api/preferences/**").authenticated()
                        // Tout le reste est public (recherche, fiche, trajet, comparaison, économie, auth)
                        .anyRequest().permitAll())
                .exceptionHandling(erreurs -> erreurs
                        .authenticationEntryPoint(erreurSecurite)
                        .accessDeniedHandler(erreurSecurite))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService, utilisateurRepository),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
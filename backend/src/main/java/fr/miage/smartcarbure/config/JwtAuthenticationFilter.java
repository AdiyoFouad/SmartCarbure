package fr.miage.smartcarbure.config;

import fr.miage.smartcarbure.repository.UtilisateurRepository;
import fr.miage.smartcarbure.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lit le jeton JWT de l'en-tête Authorization. Si le jeton est valide et que l'utilisateur existe,
 * la requête est marquée « connectée » ; l'identifiant de l'utilisateur sert de principal.
 * Sinon, la requête reste anonyme : une route protégée répondra alors 401.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXE = "Bearer ";

    private final JwtService jwtService;
    private final UtilisateurRepository utilisateurRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UtilisateurRepository utilisateurRepository) {
        this.jwtService = jwtService;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requete, HttpServletResponse reponse,
                                    FilterChain chaine) throws ServletException, IOException {
        String entete = requete.getHeader(HttpHeaders.AUTHORIZATION);

        if (entete != null && entete.startsWith(PREFIXE)) {
            String jeton = entete.substring(PREFIXE.length()).trim();
            try {
                Long idUtilisateur = jwtService.extraireIdUtilisateur(jeton);
                if (utilisateurRepository.existsById(idUtilisateur)) {
                    UsernamePasswordAuthenticationToken authentification =
                            new UsernamePasswordAuthenticationToken(idUtilisateur, null, List.of());
                    SecurityContextHolder.getContext().setAuthentication(authentification);
                }
            } catch (JwtException | IllegalArgumentException e) {
                // Jeton invalide, expiré ou mal formé : on laisse la requête anonyme.
            }
        }
        chaine.doFilter(requete, reponse);
    }
}
package fr.miage.smartcarbure.config;

import fr.miage.smartcarbure.dto.ErreurDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Réponse de Spring Security quand l'accès est refusé. Le contrat d'API impose toujours 401
 * (jamais 403), avec le corps { "statut": 401, "message": "..." }.
 */
@Component
public class ReponseErreurSecurite implements AuthenticationEntryPoint, AccessDeniedHandler {

    static final String MESSAGE = "Authentification requise : jeton absent, invalide ou expiré.";

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** Pas de jeton, ou jeton invalide ou expiré. */
    @Override
    public void commence(HttpServletRequest requete, HttpServletResponse reponse,
                         AuthenticationException exception) throws IOException {
        ecrire401(reponse);
    }

    /** Accès interdit : on répond aussi 401, comme le demande le contrat. */
    @Override
    public void handle(HttpServletRequest requete, HttpServletResponse reponse,
                       AccessDeniedException exception) throws IOException {
        ecrire401(reponse);
    }

    private void ecrire401(HttpServletResponse reponse) throws IOException {
        reponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        reponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        reponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        reponse.getWriter().write(JSON.writeValueAsString(new ErreurDTO(401, MESSAGE)));
    }
}
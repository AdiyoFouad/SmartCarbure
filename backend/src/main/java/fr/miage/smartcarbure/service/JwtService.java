package fr.miage.smartcarbure.service;

import fr.miage.smartcarbure.model.Utilisateur;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** Génère et vérifie les jetons d'authentification (JWT) des utilisateurs connectés. */
@Service
public class JwtService {

    private final SecretKey cle;
    private final Duration dureeValidite;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-heures}") long heures) {
        this.cle = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.dureeValidite = Duration.ofHours(heures);
    }

    /** Fabrique un jeton signé contenant l'id de l'utilisateur, valable pour la durée configurée. */
    public String genererJeton(Utilisateur utilisateur) {
        Instant maintenant = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(utilisateur.getId()))
                .claim("email", utilisateur.getEmail())
                .issuedAt(Date.from(maintenant))
                .expiration(Date.from(maintenant.plus(dureeValidite)))
                .signWith(cle)
                .compact();
    }

    /**
     * Vérifie la signature et la date d'expiration du jeton, et renvoie l'id de l'utilisateur.
     * Lève JwtException si le jeton est invalide ou expiré (utilisé par le filtre de L4-04).
     */
    public Long extraireIdUtilisateur(String jeton) throws JwtException {
        String sujet = Jwts.parser()
                .verifyWith(cle)
                .build()
                .parseSignedClaims(jeton)
                .getPayload()
                .getSubject();
        return Long.valueOf(sujet);
    }
}
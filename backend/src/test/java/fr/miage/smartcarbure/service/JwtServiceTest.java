package fr.miage.smartcarbure.service;

import fr.miage.smartcarbure.model.Utilisateur;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires de JwtService (L4-07) :
 * - Génération de jeton JWT
 * - Extraction et vérification de l'ID utilisateur
 * - Rejet des jetons altérés ou invalides
 */
class JwtServiceTest {

    private static final String SECRET_TEST = "cle-secrete-de-test-tres-longue-pour-hmac-sha256-smartcarbure-2026";
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET_TEST, 24);
    }

    @Test
    @DisplayName("génère un jeton JWT contenant l'identifiant et l'email de l'utilisateur")
    void genererJeton_succes() {
        Utilisateur utilisateur = new Utilisateur("Camille", "camille@exemple.fr", "hash");
        utilisateur.setId(42L);

        String jeton = jwtService.genererJeton(utilisateur);

        assertThat(jeton).isNotBlank();
        Long idExtrait = jwtService.extraireIdUtilisateur(jeton);
        assertThat(idExtrait).isEqualTo(42L);
    }

    @Test
    @DisplayName("lève une JwtException si le jeton est altéré ou corrompu")
    void extraireIdUtilisateur_jetonInvalide_leveException() {
        String jetonInvalide = "eyJhbGciOiJIUzI1NiJ9.corrompu.faux";

        assertThatThrownBy(() -> jwtService.extraireIdUtilisateur(jetonInvalide))
                .isInstanceOf(JwtException.class);
    }
}

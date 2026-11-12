package fr.miage.smartcarbure.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.miage.smartcarbure.enums.Carburant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tests de GlobalExceptionHandler sans démarrer Spring Boot ni la base de données :
 * un petit contrôleur de test lève chaque exception, MockMvc vérifie le code HTTP et le JSON exact.
 */
class GlobalExceptionHandlerTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private MockMvc mockMvc;

    @BeforeEach
    void creerMockMvc() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ControleurDeTest())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ------------------------------------------------------------------
    // Contrôleur et exceptions de test
    // ------------------------------------------------------------------

    @ResponseStatus(HttpStatus.CONFLICT)
    static class ConflitDeTestException extends RuntimeException {
        ConflitDeTestException(String message) {
            super(message);
        }
    }

    record CorpsDeTest(@NotBlank(message = "ne doit pas être vide") String email,
                       @Min(value = 1, message = "doit être supérieur ou égal à 1") int litres) {
    }

    @RestController
    @RequestMapping("/test")
    static class ControleurDeTest {

        @GetMapping("/api-indisponible")
        String apiIndisponible() {
            throw new ApiIndisponibleException("L'API Prix des carburants a répondu 400 : ODSQL query is malformed");
        }

        @GetMapping("/station-introuvable")
        String stationIntrouvable() {
            throw new StationIntrouvableException("35000001");
        }

        @GetMapping("/utilisateur-existant")
        String utilisateurExistant() {
            throw new UtilisateurDejaExistantException("jean@exemple.fr");
        }

        @GetMapping("/identifiants-invalides")
        String identifiantsInvalides() {
            throw new IdentifiantsInvalidesException();
        }

        @GetMapping("/argument-invalide")
        String argumentInvalide() {
            throw new IllegalArgumentException("Le volume du plein doit être un nombre strictement positif : -5.0");
        }

        @GetMapping("/argument-invalide-sans-message")
        String argumentInvalideSansMessage() {
            throw new IllegalArgumentException();
        }

        @GetMapping("/recherche")
        String recherche(@RequestParam Carburant carburant, @RequestParam int rayon) {
            return "ok";
        }

        @GetMapping("/rayon")
        String rayon(@RequestParam @Min(value = 1, message = "doit être supérieur ou égal à 1")
                     @Max(value = 50, message = "doit être inférieur ou égal à 50") int rayon) {
            return "ok";
        }

        @PostMapping("/corps")
        String corps(@Valid @RequestBody CorpsDeTest corps) {
            return "ok";
        }

        @GetMapping("/response-status")
        String responseStatus() {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès interdit au test.");
        }

        @GetMapping("/exception-annotee")
        String exceptionAnnotee() {
            throw new ConflitDeTestException("Cette station est déjà dans vos favoris.");
        }

        @GetMapping("/exception-annotee-sans-message")
        String exceptionAnnoteeSansMessage() {
            throw new ConflitDeTestException(null);
        }

        @GetMapping("/erreur-interne")
        String erreurInterne() {
            throw new NullPointerException("détail technique secret");
        }
    }

    // ------------------------------------------------------------------
    // Outil : vérifie le code HTTP et le JSON exact { statut, message }
    // ------------------------------------------------------------------

    private void verifier(RequestBuilder requete, int statut, String message) throws Exception {
        MvcResult resultat = mockMvc.perform(requete).andExpect(status().is(statut)).andReturn();
        String corps = resultat.getResponse().getContentAsString(StandardCharsets.UTF_8);

        JsonNode attendu = JSON.createObjectNode().put("statut", statut).put("message", message);
        assertEquals(attendu, JSON.readTree(corps), "Corps de la réponse : " + corps);
        assertEquals(MediaType.APPLICATION_JSON_VALUE,
                MediaType.parseMediaType(resultat.getResponse().getContentType()).getType() + "/"
                        + MediaType.parseMediaType(resultat.getResponse().getContentType()).getSubtype());
    }

    // ------------------------------------------------------------------
    // Exceptions métier
    // ------------------------------------------------------------------

    @Test
    void apiIndisponibleDonne503SansDetailTechnique() throws Exception {
        verifier(get("/test/api-indisponible"), 503, GlobalExceptionHandler.MESSAGE_API_INDISPONIBLE);

        String corps = mockMvc.perform(get("/test/api-indisponible")).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertFalse(corps.contains("ODSQL"));
    }

    @Test
    void stationIntrouvableDonne404AvecLId() throws Exception {
        verifier(get("/test/station-introuvable"), 404, "Station introuvable : 35000001.");
        assertEquals("35000001", new StationIntrouvableException("35000001").getIdStation());
    }

    @Test
    void utilisateurDejaExistantDonne409() throws Exception {
        verifier(get("/test/utilisateur-existant"), 409, "Un compte existe déjà avec l'email jean@exemple.fr.");
    }

    @Test
    void identifiantsInvalidesDonne401() throws Exception {
        verifier(get("/test/identifiants-invalides"), 401, "Email ou mot de passe incorrect.");
    }

    @Test
    void illegalArgumentDonne400AvecSonMessage() throws Exception {
        verifier(get("/test/argument-invalide"), 400,
                "Le volume du plein doit être un nombre strictement positif : -5.0");
    }

    @Test
    void illegalArgumentSansMessageDonne400SaisieInvalide() throws Exception {
        verifier(get("/test/argument-invalide-sans-message"), 400, "Saisie invalide.");
    }

    // ------------------------------------------------------------------
    // Erreurs de saisie détectées par Spring (400)
    // ------------------------------------------------------------------

    @Test
    void parametreManquantDonne400() throws Exception {
        verifier(get("/test/recherche").param("carburant", "GAZOLE"), 400,
                "Paramètre obligatoire manquant : rayon.");
    }

    @Test
    void carburantInconnuDonne400AvecLesValeursAcceptees() throws Exception {
        verifier(get("/test/recherche").param("carburant", "XYZ").param("rayon", "10"), 400,
                "Valeur invalide pour le paramètre carburant : XYZ. "
                        + "Valeurs acceptées : GAZOLE, SP95, SP98, E10, E85, GPLC.");
    }

    @Test
    void rayonNonNumeriqueDonne400() throws Exception {
        verifier(get("/test/recherche").param("carburant", "GAZOLE").param("rayon", "abc"), 400,
                "Valeur invalide pour le paramètre rayon : abc.");
    }

    @Test
    void rayonHorsLimitesDonne400() throws Exception {
        verifier(get("/test/rayon").param("rayon", "80"), 400, "rayon : doit être inférieur ou égal à 50");
    }

    @Test
    void corpsInvalideDonne400AvecLesChampsEnErreur() throws Exception {
        verifier(post("/test/corps").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"\", \"litres\": 50}"),
                400, "email : ne doit pas être vide");
    }

    @Test
    void jsonMalFormeDonne400() throws Exception {
        verifier(post("/test/corps").contentType(MediaType.APPLICATION_JSON).content("{pas du json"),
                400, "Corps de la requête absent ou JSON mal formé.");
    }

    @Test
    void corpsAbsentDonne400() throws Exception {
        verifier(post("/test/corps").contentType(MediaType.APPLICATION_JSON),
                400, "Corps de la requête absent ou JSON mal formé.");
    }

    // ------------------------------------------------------------------
    // Route, méthode, type de contenu
    // ------------------------------------------------------------------

    @Test
    void routeInexistanteDonne404() throws Exception {
        verifier(get("/api/inexistante"), 404, "Route inexistante : GET /api/inexistante.");
    }

    @Test
    void methodeNonAutoriseeDonne405() throws Exception {
        verifier(post("/test/station-introuvable"), 405, "Méthode POST non autorisée pour cette route.");
    }

    @Test
    void corpsQuiNEstPasDuJsonDonne415() throws Exception {
        verifier(post("/test/corps").contentType(MediaType.TEXT_PLAIN).content("bonjour"),
                415, "Le corps de la requête doit être en JSON.");
    }

    // ------------------------------------------------------------------
    // Exceptions génériques
    // ------------------------------------------------------------------

    @Test
    void responseStatusExceptionGardeSonCode() throws Exception {
        verifier(get("/test/response-status"), 403, "Accès interdit au test.");
    }

    @Test
    void exceptionAnnoteeResponseStatusDonneSonCodeEtSonMessage() throws Exception {
        verifier(get("/test/exception-annotee"), 409, "Cette station est déjà dans vos favoris.");
    }

    @Test
    void exceptionAnnoteeSansMessageDonneUnMessageGenerique() throws Exception {
        verifier(get("/test/exception-annotee-sans-message"), 409, "Conflit avec l'état actuel de la ressource.");
    }

    @Test
    void autreExceptionDonne500SansDetailTechnique() throws Exception {
        verifier(get("/test/erreur-interne"), 500, "Erreur interne du serveur.");
    }
}

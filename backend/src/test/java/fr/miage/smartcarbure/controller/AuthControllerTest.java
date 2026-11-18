package fr.miage.smartcarbure.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.miage.smartcarbure.dto.ConnexionRequest;
import fr.miage.smartcarbure.dto.InscriptionRequest;
import fr.miage.smartcarbure.dto.UtilisateurDTO;
import fr.miage.smartcarbure.exception.GlobalExceptionHandler;
import fr.miage.smartcarbure.exception.IdentifiantsInvalidesException;
import fr.miage.smartcarbure.exception.UtilisateurDejaExistantException;
import fr.miage.smartcarbure.model.Utilisateur;
import fr.miage.smartcarbure.service.JwtService;
import fr.miage.smartcarbure.service.UtilisateurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests unitaires du contrôleur REST d'authentification (L4-07).
 * Vérifie les formats JSON, les codes HTTP (200, 201, 400, 401, 409)
 * selon le contrat d'API.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UtilisateurService utilisateurService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/inscription : renvoie 201 et l'UtilisateurDTO en cas de succès")
    void inscrire_succes_renvoie201() throws Exception {
        InscriptionRequest requete = new InscriptionRequest("Camille Martin", "camille@exemple.fr", "motdepasse123");
        UtilisateurDTO reponseDTO = new UtilisateurDTO(1L, "Camille Martin", "camille@exemple.fr", null, null);

        when(utilisateurService.inscrire(any(InscriptionRequest.class))).thenReturn(reponseDTO);

        mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nom").value("Camille Martin"))
                .andExpect(jsonPath("$.email").value("camille@exemple.fr"));
    }

    @Test
    @DisplayName("POST /api/auth/inscription : renvoie 400 si validation échoue (ex. email invalide)")
    void inscrire_emailInvalide_renvoie400() throws Exception {
        InscriptionRequest requete = new InscriptionRequest("Camille Martin", "email-invalide", "motdepasse123");

        mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statut").value(400));
    }

    @Test
    @DisplayName("POST /api/auth/inscription : renvoie 409 si l'email existe déjà")
    void inscrire_emailExistant_renvoie409() throws Exception {
        InscriptionRequest requete = new InscriptionRequest("Camille Martin", "existant@exemple.fr", "motdepasse123");

        when(utilisateurService.inscrire(any(InscriptionRequest.class)))
                .thenThrow(new UtilisateurDejaExistantException("existant@exemple.fr"));

        mockMvc.perform(post("/api/auth/inscription")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statut").value(409))
                .andExpect(jsonPath("$.message").value(containsString("existant@exemple.fr")));
    }

    @Test
    @DisplayName("POST /api/auth/connexion : renvoie 200 avec jeton et utilisateur connecté")
    void connecter_succes_renvoie200() throws Exception {
        ConnexionRequest requete = new ConnexionRequest("camille@exemple.fr", "motdepasse123");
        Utilisateur utilisateur = new Utilisateur("Camille Martin", "camille@exemple.fr", "hash");
        utilisateur.setId(1L);

        when(utilisateurService.authentifier(any(ConnexionRequest.class))).thenReturn(utilisateur);
        when(jwtService.genererJeton(utilisateur)).thenReturn("jwt.token.valide");

        mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jeton").value("jwt.token.valide"))
                .andExpect(jsonPath("$.utilisateur.id").value(1))
                .andExpect(jsonPath("$.utilisateur.nom").value("Camille Martin"))
                .andExpect(jsonPath("$.utilisateur.email").value("camille@exemple.fr"));
    }

    @Test
    @DisplayName("POST /api/auth/connexion : renvoie 401 si les identifiants sont invalides")
    void connecter_identifiantsInvalides_renvoie401() throws Exception {
        ConnexionRequest requete = new ConnexionRequest("faux@exemple.fr", "mauvaisMdp");

        when(utilisateurService.authentifier(any(ConnexionRequest.class)))
                .thenThrow(new IdentifiantsInvalidesException());

        mockMvc.perform(post("/api/auth/connexion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requete)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statut").value(401))
                .andExpect(jsonPath("$.message").value("Email ou mot de passe incorrect."));
    }
}

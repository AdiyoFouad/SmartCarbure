package fr.miage.smartcarbure.service;

import fr.miage.smartcarbure.dto.ConnexionRequest;
import fr.miage.smartcarbure.dto.InscriptionRequest;
import fr.miage.smartcarbure.dto.UtilisateurDTO;
import fr.miage.smartcarbure.enums.Carburant;
import fr.miage.smartcarbure.exception.IdentifiantsInvalidesException;
import fr.miage.smartcarbure.exception.UtilisateurDejaExistantException;
import fr.miage.smartcarbure.model.Utilisateur;
import fr.miage.smartcarbure.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de UtilisateurService (L4-07) :
 * - Inscription avec mot de passe haché (BCrypt)
 * - Détection des doublons d'email (409)
 * - Authentification et vérification des identifiants (401)
 */
@ExtendWith(MockitoExtension.class)
class UtilisateurServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    private PasswordEncoder passwordEncoder;
    private UtilisateurService utilisateurService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        utilisateurService = new UtilisateurService(utilisateurRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Inscription : crée un compte et hache le mot de passe avec BCrypt")
    void inscrire_creeCompteAvecMotDePasseHache() {
        InscriptionRequest requete = new InscriptionRequest("Camille Martin", "camille@exemple.fr", "motdepasse123");

        when(utilisateurRepository.existsByEmail("camille@exemple.fr")).thenReturn(false);
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(invocation -> {
            Utilisateur u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        UtilisateurDTO resultat = utilisateurService.inscrire(requete);

        assertThat(resultat).isNotNull();
        assertThat(resultat.getId()).isEqualTo(1L);
        assertThat(resultat.getNom()).isEqualTo("Camille Martin");
        assertThat(resultat.getEmail()).isEqualTo("camille@exemple.fr");
        assertThat(resultat.getCarburantPrefere()).isNull();
        assertThat(resultat.getRayonPrefere()).isNull();

        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(captor.capture());
        Utilisateur utilisateurSauvegarde = captor.getValue();

        assertThat(utilisateurSauvegarde.getMotDePasse()).isNotEqualTo("motdepasse123");
        assertThat(passwordEncoder.matches("motdepasse123", utilisateurSauvegarde.getMotDePasse())).isTrue();
    }

    @Test
    @DisplayName("Inscription : normalise l'adresse email (trim et minuscules)")
    void inscrire_normaliseEmail() {
        InscriptionRequest requete = new InscriptionRequest("  Camille  ", "  CAMILLE@Exemple.FR  ", "motdepasse123");

        when(utilisateurRepository.existsByEmail("camille@exemple.fr")).thenReturn(false);
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> {
            Utilisateur u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UtilisateurDTO dto = utilisateurService.inscrire(requete);

        assertThat(dto.getNom()).isEqualTo("Camille");
        assertThat(dto.getEmail()).isEqualTo("camille@exemple.fr");
        verify(utilisateurRepository).existsByEmail("camille@exemple.fr");
    }

    @Test
    @DisplayName("Inscription : lève UtilisateurDejaExistantException en cas d'email déjà utilisé")
    void inscrire_emailExistant_leveException() {
        InscriptionRequest requete = new InscriptionRequest("Camille Martin", "existant@exemple.fr", "motdepasse123");
        when(utilisateurRepository.existsByEmail("existant@exemple.fr")).thenReturn(true);

        assertThatThrownBy(() -> utilisateurService.inscrire(requete))
                .isInstanceOf(UtilisateurDejaExistantException.class)
                .hasMessageContaining("existant@exemple.fr");

        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    @DisplayName("Authentification : renvoie l'utilisateur lorsque les identifiants sont corrects")
    void authentifier_succes() {
        String hash = passwordEncoder.encode("motdepasse123");
        Utilisateur utilisateurExistant = new Utilisateur("Camille", "camille@exemple.fr", hash);
        utilisateurExistant.setId(1L);
        utilisateurExistant.setCarburantPrefere(Carburant.GAZOLE);
        utilisateurExistant.setRayonPrefere(15);

        when(utilisateurRepository.findByEmail("camille@exemple.fr")).thenReturn(Optional.of(utilisateurExistant));

        ConnexionRequest requete = new ConnexionRequest("camille@exemple.fr", "motdepasse123");
        Utilisateur utilisateur = utilisateurService.authentifier(requete);

        assertThat(utilisateur).isNotNull();
        assertThat(utilisateur.getId()).isEqualTo(1L);
        assertThat(utilisateur.getEmail()).isEqualTo("camille@exemple.fr");
        assertThat(utilisateur.getCarburantPrefere()).isEqualTo(Carburant.GAZOLE);
    }

    @Test
    @DisplayName("Authentification : lève IdentifiantsInvalidesException si l'email est inconnu")
    void authentifier_emailInconnu_leveException() {
        when(utilisateurRepository.findByEmail("inconnu@exemple.fr")).thenReturn(Optional.empty());

        ConnexionRequest requete = new ConnexionRequest("inconnu@exemple.fr", "motdepasse123");

        assertThatThrownBy(() -> utilisateurService.authentifier(requete))
                .isInstanceOf(IdentifiantsInvalidesException.class);
    }

    @Test
    @DisplayName("Authentification : lève IdentifiantsInvalidesException si le mot de passe est faux")
    void authentifier_mauvaisMotDePasse_leveException() {
        String hash = passwordEncoder.encode("bonMotDePasse");
        Utilisateur utilisateurExistant = new Utilisateur("Camille", "camille@exemple.fr", hash);

        when(utilisateurRepository.findByEmail("camille@exemple.fr")).thenReturn(Optional.of(utilisateurExistant));

        ConnexionRequest requete = new ConnexionRequest("camille@exemple.fr", "mauvaisMotDePasse");

        assertThatThrownBy(() -> utilisateurService.authentifier(requete))
                .isInstanceOf(IdentifiantsInvalidesException.class);
    }
}

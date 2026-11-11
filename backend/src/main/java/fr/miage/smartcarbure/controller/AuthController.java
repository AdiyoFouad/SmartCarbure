package fr.miage.smartcarbure.controller;

import fr.miage.smartcarbure.dto.AuthResponse;
import fr.miage.smartcarbure.dto.ConnexionRequest;
import fr.miage.smartcarbure.dto.InscriptionRequest;
import fr.miage.smartcarbure.dto.UtilisateurDTO;
import fr.miage.smartcarbure.model.Utilisateur;
import fr.miage.smartcarbure.service.JwtService;
import fr.miage.smartcarbure.service.UtilisateurService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Expose les routes REST d'inscription et de connexion. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UtilisateurService utilisateurService;
    private final JwtService jwtService;

    public AuthController(UtilisateurService utilisateurService, JwtService jwtService) {
        this.utilisateurService = utilisateurService;
        this.jwtService = jwtService;
    }

    /** POST /api/auth/inscription : crée un compte et renvoie l'utilisateur (201). */
    @PostMapping("/inscription")
    @ResponseStatus(HttpStatus.CREATED)
    public UtilisateurDTO inscrire(@Valid @RequestBody InscriptionRequest requete) {
        return utilisateurService.inscrire(requete);
    }

    /** POST /api/auth/connexion : vérifie les identifiants et renvoie le jeton et l'utilisateur (200). */
    @PostMapping("/connexion")
    public AuthResponse connecter(@Valid @RequestBody ConnexionRequest requete) {
        Utilisateur utilisateur = utilisateurService.authentifier(requete);
        String jeton = jwtService.genererJeton(utilisateur);
        return new AuthResponse(jeton, UtilisateurDTO.depuis(utilisateur));
    }
}
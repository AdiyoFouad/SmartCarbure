package fr.miage.smartcarbure.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Réponse de connexion : jeton d'authentification et utilisateur connecté. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String jeton;
    private UtilisateurDTO utilisateur;
}
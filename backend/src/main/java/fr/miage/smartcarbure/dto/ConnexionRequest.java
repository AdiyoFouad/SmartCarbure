package fr.miage.smartcarbure.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Identifiants envoyés par le frontend pour se connecter. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConnexionRequest {

    @NotBlank(message = "L'email est obligatoire.")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire.")
    private String motDePasse;
}
package fr.miage.smartcarbure.dto;

import fr.miage.smartcarbure.enums.Carburant;
import fr.miage.smartcarbure.model.Utilisateur;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Utilisateur renvoyé au frontend : jamais de mot de passe. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UtilisateurDTO {

    private Long id;
    private String nom;
    private String email;
    private Carburant carburantPrefere;
    private Integer rayonPrefere;

    public static UtilisateurDTO toDTO(Utilisateur utilisateur) {
        return new UtilisateurDTO(
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getEmail(),
                utilisateur.getCarburantPrefere(),
                utilisateur.getRayonPrefere());
    }
}
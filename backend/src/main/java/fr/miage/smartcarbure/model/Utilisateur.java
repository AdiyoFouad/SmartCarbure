package fr.miage.smartcarbure.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Compte utilisateur : identité, email, mot de passe haché (BCrypt)
 * et préférences de recherche (carburant et rayon).
 */
@Entity
@Table(name = "utilisateur")
@Getter
@Setter
@NoArgsConstructor
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    /** Mot de passe haché avec BCrypt, jamais en clair. */
    @Column(name = "mot_de_passe", nullable = false, length = 100)
    private String motDePasse;

    /** Code carburant préféré (GAZOLE, SP95...), ou null. */
    @Column(name = "carburant_prefere", length = 10)
    private String carburantPrefere;

    /** Rayon de recherche préféré en km (1 à 50), ou null. */
    @Column(name = "rayon_prefere")
    private Integer rayonPrefere;

    public Utilisateur(String nom, String email, String motDePasse) {
        this.nom = nom;
        this.email = email;
        this.motDePasse = motDePasse;
    }
}
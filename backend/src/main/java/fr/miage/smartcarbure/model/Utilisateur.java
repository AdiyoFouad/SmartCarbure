package fr.miage.smartcarbure.model;

import fr.miage.smartcarbure.enums.Carburant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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

    /** Carburant préféré, ou null. Stocké en texte (GAZOLE, SP95...). */
    @Enumerated(EnumType.STRING)
    @Column(name = "carburant_prefere", length = 10)
    private Carburant carburantPrefere;

    /** Rayon de recherche préféré en km (1 à 50), ou null. */
    @Column(name = "rayon_prefere")
    private Integer rayonPrefere;

    /** Date de création du compte, remplie automatiquement. */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Date de dernière modification, mise à jour automatiquement. */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Utilisateur(String nom, String email, String motDePasse) {
        this.nom = nom;
        this.email = email;
        this.motDePasse = motDePasse;
    }
}
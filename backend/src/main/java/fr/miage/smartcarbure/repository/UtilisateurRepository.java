package fr.miage.smartcarbure.repository;

import fr.miage.smartcarbure.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Accès aux utilisateurs enregistrés dans PostgreSQL. */
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);
}
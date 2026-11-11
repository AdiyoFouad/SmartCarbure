package fr.miage.smartcarbure.service;

import fr.miage.smartcarbure.dto.ConnexionRequest;
import fr.miage.smartcarbure.dto.InscriptionRequest;
import fr.miage.smartcarbure.dto.UtilisateurDTO;
import fr.miage.smartcarbure.exception.IdentifiantsInvalidesException;
import fr.miage.smartcarbure.exception.UtilisateurDejaExistantException;
import fr.miage.smartcarbure.model.Utilisateur;
import fr.miage.smartcarbure.repository.UtilisateurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gère la création de compte et la connexion des utilisateurs. */
@Service
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    public UtilisateurService(UtilisateurRepository utilisateurRepository,
                              PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Crée un compte. Lève UtilisateurDejaExistantException si l'email est déjà pris. */
    @Transactional
    public UtilisateurDTO inscrire(InscriptionRequest requete) {
        String email = normaliser(requete.getEmail());

        if (utilisateurRepository.existsByEmail(email)) {
            throw new UtilisateurDejaExistantException(email);
        }

        Utilisateur utilisateur = new Utilisateur(
                requete.getNom().trim(),
                email,
                passwordEncoder.encode(requete.getMotDePasse()));

        return UtilisateurDTO.depuis(utilisateurRepository.save(utilisateur));
    }

    /**
     * Vérifie les identifiants et renvoie l'utilisateur.
     * Lève IdentifiantsInvalidesException si l'email est inconnu ou le mot de passe faux.
     */
    @Transactional(readOnly = true)
    public Utilisateur authentifier(ConnexionRequest requete) {
        Utilisateur utilisateur = utilisateurRepository
                .findByEmail(normaliser(requete.getEmail()))
                .orElseThrow(IdentifiantsInvalidesException::new);

        if (!passwordEncoder.matches(requete.getMotDePasse(), utilisateur.getMotDePasse())) {
            throw new IdentifiantsInvalidesException();
        }
        return utilisateur;
    }

    /** Email comparé sans espaces et en minuscules. */
    private String normaliser(String email) {
        return email.trim().toLowerCase();
    }
}
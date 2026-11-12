package fr.miage.smartcarbure.exception;

/** Levée quand l'email d'inscription est déjà utilisé (renvoyée en 409). */
public class UtilisateurDejaExistantException extends RuntimeException {

    public UtilisateurDejaExistantException(String email) {
        super("Un compte existe déjà avec l'email " + email + ".");
    }
}

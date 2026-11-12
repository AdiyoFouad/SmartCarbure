package fr.miage.smartcarbure.exception;

/** Levée quand l'email ou le mot de passe est incorrect (renvoyée en 401). */
public class IdentifiantsInvalidesException extends RuntimeException {

    public IdentifiantsInvalidesException() {
        super("Email ou mot de passe incorrect.");
    }
}

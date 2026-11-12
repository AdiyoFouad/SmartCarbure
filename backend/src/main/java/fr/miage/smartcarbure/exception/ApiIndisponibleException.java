package fr.miage.smartcarbure.exception;

/**
 * Levée quand l'API Prix des carburants ne répond pas correctement : délai dépassé, réseau coupé,
 * erreur 4xx ou 5xx, réponse vide ou illisible. Elle est renvoyée au frontend en 503.
 * La cause détaillée est journalisée par CarburantApiClient.
 */
public class ApiIndisponibleException extends RuntimeException {

    public ApiIndisponibleException(String message) {
        super(message);
    }

    public ApiIndisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}

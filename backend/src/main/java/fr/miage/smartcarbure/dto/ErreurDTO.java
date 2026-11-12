package fr.miage.smartcarbure.dto;

/**
 * Corps de toutes les réponses d'erreur (voir docs/contrat-api.md, « Codes d'erreur »).
 *
 * @param statut  code HTTP de la réponse (même valeur que le statut HTTP)
 * @param message explication lisible en français, utile pour le débogage
 */
public record ErreurDTO(int statut, String message) {
}

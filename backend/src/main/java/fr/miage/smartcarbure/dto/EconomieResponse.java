package fr.miage.smartcarbure.dto;

import java.math.BigDecimal;

/**
 * Résultat d'un calcul d'économie : réponse de {@code GET /api/economie} (F15).
 *
 * <p>Exemple JSON : {@code {"prixStation":1.749,"prixMoyenLocal":1.771,"litres":50.0,"montant":1.10}}</p>
 *
 * <p>Les prix et le montant sont des {@link BigDecimal} : ce sont des sommes d'argent, qu'un double
 * ne sait pas toujours représenter exactement (le double 1.749 vaut en réalité 1.74900000000000011…).</p>
 *
 * @param prixStation    prix du litre dans la station choisie, en €/L (3 décimales)
 * @param prixMoyenLocal prix moyen du litre dans les stations de la même ville, en €/L (arrondi à 3 décimales)
 * @param litres         volume du plein saisi par l'utilisateur
 * @param montant        (prixMoyenLocal − prixStation) × litres, arrondi au centime :
 *                       positif pour une économie, négatif pour un surcoût
 */
public record EconomieResponse(BigDecimal prixStation, BigDecimal prixMoyenLocal, double litres, BigDecimal montant) {
}

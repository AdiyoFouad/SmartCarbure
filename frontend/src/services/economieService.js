/**
 * Calcule l'économie (ou le surcoût) d'un plein dans une station par rapport au prix moyen
 * local (F15) : GET /api/economie?idStation=&carburant=&litres=
 * TODO : à implémenter.
 * @param {{idStation: string, carburant: string, litres: number}} criteres
 * @returns {Promise<{prixStation: number, prixMoyenLocal: number, litres: number, montant: number}>}
 *   `montant` positif pour une économie, négatif pour un surcoût
 * @throws {ApiError} 400 si la saisie est invalide, 404 si la station n'existe pas,
 *   503 si le service est indisponible
 */
export async function calculerEconomie(criteres) {
  // TODO
}

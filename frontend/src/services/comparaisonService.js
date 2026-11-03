/**
 * Compare le prix moyen d'un carburant entre deux villes ou deux départements (F16) :
 * GET /api/comparer?type=VILLE|DEPARTEMENT&zone1=&zone2=&carburant=
 * TODO : à implémenter.
 * @param {object} criteres { type, zone1, zone2, carburant }
 * @returns {Promise<object>} prix moyen de chaque zone et écart
 * @throws {ApiError} 400 si la saisie est invalide, 503 si le service est indisponible
 */
export async function comparerZones(criteres) {
  // TODO
}

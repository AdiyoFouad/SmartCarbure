/**
 * Recherche des stations par ville (ou code postal), ou autour d'une position.
 * - par ville : GET /api/stations?ville=&carburant=&tri=
 * - par position : GET /api/stations?lat=&lon=&rayon=&carburant=&tri=
 * Une ville inconnue renvoie une liste vide (200), jamais un 404.
 * En mode démonstration (MODE_DEMO), renvoyer les stations fictives de data/stations.js.
 * TODO : à implémenter.
 * @param {object} criteres { ville } ou { latitude, longitude, rayon }, avec carburant et tri
 * @returns {Promise<Array<object>>} stations trouvées
 * @throws {ApiError} 400 si la saisie est invalide, 503 si le service est indisponible
 */
export async function rechercherStations(criteres) {
  // TODO
}

/**
 * Récupère la fiche complète d'une station : GET /api/stations/{id}.
 * TODO : à implémenter.
 * @param {string} id identifiant de la station
 * @returns {Promise<object>} station
 * @throws {ApiError} 404 si la station n'existe pas, 503 si le service est indisponible
 */
export async function obtenirStation(id) {
  // TODO
}

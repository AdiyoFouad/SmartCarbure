/**
 * Recherche les stations le long d'un trajet, triées par prix :
 * GET /api/trajet?depart=lat,lon&arrivee=lat,lon&carburant=
 * TODO : à implémenter.
 * @param {object} criteres { depart: {latitude, longitude}, arrivee: {latitude, longitude}, carburant }
 * @returns {Promise<{trajet: object, stations: Array<object>}>} itinéraire et stations
 * @throws {ApiError} 400 si les positions sont invalides, 503 si un service est indisponible
 */
export async function rechercherStationsTrajet(criteres) {
  // TODO
}

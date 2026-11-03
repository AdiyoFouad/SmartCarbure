/**
 * Liste les stations favorites de l'utilisateur connecté : GET /api/favoris.
 * TODO : à implémenter.
 * @returns {Promise<Array<object>>} stations favorites
 * @throws {ApiError} 401 si l'utilisateur n'est pas connecté
 */
export async function listerFavoris() {
  // TODO
}

/**
 * Ajoute une station aux favoris : POST /api/favoris { idStation }.
 * TODO : à implémenter.
 * @param {string} idStation identifiant de la station
 * @returns {Promise<void>}
 * @throws {ApiError} 401 si l'utilisateur n'est pas connecté, 404 si la station n'existe pas
 */
export async function ajouterFavori(idStation) {
  // TODO
}

/**
 * Retire une station des favoris : DELETE /api/favoris/{idStation}.
 * TODO : à implémenter.
 * @param {string} idStation identifiant de la station
 * @returns {Promise<void>}
 * @throws {ApiError} 401 si l'utilisateur n'est pas connecté
 */
export async function retirerFavori(idStation) {
  // TODO
}

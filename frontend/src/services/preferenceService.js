/**
 * Récupère les préférences de l'utilisateur connecté : GET /api/preferences.
 * TODO : à implémenter.
 * @returns {Promise<{carburantPrefere: string|null, rayonPrefere: number|null}>} préférences
 * @throws {ApiError} 401 si l'utilisateur n'est pas connecté
 */
export async function obtenirPreferences() {
  // TODO
}

/**
 * Enregistre les préférences : PUT /api/preferences { carburantPrefere, rayonPrefere }.
 * TODO : à implémenter.
 * @param {{carburantPrefere: string|null, rayonPrefere: number|null}} preferences
 * @returns {Promise<object>} préférences enregistrées
 * @throws {ApiError} 400 si les valeurs sont invalides, 401 si l'utilisateur n'est pas connecté
 */
export async function enregistrerPreferences(preferences) {
  // TODO
}

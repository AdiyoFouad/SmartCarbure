// Outils du mode démonstration. Seuls les services utilisent ce fichier :
// les pages ne savent jamais si elles reçoivent des données fictives ou réelles.

/** true si les services doivent renvoyer les données fictives de src/data (VITE_USE_MOCK=true). */
export const MODE_DEMO = import.meta.env.VITE_USE_MOCK === 'true';

/**
 * Simule un appel au backend : attend un court délai, puis renvoie une copie du résultat
 * de `produire`, ou propage l'ApiError qu'elle lève (pour simuler les cas d'erreur
 * du cahier de recette : ville « panne » ou paramètre ?simuler=503 dans l'URL de la page).
 * TODO : à implémenter.
 * @param {() => any} produire fonction qui calcule la réponse fictive
 * @returns {Promise<any>} réponse simulée
 */
export async function reponseSimulee(produire) {
  // TODO
}

// Outils du mode démonstration. Seuls les services utilisent ce fichier :
// les pages ne savent jamais si elles reçoivent des données fictives ou réelles.

import { ApiError } from './api';

/** true si les services doivent renvoyer les données fictives de src/data (VITE_USE_MOCK=true). */
export const MODE_DEMO = import.meta.env.VITE_USE_MOCK === 'true';

/** Délai simulé d'une réponse du backend, en millisecondes. */
export const DELAI_SIMULE_MS = 300;

/** Durée pendant laquelle ?simuler=503 fait échouer les appels, à partir du premier échec. */
export const DUREE_PANNE_SIMULEE_MS = 3000;

let debutPanneSimulee = null;

/**
 * Indique si une panne doit être simulée : l'URL de la page contient ?simuler=503.
 * La panne dure DUREE_PANNE_SIMULEE_MS à partir du premier appel en échec, puis les appels
 * réussissent : on peut ainsi tester le bouton « Réessayer ». Une fenêtre de temps (et non un seul
 * échec) est nécessaire car, en StrictMode, React lance deux fois chaque effet.
 * @param {number} [maintenant=Date.now()]
 * @returns {boolean}
 */
export function panneSimulee(maintenant = Date.now()) {
  const parametres = new URLSearchParams(globalThis.location?.search ?? '');
  if (parametres.get('simuler') !== '503') {
    return false;
  }
  if (debutPanneSimulee === null) {
    debutPanneSimulee = maintenant;
  }
  return maintenant - debutPanneSimulee < DUREE_PANNE_SIMULEE_MS;
}

/** Remet à zéro la panne simulée (utile pour les tests). */
export function reinitialiserPanneSimulee() {
  debutPanneSimulee = null;
}

/**
 * Simule un appel au backend : attend un court délai, puis renvoie une copie du résultat
 * de `produire`, ou propage l'ApiError qu'elle lève (par exemple un 404 pour un id inconnu).
 * Avec ?simuler=503 dans l'URL de la page, renvoie une ApiError 503 pendant 3 secondes.
 * @param {() => any} produire fonction qui calcule la réponse fictive
 * @param {number} [delai=DELAI_SIMULE_MS] délai simulé en millisecondes
 * @returns {Promise<any>} réponse simulée
 * @throws {ApiError} erreur levée par `produire`, ou 503 simulé
 */
export async function reponseSimulee(produire, delai = DELAI_SIMULE_MS) {
  await new Promise((resoudre) => setTimeout(resoudre, delai));
  if (panneSimulee()) {
    throw new ApiError(503);
  }
  const resultat = await produire();
  // Copie profonde : une page qui modifie les données reçues ne modifie pas les données fictives.
  return resultat === undefined ? undefined : structuredClone(resultat);
}

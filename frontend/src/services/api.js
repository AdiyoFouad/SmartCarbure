// Client HTTP unique de l'application. Toutes les URL sont relatives et commencent par /api :
// en développement, Vite les redirige vers le backend (vite.config.js) ; dans Docker, c'est Nginx.

const PREFIXE = '/api';
const DELAI_PAR_DEFAUT_MS = 10000;

/** Messages affichés par défaut pour chaque code d'erreur HTTP. */
export const MESSAGES_ERREUR = {
  400: 'Saisie invalide, vérifiez les champs du formulaire.',
  401: 'Vous devez être connecté pour accéder à ce contenu.',
  404: "L'élément demandé est introuvable.",
  409: 'Cette ressource existe déjà.',
  503: 'Service temporairement indisponible. Réessayez dans quelques instants.',
  inconnu: 'Une erreur inattendue est survenue. Réessayez plus tard.',
};

/** Erreur renvoyée par tous les services, quelle que soit son origine (HTTP, réseau, délai). */
export class ApiError extends Error {
  /**
   * @param {number} statut code HTTP (503 pour une panne réseau ou un délai dépassé)
   * @param {string} [message] message lisible par l'utilisateur
   * @param {object} [details] corps de la réponse d'erreur du backend, s'il existe
   */
  constructor(statut, message, details = null) {
    super(message || MESSAGES_ERREUR[statut] || MESSAGES_ERREUR.inconnu);
    this.name = 'ApiError';
    this.statut = statut;
    this.details = details;
  }
}

let jetonCourant = null;

/**
 * Enregistre le jeton envoyé dans l'en-tête Authorization des requêtes suivantes.
 * Appelée par AuthContext à la connexion et à la déconnexion.
 * @param {string|null} jeton jeton d'authentification, ou null pour l'effacer
 */
export function definirJeton(jeton) {
  jetonCourant = jeton;
}

/**
 * Renvoie le jeton d'authentification courant.
 * @returns {string|null} jeton, ou null si l'utilisateur n'est pas connecté
 */
export function obtenirJeton() {
  return jetonCourant;
}

function construireUrl(chemin, params) {
  const recherche = new URLSearchParams();
  Object.entries(params || {}).forEach(([cle, valeur]) => {
    if (valeur !== undefined && valeur !== null && valeur !== '') {
      recherche.append(cle, valeur);
    }
  });
  const chaine = recherche.toString();
  return `${PREFIXE}${chemin}${chaine ? `?${chaine}` : ''}`;
}

async function lireCorps(reponse) {
  if (reponse.status === 204) {
    return null;
  }
  const texte = await reponse.text();
  if (!texte) {
    return null;
  }
  try {
    return JSON.parse(texte);
  } catch {
    return texte;
  }
}

/**
 * Envoie une requête au backend et renvoie le corps JSON de la réponse.
 * @param {string} chemin chemin après /api (ex. « /stations »)
 * @param {object} [options]
 * @param {string} [options.methode='GET'] méthode HTTP
 * @param {object} [options.params] paramètres de la chaîne de requête (valeurs vides ignorées)
 * @param {object} [options.corps] corps envoyé en JSON
 * @param {number} [options.delai] délai d'attente maximal en millisecondes
 * @returns {Promise<any>} corps de la réponse
 * @throws {ApiError} si le backend répond par une erreur, est injoignable ou trop lent
 */
export async function requete(chemin, { methode = 'GET', params, corps, delai = DELAI_PAR_DEFAUT_MS } = {}) {
  const controleur = new AbortController();
  const minuteur = setTimeout(() => controleur.abort(), delai);
  const entetes = { Accept: 'application/json' };
  if (corps !== undefined) {
    entetes['Content-Type'] = 'application/json';
  }
  if (jetonCourant) {
    entetes.Authorization = `Bearer ${jetonCourant}`;
  }

  let reponse;
  try {
    reponse = await fetch(construireUrl(chemin, params), {
      method: methode,
      headers: entetes,
      body: corps !== undefined ? JSON.stringify(corps) : undefined,
      signal: controleur.signal,
    });
  } catch {
    // Panne réseau, backend arrêté ou délai dépassé : même message pour l'utilisateur.
    throw new ApiError(503);
  } finally {
    clearTimeout(minuteur);
  }

  const donnees = await lireCorps(reponse);
  if (!reponse.ok) {
    // 502 et 504 (proxy Nginx sans backend) sont traités comme une indisponibilité.
    const statut = reponse.status === 502 || reponse.status === 504 ? 503 : reponse.status;
    throw new ApiError(statut, undefined, donnees);
  }
  return donnees;
}

export const api = {
  get: (chemin, params) => requete(chemin, { params }),
  post: (chemin, corps) => requete(chemin, { methode: 'POST', corps }),
  put: (chemin, corps) => requete(chemin, { methode: 'PUT', corps }),
  supprimer: (chemin) => requete(chemin, { methode: 'DELETE' }),
};

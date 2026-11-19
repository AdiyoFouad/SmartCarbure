import { api, ApiError } from './api';
import { MODE_DEMO, reponseSimulee } from './mock';
import { STATIONS } from '../data/stations';

/** Nom de ville comparé sans accents, sans espaces autour et sans tenir compte de la casse. */
function normaliserVille(ville) {
  return String(ville ?? '')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .trim()
    .toLocaleLowerCase('fr-FR');
}

/**
 * Prix du carburant en millièmes d'euro (1.749 → 1749), s'il est vendu, disponible et positif ;
 * null sinon. Les calculs en entiers évitent les erreurs d'arrondi des nombres décimaux.
 */
function prixDisponibleEnMilliemes(station, carburant) {
  const c = station.carburants?.find((item) => item.type === carburant);
  if (!c || !c.disponible || typeof c.prix !== 'number' || c.prix <= 0) {
    return null;
  }
  return Math.round(c.prix * 1000);
}

/** Arrondi à l'entier le plus proche, la moitié s'éloignant de zéro (RoundingMode.HALF_UP de Java). */
function arrondiHalfUp(valeur) {
  return Math.sign(valeur) * Math.round(Math.abs(valeur) + 1e-9);
}

/**
 * Calcul du mode démonstration, avec les mêmes règles que EconomieService du backend :
 * moyenne des stations de la même ville qui ont un prix disponible (station choisie toujours comptée),
 * arrondie à 3 décimales ; montant = (moyenne − prix de la station) × litres, arrondi au centime.
 */
function calculerDansDemo({ idStation, carburant, litres }) {
  const volume = Number(litres);
  if (!carburant || !Number.isFinite(volume) || volume <= 0) {
    throw new ApiError(400, 'Saisie invalide.');
  }
  const station = STATIONS.find((s) => s.id === String(idStation));
  if (!station) {
    throw new ApiError(404, 'Station introuvable.');
  }
  const prixStation = prixDisponibleEnMilliemes(station, carburant);
  if (prixStation === null) {
    throw new ApiError(400, `La station ${station.id} ne vend pas le carburant ${carburant} ou l'a en rupture.`);
  }

  const ville = normaliserVille(station.ville);
  const prixVille = STATIONS
    .filter((s) => s.id !== station.id && normaliserVille(s.ville) === ville)
    .map((s) => prixDisponibleEnMilliemes(s, carburant))
    .filter((p) => p !== null);
  const tousLesPrix = [prixStation, ...prixVille];
  const moyenne = arrondiHalfUp(tousLesPrix.reduce((somme, p) => somme + p, 0) / tousLesPrix.length);

  // (moyenne − prix) en millièmes × litres / 10 = montant en centimes
  const montantCentimes = arrondiHalfUp(((moyenne - prixStation) * volume) / 10);
  return {
    prixStation: prixStation / 1000,
    prixMoyenLocal: moyenne / 1000,
    litres: volume,
    montant: montantCentimes / 100,
  };
}

/**
 * Calcule l'économie (ou le surcoût) d'un plein dans une station par rapport au prix moyen
 * local (F15) : GET /api/economie?idStation=&carburant=&litres=
 * En mode démonstration (MODE_DEMO), le calcul est fait sur les stations de data/stations.js,
 * avec les règles du backend (moyenne des stations de la même ville, ruptures exclues).
 * @param {{idStation: string, carburant: string, litres: number}} criteres
 * @returns {Promise<{prixStation: number, prixMoyenLocal: number, litres: number, montant: number}>}
 *   `montant` positif pour une économie, négatif pour un surcoût
 * @throws {ApiError} 400 si la saisie est invalide, 404 si la station n'existe pas,
 *   503 si le service est indisponible
 */
export async function calculerEconomie(criteres) {
  if (MODE_DEMO) {
    return reponseSimulee(() => calculerDansDemo(criteres));
  }
  return api.get('/economie', {
    idStation: criteres.idStation,
    carburant: criteres.carburant,
    litres: criteres.litres,
  });
}

import { requete } from './api';
import { MODE_DEMO, reponseSimulee } from './mock';
import { STATIONS } from '../data/stations';

/**
 * Délai d'attente de GET /api/trajet : 30 s au lieu des 10 s habituelles.
 * Un long trajet interroge le service d'itinéraire puis l'API des carburants tronçon par tronçon :
 * un Rennes → Paris réel prend de 6 à 12 s.
 */
export const DELAI_TRAJET_MS = 30000;

/** Distance maximale à la route utilisée par le backend quand rayon n'est pas fourni. */
const RAYON_PAR_DEFAUT_KM = 5;

/** « latitude,longitude » attendu par le backend. */
export function formaterPosition(position) {
  return `${position.latitude},${position.longitude}`;
}

function distanceEntre(a, b) {
  const radians = (degres) => (degres * Math.PI) / 180;
  const latitude = radians(b.latitude - a.latitude);
  const longitude = radians(b.longitude - a.longitude);
  const valeur = Math.sin(latitude / 2) ** 2 +
    Math.cos(radians(a.latitude)) * Math.cos(radians(b.latitude)) * Math.sin(longitude / 2) ** 2;
  return 6371 * 2 * Math.atan2(Math.sqrt(valeur), Math.sqrt(1 - valeur));
}

/** Distance approximative (km) entre un point et le segment [a, b], pour le mode démonstration. */
function distanceAuSegment(point, a, b) {
  const kmParDegre = 111.195;
  const cos = Math.cos((point.latitude * Math.PI) / 180);
  const ax = (a.longitude - point.longitude) * cos * kmParDegre;
  const ay = (a.latitude - point.latitude) * kmParDegre;
  const bx = (b.longitude - point.longitude) * cos * kmParDegre;
  const by = (b.latitude - point.latitude) * kmParDegre;
  const dx = bx - ax;
  const dy = by - ay;
  const longueur = dx * dx + dy * dy;
  const t = longueur === 0 ? 0 : Math.max(0, Math.min(1, -(ax * dx + ay * dy) / longueur));
  return Math.hypot(ax + t * dx, ay + t * dy);
}

/** Mode démonstration : trajet en ligne droite et stations fictives proches de cette ligne. */
function rechercherDansDemo({ depart, arrivee, carburant, rayon }) {
  const rayonKm = rayon ?? RAYON_PAR_DEFAUT_KM;
  const arrondir = (valeur) => Math.round(valeur * 10) / 10;
  const stations = STATIONS
    .filter((station) => station.position && station.carburants?.some((item) => item.type === carburant))
    .map((station) => ({ ...station, distanceKm: distanceAuSegment(station.position, depart, arrivee) }))
    .filter((station) => station.distanceKm <= rayonKm)
    .map((station) => ({ ...station, distanceKm: arrondir(station.distanceKm) }))
    .sort((a, b) => {
      const prixA = a.carburants.find((item) => item.type === carburant);
      const prixB = b.carburants.find((item) => item.type === carburant);
      if (!prixA.disponible) return prixB.disponible ? 1 : 0;
      if (!prixB.disponible) return -1;
      return prixA.prix - prixB.prix;
    });
  return {
    trajet: { depart, arrivee, points: [depart, arrivee], distanceKm: arrondir(distanceEntre(depart, arrivee)) },
    stations,
  };
}

/**
 * Recherche les stations le long d'un trajet (F6) :
 * GET /api/trajet?depart=lat,lon&arrivee=lat,lon&carburant=&rayon=
 * Le délai d'attente est de 30 s (DELAI_TRAJET_MS) au lieu de 10 s.
 * En mode démonstration (MODE_DEMO), le trajet est une ligne droite et les stations viennent de data/stations.js.
 * @param {object} criteres
 * @param {{latitude: number, longitude: number}} criteres.depart position de départ
 * @param {{latitude: number, longitude: number}} criteres.arrivee position d'arrivée
 * @param {string} criteres.carburant code du carburant
 * @param {number} [criteres.rayon] distance maximale à la route en km (1 à 10, 5 par défaut côté backend)
 * @returns {Promise<{trajet: object, stations: Array<object>}>} réponse au format du contrat d'API
 * @throws {ApiError} 400 (saisie invalide, trajet trop long, aucune route), 503 (service indisponible)
 */
export async function rechercherTrajet({ depart, arrivee, carburant, rayon }) {
  if (MODE_DEMO) {
    return reponseSimulee(() => rechercherDansDemo({ depart, arrivee, carburant, rayon }));
  }
  return requete('/trajet', {
    params: {
      depart: formaterPosition(depart),
      arrivee: formaterPosition(arrivee),
      carburant,
      rayon,
    },
    delai: DELAI_TRAJET_MS,
  });
}

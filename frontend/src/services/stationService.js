import { api, ApiError } from './api';
import { MODE_DEMO, reponseSimulee } from './mock';
import { STATIONS } from '../data/stations';

function normaliser(texte) {
  return texte.normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toLocaleLowerCase('fr-FR');
}

function distanceEntre(a, b) {
  const radians = (degres) => (degres * Math.PI) / 180;
  const latitude = radians(b.latitude - a.latitude);
  const longitude = radians(b.longitude - a.longitude);
  const valeur = Math.sin(latitude / 2) ** 2 +
    Math.cos(radians(a.latitude)) * Math.cos(radians(b.latitude)) * Math.sin(longitude / 2) ** 2;
  return 6371 * 2 * Math.atan2(Math.sqrt(valeur), Math.sqrt(1 - valeur));
}

function prixDe(station, carburant) {
  return station.carburants?.find((item) => item.type === carburant);
}

function rechercherDansDemo(criteres) {
  let stations = STATIONS;
  const carburant = criteres.carburant;
  const position = criteres.latitude != null && criteres.longitude != null
    ? { latitude: Number(criteres.latitude), longitude: Number(criteres.longitude) }
    : null;

  if (criteres.ville) {
    const requete = normaliser(criteres.ville);
    stations = stations.filter((station) =>
      normaliser(station.ville) === requete || station.codePostal.startsWith(requete),
    );
  }

  stations = stations
    .filter((station) => !carburant || prixDe(station, carburant))
    .map((station) => ({
      ...station,
      distanceKm: position && station.position
        ? distanceEntre(position, station.position)
        : null,
    }));

  if (position) {
    stations = stations.filter((station) => station.distanceKm <= (Number(criteres.rayon) || 10));
  }

  const tri = String(criteres.tri || 'PRIX').toUpperCase();
  return stations.sort((a, b) => {
    if (tri === 'DISTANCE' && position) return a.distanceKm - b.distanceKm;
    const prixA = prixDe(a, carburant);
    const prixB = prixDe(b, carburant);
    if (!prixA?.disponible) return prixB?.disponible ? 1 : 0;
    if (!prixB?.disponible) return -1;
    return prixA.prix - prixB.prix;
  });
}

/**
 * Recherche des stations par ville (ou code postal), ou autour d'une position.
 * - par ville : GET /api/stations?ville=&carburant=&tri=
 * - par position : GET /api/stations?lat=&lon=&rayon=&carburant=&tri=
 * Une ville inconnue renvoie une liste vide (200), jamais un 404.
 * En mode démonstration (MODE_DEMO), renvoyer les stations fictives de data/stations.js.
 * @param {object} criteres { ville } ou { latitude, longitude, rayon }, avec carburant et tri
 * @returns {Promise<Array<object>>} stations trouvées
 * @throws {ApiError} 400 si la saisie est invalide, 503 si le service est indisponible
 */
export async function rechercherStations(criteres) {
  if (MODE_DEMO) {
    return reponseSimulee(() => rechercherDansDemo(criteres));
  }

  const params = {
    ville: criteres.ville,
    lat: criteres.latitude,
    lon: criteres.longitude,
    rayon: criteres.rayon,
    carburant: criteres.carburant,
    tri: criteres.tri,
  };
  return api.get('/stations', params);
}

/**
 * Récupère la fiche complète d'une station : GET /api/stations/{id}.
 * Tous les carburants sont renvoyés, y compris ceux en rupture (F8) ; distanceKm vaut null.
 * En mode démonstration (MODE_DEMO), cherche la station dans data/stations.js.
 * @param {string} id identifiant de la station
 * @returns {Promise<object>} station au format du contrat d'API
 * @throws {ApiError} 404 si la station n'existe pas, 503 si le service est indisponible
 */
export async function obtenirStation(id) {
  if (MODE_DEMO) {
    return reponseSimulee(() => {
      const station = STATIONS.find((s) => s.id === String(id));
      if (!station) {
        throw new ApiError(404, 'Station introuvable.');
      }
      return station;
    });
  }
  return api.get(`/stations/${encodeURIComponent(id)}`);
}

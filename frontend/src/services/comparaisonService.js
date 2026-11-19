import { api } from './api';
import { MODE_DEMO, reponseSimulee } from './mock';
import { STATIONS } from '../data/stations';
import { CARBURANTS } from '../constants/carburants';

function comparerDansDemo({ type, zone1, zone2, carburant }) {
  const typeNormalise = String(type).toUpperCase();
  const zones = [zone1, zone2].map((zone) => {
    const stations = STATIONS.filter((station) => {
      if (typeNormalise === 'VILLE') {
        return station.ville.toLocaleLowerCase('fr-FR') === String(zone).trim().toLocaleLowerCase('fr-FR');
      }
      return station.departement?.code === String(zone).trim();
    });
    const moyennes = {};
    const nombreStationsParCarburant = {};
    CARBURANTS.forEach(({ code }) => {
      const prix = stations
        .flatMap((station) => station.carburants || [])
        .filter((item) => item.type === code && item.disponible && Number.isFinite(item.prix))
        .map((item) => item.prix);
      moyennes[code] = prix.length
        ? Number((prix.reduce((somme, valeur) => somme + valeur, 0) / prix.length).toFixed(3))
        : null;
      nombreStationsParCarburant[code] = prix.length;
    });
    const departement = stations[0]?.departement;
    return {
      nom: typeNormalise === 'DEPARTEMENT' && departement
        ? `${departement.nom} (${departement.code})`
        : stations[0]?.ville || String(zone).trim(),
      prixMoyen: moyennes[carburant],
      nombreStations: nombreStationsParCarburant[carburant],
      moyennes,
      nombreStationsParCarburant,
      nombreStationsTotal: stations.length,
    };
  });

  return {
    type: typeNormalise,
    carburant,
    zone1: zones[0],
    zone2: zones[1],
    ecart: zones[0].moyennes[carburant] == null || zones[1].moyennes[carburant] == null
      ? null
      : Number((zones[0].moyennes[carburant] - zones[1].moyennes[carburant]).toFixed(3)),
  };
}

/**
 * Compare le prix moyen d'un carburant entre deux villes ou deux départements (F16) :
 * GET /api/comparer?type=VILLE|DEPARTEMENT&zone1=&zone2=&carburant=
 * @param {object} criteres { type, zone1, zone2, carburant }
 * @returns {Promise<object>} prix moyen de chaque zone et écart
 * @throws {ApiError} 400 si la saisie est invalide, 503 si le service est indisponible
 */
export async function comparerZones(criteres) {
  if (MODE_DEMO) {
    return reponseSimulee(() => comparerDansDemo(criteres));
  }

  return api.get('/comparer', {
    type: String(criteres.type).toUpperCase(),
    zone1: criteres.zone1,
    zone2: criteres.zone2,
    carburant: criteres.carburant,
  });
}

/** Types de carburant vendus en France (codes de l'API Prix des carburants). */
export const CARBURANTS = [
  { code: 'GAZOLE', libelle: 'Gazole' },
  { code: 'SP95', libelle: 'SP95' },
  { code: 'SP98', libelle: 'SP98' },
  { code: 'E10', libelle: 'SP95-E10' },
  { code: 'E85', libelle: 'E85' },
  { code: 'GPLC', libelle: 'GPLc' },
];

export const CODES_CARBURANTS = CARBURANTS.map((carburant) => carburant.code);

export const CARBURANT_PAR_DEFAUT = 'GAZOLE';

/**
 * Renvoie le libellé affiché d'un carburant.
 * @param {string} code code du carburant (ex. « E10 »)
 * @returns {string} libellé (ex. « SP95-E10 »), ou le code lui-même s'il est inconnu
 */
export function libelleCarburant(code) {
  const carburant = CARBURANTS.find((c) => c.code === code);
  return carburant ? carburant.libelle : code;
}

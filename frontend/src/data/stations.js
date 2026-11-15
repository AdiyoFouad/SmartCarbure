/**
 * Stations fictives du mode démonstration, autour de Rennes (35) et de Nantes (44),
 * au format du contrat d'API (docs/contrat-api.md, à la racine du dépôt). Chaque station a son
 * département et sa région pour que la comparaison par département fonctionne.
 *
 * Comme pour l'API réelle : nom vaut toujours null, distanceKm vaut null (c'est la recherche qui le calcule),
 * prix et dateMaj valent null pour un carburant en rupture sans prix.
 *
 * Cas couverts :
 * - 35000101 : station de route complète, ouverte tous les jours ;
 * - 35200102 : ruptures (E85 temporaire avec prix, SP98 définitive sans prix) ;
 * - 35510103 : station d'autoroute, automate 24h/24, horaires non communiqués ([]) ;
 * - 35700104 : fermée le dimanche, horaires du samedi non communiqués ;
 * - 44000105 et 44300106 : stations de Nantes.
 */

const ILLE_ET_VILAINE = { code: '35', nom: 'Ille-et-Vilaine' };
const BRETAGNE = { code: '53', nom: 'Bretagne' };
const LOIRE_ATLANTIQUE = { code: '44', nom: 'Loire-Atlantique' };
const PAYS_DE_LA_LOIRE = { code: '52', nom: 'Pays de la Loire' };

const JOURS = ['lundi', 'mardi', 'mercredi', 'jeudi', 'vendredi', 'samedi', 'dimanche'];

/**
 * Horaires des sept jours.
 * @param {string} ouverture heure d'ouverture (« 07:00 »)
 * @param {string} fermeture heure de fermeture (« 21:00 »)
 * @param {object} [exceptions] jours fermés et jours aux horaires non communiqués
 */
function semaine(ouverture, fermeture, { fermes = [], inconnus = [] } = {}) {
  return JOURS.map((jour) => {
    if (fermes.includes(jour)) {
      return { jour, ferme: true, ouverture: null, fermeture: null };
    }
    if (inconnus.includes(jour)) {
      return { jour, ferme: false, ouverture: null, fermeture: null };
    }
    return { jour, ferme: false, ouverture, fermeture };
  });
}

/** Carburant disponible avec son prix. */
function prix(type, valeur, dateMaj) {
  return { type, prix: valeur, dateMaj, disponible: true, typeRupture: null, debutRupture: null };
}

/** Carburant en rupture ; prix et dateMaj peuvent être null. */
function rupture(type, typeRupture, debutRupture, valeur = null, dateMaj = null) {
  return { type, prix: valeur, dateMaj, disponible: false, typeRupture, debutRupture };
}

export const STATIONS = [
  {
    id: '35000101',
    nom: null,
    adresse: '12 boulevard de la Liberté',
    ville: 'Rennes',
    codePostal: '35000',
    departement: ILLE_ET_VILAINE,
    region: BRETAGNE,
    position: { latitude: 48.1066, longitude: -1.6793 },
    emplacement: 'ROUTE',
    automate24h: true,
    horaires: semaine('07:00', '21:00'),
    services: ['Boutique alimentaire', 'Station de gonflage', 'Lavage automatique'],
    carburants: [
      prix('GAZOLE', 1.749, '2026-11-14T08:15:00Z'),
      prix('SP95', 1.839, '2026-11-14T08:15:00Z'),
      prix('E10', 1.799, '2026-11-14T08:15:00Z'),
      prix('SP98', 1.889, '2026-11-14T08:15:00Z'),
    ],
    distanceKm: null,
  },
  {
    id: '35200102',
    nom: null,
    adresse: '2 avenue des Pays-Bas',
    ville: 'Rennes',
    codePostal: '35200',
    departement: ILLE_ET_VILAINE,
    region: BRETAGNE,
    position: { latitude: 48.0889, longitude: -1.6512 },
    emplacement: 'ROUTE',
    automate24h: true,
    horaires: semaine('08:30', '20:30'),
    services: ['Boutique alimentaire', 'DAB (Distributeur automatique de billets)'],
    carburants: [
      prix('GAZOLE', 1.689, '2026-11-14T07:50:00Z'),
      prix('E10', 1.759, '2026-11-14T07:50:00Z'),
      rupture('E85', 'TEMPORAIRE', '2026-11-12T09:15:00Z', 0.899, '2026-11-10T09:00:00Z'),
      rupture('SP98', 'DEFINITIVE', '2026-03-27T17:43:44Z'),
    ],
    distanceKm: null,
  },
  {
    id: '35510103',
    nom: null,
    adresse: 'Aire de Cesson-Sévigné, A84',
    ville: 'Cesson-Sévigné',
    codePostal: '35510',
    departement: ILLE_ET_VILAINE,
    region: BRETAGNE,
    position: { latitude: 48.1208, longitude: -1.5966 },
    emplacement: 'AUTOROUTE',
    automate24h: true,
    horaires: [],
    services: ['Toilettes publiques', 'Restauration à emporter', 'Piste poids lourds'],
    carburants: [
      prix('GAZOLE', 1.899, '2026-11-14T06:30:00Z'),
      prix('SP95', 1.979, '2026-11-14T06:30:00Z'),
      prix('SP98', 2.029, '2026-11-14T06:30:00Z'),
      prix('GPLC', 0.999, '2026-11-13T18:00:00Z'),
    ],
    distanceKm: null,
  },
  {
    id: '35700104',
    nom: null,
    adresse: 'Allée Morvan Lebesque',
    ville: 'Rennes',
    codePostal: '35700',
    departement: ILLE_ET_VILAINE,
    region: BRETAGNE,
    position: { latitude: 48.131, longitude: -1.633 },
    emplacement: 'ROUTE',
    automate24h: false,
    horaires: semaine('07:30', '19:30', { fermes: ['dimanche'], inconnus: ['samedi'] }),
    services: [],
    carburants: [
      prix('GAZOLE', 1.719, '2026-11-13T16:20:00Z'),
      prix('E10', 1.779, '2026-11-13T16:20:00Z'),
      prix('E85', 0.879, '2026-11-13T16:20:00Z'),
    ],
    distanceKm: null,
  },
  {
    id: '44000105',
    nom: null,
    adresse: '3 quai de la Fosse',
    ville: 'Nantes',
    codePostal: '44000',
    departement: LOIRE_ATLANTIQUE,
    region: PAYS_DE_LA_LOIRE,
    position: { latitude: 47.2116, longitude: -1.5655 },
    emplacement: 'ROUTE',
    automate24h: true,
    horaires: semaine('06:00', '22:00'),
    services: ['Boutique alimentaire', 'Station de gonflage'],
    carburants: [
      prix('GAZOLE', 1.779, '2026-11-14T09:05:00Z'),
      prix('SP95', 1.859, '2026-11-14T09:05:00Z'),
      prix('E10', 1.819, '2026-11-14T09:05:00Z'),
    ],
    distanceKm: null,
  },
  {
    id: '44300106',
    nom: null,
    adresse: '45 route de Rennes',
    ville: 'Nantes',
    codePostal: '44300',
    departement: LOIRE_ATLANTIQUE,
    region: PAYS_DE_LA_LOIRE,
    position: { latitude: 47.2471, longitude: -1.5512 },
    emplacement: 'ROUTE',
    automate24h: false,
    horaires: semaine('08:00', '20:00', { fermes: ['dimanche'] }),
    services: ['Lavage manuel'],
    carburants: [
      prix('GAZOLE', 1.739, '2026-11-14T07:40:00Z'),
      prix('SP98', 1.909, '2026-11-14T07:40:00Z'),
      rupture('SP95', 'TEMPORAIRE', '2026-11-13T12:00:00Z'),
    ],
    distanceKm: null,
  },
];

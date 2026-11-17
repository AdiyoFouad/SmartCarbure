import { libelleCarburant } from '../constants/carburants';

// Format imposé par le contrat d'API (docs/contrat-api.md, « Export CSV »)
const SEPARATEUR = ';';
const FIN_DE_LIGNE = '\r\n';
/** BOM UTF-8 : sans lui, Excel affiche mal les accents (« Ã© » au lieu de « é »). */
const BOM = '\uFEFF';
const EN_TETE = [
  'Nom',
  'Adresse',
  'Code postal',
  'Ville',
  'Carburant',
  'Prix (€/L)',
  'Disponible',
  'Distance (km)',
  'Mise à jour',
];

/**
 * Prépare une valeur pour une cellule : vide si absente, entre guillemets si elle contient
 * un point-virgule, un guillemet ou un retour à la ligne (les guillemets internes sont doublés).
 */
function cellule(valeur) {
  if (valeur === null || valeur === undefined) {
    return '';
  }
  const texte = String(valeur);
  if (/[;"\r\n]/.test(texte)) {
    return `"${texte.replace(/"/g, '""')}"`;
  }
  return texte;
}

/** Nombre avec une virgule décimale (1.749 → « 1,749 ») ; vide si le nombre est absent. */
function nombre(valeur) {
  if (valeur === null || valeur === undefined || Number.isNaN(valeur)) {
    return '';
  }
  return String(valeur).replace('.', ',');
}

/** Une ligne du fichier : la station et son prix pour le carburant recherché. */
function ligneStation(station, carburant) {
  // Si la station ne vend pas ce carburant, les colonnes de prix restent vides
  const prix = (station.carburants || []).find((c) => c.type === carburant);
  return [
    station.nom,
    station.adresse,
    station.codePostal,
    station.ville,
    libelleCarburant(carburant),
    prix ? nombre(prix.prix) : '',
    prix ? (prix.disponible ? 'Oui' : 'Non') : '',
    nombre(station.distanceKm), // vide pour une recherche par ville (distanceKm null)
    prix ? prix.dateMaj : '',
  ];
}

/**
 * Génère le contenu CSV des résultats d'une recherche (F18), au format du contrat d'API.
 * @param {Array<object>} stations stations affichées
 * @param {string} carburant code du carburant recherché
 * @returns {string} contenu du fichier CSV
 */
export function genererCsv(stations, carburant) {
  const lignes = [EN_TETE, ...(stations || []).map((station) => ligneStation(station, carburant))];
  return BOM + lignes.map((colonnes) => colonnes.map(cellule).join(SEPARATEUR) + FIN_DE_LIGNE).join('');
}

/**
 * Nom du fichier exporté : smartcarbure-stations-AAAA-MM-JJ.csv (date du jour, en heure locale).
 * @param {Date} [date=new Date()] date à utiliser (utile pour les tests)
 * @returns {string} nom du fichier
 */
export function nomFichierCsv(date = new Date()) {
  const annee = date.getFullYear();
  const mois = String(date.getMonth() + 1).padStart(2, '0');
  const jour = String(date.getDate()).padStart(2, '0');
  return `smartcarbure-stations-${annee}-${mois}-${jour}.csv`;
}

/**
 * Propose au navigateur le téléchargement d'un fichier CSV.
 * @param {string} contenu contenu CSV
 * @param {string} nomFichier nom du fichier proposé (ex. « stations.csv »)
 */
export function telechargerCsv(contenu, nomFichier) {
  // Le fichier est créé en mémoire (Blob), puis un lien invisible le fait télécharger
  const blob = new Blob([contenu], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const lien = document.createElement('a');
  lien.href = url;
  lien.download = nomFichier;
  document.body.appendChild(lien);
  lien.click();
  lien.remove();
  // On libère l'URL juste après, pour laisser au navigateur le temps de lancer le téléchargement
  setTimeout(() => URL.revokeObjectURL(url), 0);
}

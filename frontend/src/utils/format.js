// Fonctions de formatage partagées par toutes les pages (prix, distances, dates).
// Elles acceptent null ou undefined sans planter : le backend renvoie null pour les valeurs inconnues.

const FORMAT_PRIX = new Intl.NumberFormat('fr-FR', {
  minimumFractionDigits: 3,
  maximumFractionDigits: 3,
});

const FORMAT_KM = new Intl.NumberFormat('fr-FR', {
  minimumFractionDigits: 1,
  maximumFractionDigits: 1,
});

const FORMAT_DATE = new Intl.DateTimeFormat('fr-FR', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  timeZone: 'Europe/Paris',
});

/**
 * Convertit une date ISO 8601 (ou un objet Date) en Date valide.
 * @param {string|Date|null|undefined} date
 * @returns {Date|null} la date, ou null si elle est absente ou invalide
 */
function versDate(date) {
  if (date == null || date === '') {
    return null;
  }
  const resultat = date instanceof Date ? date : new Date(date);
  return Number.isNaN(resultat.getTime()) ? null : resultat;
}

/**
 * Formate un prix au litre avec trois décimales : 1.789 devient « 1,789 € ».
 * @param {number|null|undefined} prix prix en euros
 * @returns {string} prix formaté, ou « Prix indisponible » si le prix est inconnu (carburant en rupture)
 */
export function formaterPrix(prix) {
  if (typeof prix !== 'number' || !Number.isFinite(prix)) {
    return 'Prix indisponible';
  }
  return `${FORMAT_PRIX.format(prix)} €`;
}

/**
 * Formate une distance : « 450 m » sous 1 km, « 3,2 km » au-delà.
 * @param {number|null|undefined} distanceKm distance en kilomètres
 * @returns {string} distance formatée, ou chaîne vide si la distance est inconnue (recherche par ville, fiche)
 */
export function formaterDistance(distanceKm) {
  if (typeof distanceKm !== 'number' || !Number.isFinite(distanceKm) || distanceKm < 0) {
    return '';
  }
  if (distanceKm < 1) {
    return `${Math.round(distanceKm * 1000)} m`;
  }
  return `${FORMAT_KM.format(distanceKm)} km`;
}

/**
 * Formate la date de mise à jour d'un prix : « mis à jour il y a 25 min ».
 * Moins d'une minute (ou date dans le futur) : « mis à jour à l'instant » ; puis minutes, heures, jours.
 * @param {string|Date|null|undefined} date date ISO 8601 ou objet Date
 * @param {Date} [maintenant=new Date()] date de référence (utile pour les tests)
 * @returns {string} texte de mise à jour, ou chaîne vide si la date est inconnue
 */
export function formaterDateMaj(date, maintenant = new Date()) {
  const d = versDate(date);
  if (!d) {
    return '';
  }
  const minutes = Math.floor((maintenant.getTime() - d.getTime()) / 60000);
  if (minutes < 1) {
    return "mis à jour à l'instant";
  }
  if (minutes < 60) {
    return `mis à jour il y a ${minutes} min`;
  }
  const heures = Math.floor(minutes / 60);
  if (heures < 24) {
    return `mis à jour il y a ${heures} h`;
  }
  return `mis à jour il y a ${Math.floor(heures / 24)} j`;
}

/**
 * Formate une date au format français, à l'heure de Paris : « 26/10/2026 ».
 * @param {string|Date|null|undefined} date date ISO 8601 ou objet Date
 * @returns {string} date formatée, ou chaîne vide si la date est inconnue
 */
export function formaterDate(date) {
  const d = versDate(date);
  return d ? FORMAT_DATE.format(d) : '';
}

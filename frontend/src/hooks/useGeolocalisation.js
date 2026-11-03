/**
 * Demande la position de l'utilisateur au navigateur (navigator.geolocation).
 * Un refus ne doit jamais faire planter l'application : un message invitant à saisir
 * une ville ou un code postal est fourni dans `erreur` (test R02).
 * TODO : à implémenter.
 * @returns {{
 *   position: {latitude: number, longitude: number}|null,
 *   chargement: boolean,
 *   erreur: string|null,
 *   localiser: () => void
 * }}
 */
export function useGeolocalisation() {
  // TODO
}

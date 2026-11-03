/**
 * Exécute un appel de service et suit son état : chargement, données ou erreur.
 * Doit relancer l'appel quand une dépendance change, ignorer une réponse arrivée trop tard,
 * et fournir une fonction `relancer` pour le bouton « Réessayer ».
 * TODO : à implémenter.
 * @param {() => Promise<any>} appel fonction qui appelle un service
 * @param {Array<any>} dependances valeurs qui déclenchent un nouvel appel
 * @returns {{donnees: any, chargement: boolean, erreur: Error|null, relancer: () => void}}
 */
export function useRequete(appel, dependances) {
  // TODO
}

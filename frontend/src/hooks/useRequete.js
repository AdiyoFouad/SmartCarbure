import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Exécute un appel de service et suit son état : chargement, données ou erreur.
 * Relance l'appel quand une dépendance change, ignore une réponse arrivée trop tard
 * (après un changement de dépendance ou un nouvel appel) et fournit `relancer`
 * pour le bouton « Réessayer ».
 *
 * Exemple : const { donnees, chargement, erreur, relancer } = useRequete(() => obtenirStation(id), [id]);
 *
 * @param {() => Promise<any>} appel fonction qui appelle un service
 * @param {Array<any>} dependances valeurs qui déclenchent un nouvel appel
 * @returns {{donnees: any, chargement: boolean, erreur: Error|null, relancer: () => void}}
 */
export function useRequete(appel, dependances = []) {
  const [etat, setEtat] = useState({ donnees: null, chargement: true, erreur: null });
  const [tentative, setTentative] = useState(0);
  const numeroAppel = useRef(0);
  const appelCourant = useRef(appel);
  appelCourant.current = appel;

  useEffect(() => {
    const numero = ++numeroAppel.current;
    let actif = true;
    setEtat((precedent) => ({ ...precedent, chargement: true, erreur: null }));

    Promise.resolve()
      .then(() => appelCourant.current())
      .then(
        (donnees) => {
          if (actif && numero === numeroAppel.current) {
            setEtat({ donnees, chargement: false, erreur: null });
          }
        },
        (erreur) => {
          if (actif && numero === numeroAppel.current) {
            setEtat({ donnees: null, chargement: false, erreur });
          }
        }
      );

    return () => {
      actif = false;
    };
    // Les dépendances sont fournies par l'appelant, comme pour useEffect.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...dependances, tentative]);

  const relancer = useCallback(() => setTentative((n) => n + 1), []);

  return { ...etat, relancer };
}

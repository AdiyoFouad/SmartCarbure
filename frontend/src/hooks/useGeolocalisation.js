import { useState } from 'react';

/**
 * Demande la position de l'utilisateur au navigateur (navigator.geolocation).
 * Un refus ne doit jamais faire planter l'application : un message invitant à saisir
 * une ville ou un code postal est fourni dans `erreur` (test R02).
 * @returns {{
 *   position: {latitude: number, longitude: number}|null,
 *   chargement: boolean,
 *   erreur: string|null,
 *   localiser: () => void
 * }}
 */
export function useGeolocalisation() {
  const [position, setPosition] = useState(null);
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState(null);

  const localiser = () => {
    if (!navigator.geolocation) {
      setErreur('La géolocalisation n\'est pas disponible dans ce navigateur.');
      return;
    }

    setChargement(true);
    setErreur(null);
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        setPosition({ latitude: coords.latitude, longitude: coords.longitude });
        setChargement(false);
      },
      (cause) => {
        const messages = {
          1: 'La localisation a été refusée. Vous pouvez saisir une ville ou un code postal.',
          2: 'Votre position est momentanément indisponible. Réessayez ou saisissez une ville.',
          3: 'La localisation a pris trop de temps. Réessayez ou saisissez une ville.',
        };
        setErreur(messages[cause.code] || 'Impossible de déterminer votre position. Saisissez une ville ou un code postal.');
        setChargement(false);
      },
      { enableHighAccuracy: false, timeout: 10000, maximumAge: 60000 },
    );
  };

  return { position, chargement, erreur, localiser };
}

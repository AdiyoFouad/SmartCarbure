import { useContext } from 'react';
import { AuthContext } from '../context/AuthContext';

/**
 * Donne accès à l'utilisateur connecté et aux actions d'authentification.
 * @returns {{
 *   utilisateur: object|null,
 *   jeton: string|null,
 *   estConnecte: boolean,
 *   connecter: (email: string, motDePasse: string) => Promise<object>,
 *   inscrire: (nom: string, email: string, motDePasse: string) => Promise<object>,
 *   deconnecter: () => void,
 *   mettreAJourUtilisateur: (modifications: object) => void
 * }}
 * @throws {Error} si le composant n'est pas placé sous un AuthProvider
 */
export function useAuth() {
  const contexte = useContext(AuthContext);
  if (!contexte) {
    throw new Error('useAuth doit être utilisé dans un AuthProvider.');
  }
  return contexte;
}

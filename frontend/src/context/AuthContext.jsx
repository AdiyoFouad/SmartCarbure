import { createContext } from 'react';

export const AuthContext = createContext(null);

/**
 * Fournit l'utilisateur connecté et les actions d'authentification à toute l'application.
 * TODO : à implémenter.
 * - conserver { jeton, utilisateur } dans un état (et dans le localStorage pour survivre au rechargement) ;
 * - connecter(email, motDePasse) : appeler authService.connecter, puis definirJeton (services/api.js) ;
 * - inscrire(nom, email, motDePasse) : appeler authService.inscrire ;
 * - deconnecter() : effacer la session et le jeton ;
 * - mettreAJourUtilisateur(modifications) : par exemple après l'enregistrement des préférences.
 */
export function AuthProvider({ children }) {
  const valeur = {
    utilisateur: null,
    jeton: null,
    estConnecte: false,
    connecter: async () => {},
    inscrire: async () => {},
    deconnecter: () => {},
    mettreAJourUtilisateur: () => {},
  };

  return <AuthContext.Provider value={valeur}>{children}</AuthContext.Provider>;
}

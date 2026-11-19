import { createContext, useState, useEffect } from 'react';
import { connecter as apiConnecter, inscrire as apiInscrire } from '../services/authService';
import { definirJeton } from '../services/api';

export const AuthContext = createContext(null);

export const CLE_STOCKAGE_AUTH = 'smartcarbure_session';

/**
 * Fournit l'utilisateur connecté et les actions d'authentification à toute l'application.
 */
export function AuthProvider({ children }) {
  const [session, setSession] = useState(() => {
    try {
      const stocke = localStorage.getItem(CLE_STOCKAGE_AUTH);
      if (stocke) {
        const donnees = JSON.parse(stocke);
        if (donnees?.jeton && donnees?.utilisateur) {
          definirJeton(donnees.jeton);
          return donnees;
        }
      }
    } catch {
      // Ignorer les erreurs d'accès au localStorage
    }
    return { jeton: null, utilisateur: null };
  });

  // Synchronise le jeton dans api.js dès que la session change
  useEffect(() => {
    definirJeton(session.jeton);
  }, [session.jeton]);

  /**
   * Connecte un utilisateur, enregistre le jeton et stocke la session dans le localStorage.
   * @param {string} email
   * @param {string} motDePasse
   * @returns {Promise<{jeton: string, utilisateur: object}>}
   */
  const connecter = async (email, motDePasse) => {
    const reponse = await apiConnecter({ email, motDePasse });
    const nouvelleSession = {
      jeton: reponse.jeton,
      utilisateur: reponse.utilisateur,
    };
    definirJeton(nouvelleSession.jeton);
    setSession(nouvelleSession);
    try {
      localStorage.setItem(CLE_STOCKAGE_AUTH, JSON.stringify(nouvelleSession));
    } catch {
      // Ignorer
    }
    return reponse;
  };

  /**
   * Crée un compte utilisateur via le backend.
   * @param {string} nom
   * @param {string} email
   * @param {string} motDePasse
   * @returns {Promise<object>} l'utilisateur créé
   */
  const inscrire = async (nom, email, motDePasse) => {
    return apiInscrire({ nom, email, motDePasse });
  };

  /**
   * Déconnecte l'utilisateur en effaçant le jeton en mémoire et dans le stockage local.
   */
  const deconnecter = () => {
    definirJeton(null);
    setSession({ jeton: null, utilisateur: null });
    try {
      localStorage.removeItem(CLE_STOCKAGE_AUTH);
    } catch {
      // Ignorer
    }
  };

  /**
   * Met à jour les informations de l'utilisateur en mémoire et dans le stockage local
   * (ex: lors de l'enregistrement de nouvelles préférences).
   * @param {object} modifications
   */
  const mettreAJourUtilisateur = (modifications) => {
    setSession((precedente) => {
      if (!precedente.utilisateur) {
        return precedente;
      }
      const nouvelleSession = {
        ...precedente,
        utilisateur: { ...precedente.utilisateur, ...modifications },
      };
      try {
        localStorage.setItem(CLE_STOCKAGE_AUTH, JSON.stringify(nouvelleSession));
      } catch {
        // Ignorer
      }
      return nouvelleSession;
    });
  };

  const valeur = {
    utilisateur: session.utilisateur,
    jeton: session.jeton,
    estConnecte: Boolean(session.jeton && session.utilisateur),
    connecter,
    inscrire,
    deconnecter,
    mettreAJourUtilisateur,
  };

  return <AuthContext.Provider value={valeur}>{children}</AuthContext.Provider>;
}

import { api, ApiError } from './api';
import { MODE_DEMO, reponseSimulee } from './mock';
import { UTILISATEURS } from '../data/utilisateurs';

// En mode démo, on conserve une copie en mémoire pour simuler les inscriptions sans altérer le module
let utilisateursDemo = [...UTILISATEURS];

/**
 * Connecte un utilisateur : POST /api/auth/connexion { email, motDePasse }.
 * @param {{email: string, motDePasse: string}} identifiants
 * @returns {Promise<{jeton: string, utilisateur: object}>} jeton et utilisateur connecté
 * @throws {ApiError} 401 « Email ou mot de passe incorrect », 503 si le service est indisponible
 */
export async function connecter(identifiants) {
  if (MODE_DEMO) {
    return reponseSimulee(() => {
      const email = identifiants?.email?.trim().toLowerCase();
      const mdp = identifiants?.motDePasse;
      const utilisateur = utilisateursDemo.find(
        (u) => u.email.toLowerCase() === email && u.motDePasse === mdp,
      );
      if (!utilisateur) {
        throw new ApiError(401, 'Email ou mot de passe incorrect');
      }
      const { motDePasse: _, ...sansMdp } = utilisateur;
      return {
        jeton: `jwt-demo-${utilisateur.id}-${Date.now()}`,
        utilisateur: sansMdp,
      };
    });
  }

  return api.post('/auth/connexion', identifiants);
}

/**
 * Crée un compte : POST /api/auth/inscription { nom, email, motDePasse }.
 * @param {{nom: string, email: string, motDePasse: string}} compte
 * @returns {Promise<object>} utilisateur créé
 * @throws {ApiError} 400 si la saisie est invalide, 409 « Cet email est déjà utilisé »
 */
export async function inscrire(compte) {
  if (MODE_DEMO) {
    return reponseSimulee(() => {
      const nom = compte?.nom?.trim();
      const email = compte?.email?.trim().toLowerCase();
      const motDePasse = compte?.motDePasse;

      if (!nom || !email || !motDePasse) {
        throw new ApiError(400, 'Saisie invalide, vérifiez les champs du formulaire.');
      }
      if (email === 'test@' || !email.includes('@') || email.endsWith('@')) {
        throw new ApiError(400, "Format d'adresse email invalide.");
      }
      if (motDePasse.length < 8) {
        throw new ApiError(400, 'Le mot de passe doit comporter au moins 8 caractères.');
      }
      if (utilisateursDemo.some((u) => u.email.toLowerCase() === email)) {
        throw new ApiError(409, 'Cet email est déjà utilisé');
      }

      const nouvelUtilisateur = {
        id: utilisateursDemo.length + 1,
        nom,
        email,
        motDePasse,
        carburantPrefere: null,
        rayonPrefere: null,
      };
      utilisateursDemo.push(nouvelUtilisateur);
      const { motDePasse: _, ...sansMdp } = nouvelUtilisateur;
      return sansMdp;
    });
  }

  return api.post('/auth/inscription', compte);
}

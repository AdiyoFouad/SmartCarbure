/**
 * Connecte un utilisateur : POST /api/auth/connexion { email, motDePasse }.
 * TODO : à implémenter.
 * @param {{email: string, motDePasse: string}} identifiants
 * @returns {Promise<{jeton: string, utilisateur: object}>} jeton et utilisateur connecté
 * @throws {ApiError} 401 « Email ou mot de passe incorrect », 503 si le service est indisponible
 */
export async function connecter(identifiants) {
  // TODO
}

/**
 * Crée un compte : POST /api/auth/inscription { nom, email, motDePasse }.
 * TODO : à implémenter.
 * @param {{nom: string, email: string, motDePasse: string}} compte
 * @returns {Promise<object>} utilisateur créé
 * @throws {ApiError} 400 si la saisie est invalide, 409 « Cet email est déjà utilisé »
 */
export async function inscrire(compte) {
  // TODO
}

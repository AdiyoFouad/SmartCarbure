/**
 * Protège une page réservée aux utilisateurs connectés (/favoris, /preferences).
 * TODO : rediriger vers /connexion avec <Navigate> si l'utilisateur n'est pas connecté (useAuth).
 * En attendant, la page est affichée sans vérification.
 */
function RouteProtegee({ children }) {
  return children;
}

export default RouteProtegee;

import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

/**
 * Protège une page réservée aux utilisateurs connectés (/favoris, /preferences).
 * Redirige vers /connexion avec <Navigate> si l'utilisateur n'est pas connecté.
 * Conserve l'emplacement d'origine dans location.state.from pour y revenir après connexion.
 */
function RouteProtegee({ children }) {
  const { estConnecte } = useAuth();
  const location = useLocation();

  if (!estConnecte) {
    return <Navigate to="/connexion" state={{ from: location }} replace />;
  }

  return children;
}

export default RouteProtegee;

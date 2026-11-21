import { Link, useLocation, useNavigate } from 'react-router-dom';
import { ArrowLeft, MapPinOff } from 'lucide-react';
import './PageIntrouvable.css';

/**
 * Page affichée pour une adresse inconnue (route *) : message, lien vers l'accueil
 * et bouton « Page précédente ».
 */
function PageIntrouvable() {
  const navigate = useNavigate();
  const location = useLocation();
  // location.key vaut « default » pour la première page ouverte (lien direct, nouvel onglet) :
  // il n'y a alors pas de page précédente dans l'application, on va à l'accueil.
  const pagePrecedente = () => (location.key !== 'default' ? navigate(-1) : navigate('/'));

  return (
    <section className="page-introuvable" aria-labelledby="page-introuvable-titre">
      <title>Page introuvable – SmartCarbure</title>
      <MapPinOff className="page-introuvable__icone" size={48} aria-hidden="true" />
      <h1 id="page-introuvable-titre">Page introuvable</h1>
      <p className="page-introuvable__texte">L'adresse demandée n'existe pas ou a été déplacée.</p>
      <div className="page-introuvable__actions">
        <Link to="/" className="page-introuvable__bouton page-introuvable__bouton--principal">
          Retour à l'accueil
        </Link>
        <button
          type="button"
          className="page-introuvable__bouton page-introuvable__bouton--secondaire"
          onClick={pagePrecedente}
        >
          <ArrowLeft size={18} aria-hidden="true" />
          Page précédente
        </button>
      </div>
    </section>
  );
}

export default PageIntrouvable;

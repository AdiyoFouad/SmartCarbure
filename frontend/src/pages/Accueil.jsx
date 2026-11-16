import { Clock, MapPin, PiggyBank, Route as RouteIcon, Scale } from 'lucide-react';
import { Link } from 'react-router-dom';
import FormulaireRecherche from '../components/FormulaireRecherche';
import './Accueil.css';

function Accueil() {
  return (
    <div className="page-accueil">
      <title>SmartCarbure – Trouvez le carburant le moins cher</title>
      <meta
        name="description"
        content="Comparez les prix des carburants autour de vous, sur votre trajet ou entre deux villes."
      />
      <meta property="og:title" content="SmartCarbure – Trouvez le carburant le moins cher" />
      <meta property="og:description" content="Comparez les prix des carburants autour de vous en France." />
      <meta property="og:type" content="website" />
      <meta name="twitter:card" content="summary_large_image" />

      <section className="accueil-hero" aria-labelledby="accueil-titre">
        <div className="accueil-hero__contenu">
          <p className="accueil-hero__etiquette">
            <PiggyBank aria-hidden="true" /> Prix officiels mis à jour en continu
          </p>
          <h1 id="accueil-titre">
            Trouvez le carburant le <span>moins cher</span> près de chez vous
          </h1>
          <p className="accueil-hero__description">
            Comparez les stations autour de vous en quelques secondes et faites des économies à chaque plein.
          </p>
          <FormulaireRecherche />
          <p className="accueil-hero__exemple">
            Essayez « Rennes », « 44000 » ou « Saint-Herblain ».
          </p>
        </div>
        <div className="accueil-hero__repere" aria-hidden="true">
          <span>Prix au litre</span>
          <strong>Plus clair. Plus proche.</strong>
        </div>
      </section>

      <section className="accueil-options" aria-label="Autres façons de comparer">
        <div className="accueil-option accueil-option--rayon">
          <MapPin aria-hidden="true" />
          <span className="accueil-option__titre">Autour de vous</span>
          <span className="accueil-option__texte">
            Choisissez un rayon et triez les stations par prix ou par distance.
          </span>
          <span className="accueil-option__action">Recherche locale</span>
        </div>
        <Link to="/trajet" className="accueil-option">
          <RouteIcon aria-hidden="true" />
          <span className="accueil-option__titre">Sur votre trajet</span>
          <span className="accueil-option__texte">
            Repérez les stations intéressantes entre votre départ et votre arrivée.
          </span>
          <span className="accueil-option__action">Préparer un trajet <span aria-hidden="true">→</span></span>
        </Link>
        <Link to="/comparer" className="accueil-option">
          <Scale aria-hidden="true" />
          <span className="accueil-option__titre">Entre deux zones</span>
          <span className="accueil-option__texte">
            Comparez les prix moyens de deux villes ou départements.
          </span>
          <span className="accueil-option__action">Comparer des zones <span aria-hidden="true">→</span></span>
        </Link>
      </section>

      <p className="accueil-note">
        <Clock aria-hidden="true" /> Prix relevés par les stations, en euros par litre.
      </p>
    </div>
  );
}

export default Accueil;

import './Footer.css';

/** Lien externe ouvert dans un nouvel onglet, annoncé comme tel aux lecteurs d'écran. */
function LienExterne({ href, children }) {
  return (
    <a href={href} target="_blank" rel="noopener noreferrer">
      {children}{' '}
      <span className="visuellement-cache">(nouvel onglet)</span>
    </a>
  );
}

/**
 * Pied de page affiché sur toutes les pages : source des données (prix-carburants.gouv.fr,
 * Ministère de l'Économie, Licence Ouverte / Etalab) et cadre du projet.
 * L'attribution OpenStreetMap est affichée directement sur chaque carte Leaflet.
 */
function Footer() {
  return (
    <footer className="footer">
      <div className="footer__contenu conteneur">
        <p>
          Données : <LienExterne href="https://www.prix-carburants.gouv.fr">prix-carburants.gouv.fr</LienExterne>{' '}
          – Ministère de l'Économie,{' '}
          <LienExterne href="https://www.etalab.gouv.fr/licence-ouverte-open-licence/">
            Licence Ouverte / Etalab
          </LienExterne>
        </p>
        <p>Projet PDL – Master 1 MIAGE, Université de Rennes</p>
      </div>
    </footer>
  );
}

export default Footer;

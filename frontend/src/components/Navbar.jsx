import { useState } from 'react';
import { Link, NavLink } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import './Navbar.css';

const LIENS = [
  { vers: '/', libelle: 'Rechercher', fin: true },
  { vers: '/trajet', libelle: 'Trajet' },
  { vers: '/comparer', libelle: 'Comparer' },
];

const LIENS_CONNECTE = [
  { vers: '/favoris', libelle: 'Mes favoris' },
  { vers: '/preferences', libelle: 'Préférences' },
];

/** Barre de navigation principale, repliée derrière un bouton « Menu » sur mobile. */
function Navbar() {
  const { utilisateur, estConnecte, deconnecter } = useAuth();
  const [menuOuvert, setMenuOuvert] = useState(false);
  const fermerMenu = () => setMenuOuvert(false);
  const liens = estConnecte ? [...LIENS, ...LIENS_CONNECTE] : LIENS;

  return (
    <header className="navbar">
      <nav className="navbar__contenu conteneur" aria-label="Navigation principale">
        <Link to="/" className="navbar__marque" onClick={fermerMenu}>
          SmartCarbure
        </Link>
        <button
          type="button"
          className="navbar__bouton-menu"
          aria-expanded={menuOuvert}
          aria-controls="navbar-menu"
          onClick={() => setMenuOuvert((ouvert) => !ouvert)}
        >
          Menu
        </button>
        <ul id="navbar-menu" className={`navbar__liens${menuOuvert ? ' navbar__liens--ouvert' : ''}`}>
          {liens.map(({ vers, libelle, fin }) => (
            <li key={vers}>
              <NavLink to={vers} end={fin} className="navbar__lien" onClick={fermerMenu}>
                {libelle}
              </NavLink>
            </li>
          ))}
          <li className="navbar__compte">
            {estConnecte ? (
              <>
                <span className="navbar__utilisateur">{utilisateur?.nom}</span>
                <button
                  type="button"
                  className="navbar__lien navbar__deconnexion"
                  onClick={() => {
                    deconnecter();
                    fermerMenu();
                  }}
                >
                  Se déconnecter
                </button>
              </>
            ) : (
              <NavLink to="/connexion" className="navbar__lien" onClick={fermerMenu}>
                Se connecter
              </NavLink>
            )}
          </li>
        </ul>
      </nav>
    </header>
  );
}

export default Navbar;

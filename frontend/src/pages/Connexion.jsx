import { useState } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import { LogIn, UserPlus, AlertCircle, CheckCircle2, LogOut } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import { ApiError } from '../services/api';
import './Connexion.css';

const REGEX_EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Page de connexion et de création de compte (/connexion) avec deux onglets (F11, F12).
 */
function Connexion() {
  const { estConnecte, utilisateur, connecter, inscrire, deconnecter } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [onglet, setOnglet] = useState('connexion');

  // Champs de connexion
  const [emailConnexion, setEmailConnexion] = useState('');
  const [mdpConnexion, setMdpConnexion] = useState('');

  // Champs d'inscription
  const [nomInscription, setNomInscription] = useState('');
  const [emailInscription, setEmailInscription] = useState('');
  const [mdpInscription, setMdpInscription] = useState('');

  // États de traitement
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState(null);
  const [succes, setSucces] = useState(null);

  const destination = location.state?.from?.pathname || '/';

  const changerOnglet = (nouvelOnglet) => {
    setOnglet(nouvelOnglet);
    setErreur(null);
    setSucces(null);
  };

  const handleConnexion = async (e) => {
    e.preventDefault();
    setErreur(null);
    setSucces(null);

    const email = emailConnexion.trim();
    if (!email || !mdpConnexion) {
      setErreur('Veuillez renseigner votre email et votre mot de passe.');
      return;
    }

    setChargement(true);
    try {
      await connecter(email, mdpConnexion);
      navigate(destination, { replace: true });
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.statut === 401) {
          setErreur('Email ou mot de passe incorrect');
        } else if (err.statut === 400) {
          setErreur('Saisie invalide, vérifiez les champs du formulaire.');
        } else if (err.statut === 503) {
          setErreur('Service temporairement indisponible. Réessayez dans quelques instants.');
        } else {
          setErreur(err.message || 'Une erreur est survenue.');
        }
      } else {
        setErreur('Une erreur inattendue est survenue.');
      }
    } finally {
      setChargement(false);
    }
  };

  const handleInscription = async (e) => {
    e.preventDefault();
    setErreur(null);
    setSucces(null);

    const nom = nomInscription.trim();
    const email = emailInscription.trim().toLowerCase();

    if (!nom) {
      setErreur('Veuillez renseigner votre nom.');
      return;
    }
    if (!email || !REGEX_EMAIL.test(email) || email === 'test@') {
      setErreur("Format d'adresse email invalide.");
      return;
    }
    if (!mdpInscription || mdpInscription.length < 8) {
      setErreur('Le mot de passe doit comporter au moins 8 caractères.');
      return;
    }
    if (mdpInscription.length > 72) {
      setErreur('Le mot de passe ne doit pas dépasser 72 caractères.');
      return;
    }

    setChargement(true);
    try {
      await inscrire(nom, email, mdpInscription);
      setSucces('Votre compte a été créé avec succès ! Vous pouvez maintenant vous connecter.');
      setEmailConnexion(email);
      setNomInscription('');
      setEmailInscription('');
      setMdpInscription('');
      setOnglet('connexion');
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.statut === 409) {
          setErreur('Cet email est déjà utilisé');
        } else if (err.statut === 400) {
          setErreur('Saisie invalide, vérifiez les champs du formulaire.');
        } else if (err.statut === 503) {
          setErreur('Service temporairement indisponible. Réessayez dans quelques instants.');
        } else {
          setErreur(err.message || 'Une erreur est survenue lors de la création de compte.');
        }
      } else {
        setErreur('Une erreur inattendue est survenue.');
      }
    } finally {
      setChargement(false);
    }
  };

  if (estConnecte) {
    return (
      <section className="page-connexion conteneur" aria-labelledby="titre-connecte">
        <div className="connexion-carte">
          <h1 id="titre-connecte" className="connexion-titre">Espace utilisateur</h1>
          <p className="connexion-description">
            Vous êtes connecté en tant que <strong>{utilisateur?.nom || utilisateur?.email}</strong> ({utilisateur?.email}).
          </p>

          <div className="connexion-connecte-liens">
            <Link to="/favoris" className="connexion-bouton connexion-bouton--secondaire">
              Mes favoris
            </Link>
            <Link to="/preferences" className="connexion-bouton connexion-bouton--secondaire">
              Mes préférences
            </Link>
            <button
              type="button"
              className="connexion-bouton connexion-bouton--danger"
              onClick={deconnecter}
            >
              <LogOut size={18} aria-hidden="true" />
              Se déconnecter
            </button>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="page-connexion conteneur" aria-labelledby="titre-connexion">
      <div className="connexion-carte">
        <h1 id="titre-connexion" className="connexion-titre">
          {onglet === 'connexion' ? 'Connexion' : 'Créer un compte'}
        </h1>
        <p className="connexion-description">
          Accédez à vos stations favorites et personnalisez vos préférences de recherche.
        </p>

        <div className="connexion-onglets" role="tablist" aria-label="Authentification">
          <button
            type="button"
            role="tab"
            id="onglet-connexion"
            aria-selected={onglet === 'connexion'}
            aria-controls="panneau-connexion"
            className={`connexion-onglet ${onglet === 'connexion' ? 'connexion-onglet--actif' : ''}`}
            onClick={() => changerOnglet('connexion')}
          >
            <LogIn size={18} aria-hidden="true" />
            Se connecter
          </button>
          <button
            type="button"
            role="tab"
            id="onglet-inscription"
            aria-selected={onglet === 'inscription'}
            aria-controls="panneau-inscription"
            className={`connexion-onglet ${onglet === 'inscription' ? 'connexion-onglet--actif' : ''}`}
            onClick={() => changerOnglet('inscription')}
          >
            <UserPlus size={18} aria-hidden="true" />
            Créer un compte
          </button>
        </div>

        {succes && (
          <div className="connexion-alerte connexion-alerte--succes" role="alert">
            <CheckCircle2 size={20} aria-hidden="true" className="connexion-alerte__icone" />
            <p>{succes}</p>
          </div>
        )}

        {erreur && (
          <div className="connexion-alerte connexion-alerte--erreur" role="alert">
            <AlertCircle size={20} aria-hidden="true" className="connexion-alerte__icone" />
            <p>{erreur}</p>
          </div>
        )}

        {onglet === 'connexion' ? (
          <form
            id="panneau-connexion"
            role="tabpanel"
            aria-labelledby="onglet-connexion"
            className="connexion-formulaire"
            onSubmit={handleConnexion}
            noValidate
          >
            <div className="connexion-champ">
              <label htmlFor="connexion-email">Adresse email</label>
              <input
                id="connexion-email"
                type="email"
                name="email"
                value={emailConnexion}
                onChange={(e) => setEmailConnexion(e.target.value)}
                placeholder="camille@exemple.fr"
                required
                autoComplete="email"
                disabled={chargement}
              />
            </div>

            <div className="connexion-champ">
              <label htmlFor="connexion-mdp">Mot de passe</label>
              <input
                id="connexion-mdp"
                type="password"
                name="motDePasse"
                value={mdpConnexion}
                onChange={(e) => setMdpConnexion(e.target.value)}
                placeholder="••••••••"
                required
                autoComplete="current-password"
                disabled={chargement}
              />
            </div>

            <button
              type="submit"
              className="connexion-bouton connexion-bouton--primaire"
              disabled={chargement}
            >
              {chargement ? 'Connexion en cours…' : 'Se connecter'}
            </button>
          </form>
        ) : (
          <form
            id="panneau-inscription"
            role="tabpanel"
            aria-labelledby="onglet-inscription"
            className="connexion-formulaire"
            onSubmit={handleInscription}
            noValidate
          >
            <div className="connexion-champ">
              <label htmlFor="inscription-nom">Nom complet</label>
              <input
                id="inscription-nom"
                type="text"
                name="nom"
                value={nomInscription}
                onChange={(e) => setNomInscription(e.target.value)}
                placeholder="Ex. Camille Martin"
                maxLength={100}
                required
                autoComplete="name"
                disabled={chargement}
              />
            </div>

            <div className="connexion-champ">
              <label htmlFor="inscription-email">Adresse email</label>
              <input
                id="inscription-email"
                type="email"
                name="email"
                value={emailInscription}
                onChange={(e) => setEmailInscription(e.target.value)}
                placeholder="Ex. camille@exemple.fr"
                required
                autoComplete="email"
                disabled={chargement}
              />
            </div>

            <div className="connexion-champ">
              <label htmlFor="inscription-mdp">Mot de passe</label>
              <input
                id="inscription-mdp"
                type="password"
                name="motDePasse"
                value={mdpInscription}
                onChange={(e) => setMdpInscription(e.target.value)}
                placeholder="8 caractères minimum"
                minLength={8}
                maxLength={72}
                required
                autoComplete="new-password"
                disabled={chargement}
              />
              <span className="connexion-champ__aide">Entre 8 et 72 caractères.</span>
            </div>

            <button
              type="submit"
              className="connexion-bouton connexion-bouton--primaire"
              disabled={chargement}
            >
              {chargement ? 'Création en cours…' : 'Créer un compte'}
            </button>
          </form>
        )}
      </div>
    </section>
  );
}

export default Connexion;

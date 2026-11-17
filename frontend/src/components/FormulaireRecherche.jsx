import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertCircle, Fuel, LocateFixed, Search, X } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import { useGeolocalisation } from '../hooks/useGeolocalisation';
import { CARBURANTS, CARBURANT_PAR_DEFAUT } from '../constants/carburants';
import './FormulaireRecherche.css';

function FormulaireRecherche({ initial, compact = false }) {
  const { utilisateur } = useAuth();
  const navigate = useNavigate();
  const { position, chargement, erreur: erreurGeo, localiser } = useGeolocalisation();
  const positionInitiale = initial?.lat != null && initial?.lon != null &&
    Number.isFinite(Number(initial.lat)) && Number.isFinite(Number(initial.lon))
    ? { latitude: Number(initial.lat), longitude: Number(initial.lon) }
    : null;
  const positionRecherche = position ?? positionInitiale;
  const [geo, setGeo] = useState(initial?.mode === 'geo');
  const [q, setQ] = useState(initial?.q ?? '');
  const [carburant, setCarburant] = useState(initial?.carburant ?? utilisateur?.carburantPrefere ?? CARBURANT_PAR_DEFAUT);
  const [rayon, setRayon] = useState(initial?.rayon ?? utilisateur?.rayonPrefere ?? 10);
  const [erreur, setErreur] = useState('');

  const soumettre = (event) => {
    event.preventDefault();
    if (geo && !positionRecherche) {
      if (!chargement) localiser();
      setErreur(erreurGeo || 'Autorisez la localisation ou indiquez une ville ou un code postal.');
      return;
    }
    if (!geo && !q.trim()) {
      setErreur('Indiquez une ville, un code postal ou utilisez « Me localiser ».');
      return;
    }

    setErreur('');
    const search = new URLSearchParams({ mode: geo ? 'geo' : 'ville', carburant, rayon: String(rayon) });
    if (geo) {
      search.set('lat', String(positionRecherche.latitude));
      search.set('lon', String(positionRecherche.longitude));
    } else {
      search.set('q', q.trim());
    }
    navigate(`/resultats?${search.toString()}`);
  };

  const demanderLocalisation = () => {
    setGeo(true);
    setErreur('');
    localiser();
  };

  return (
    <form className={`formulaire-recherche${compact ? ' formulaire-recherche--compact' : ''}`} onSubmit={soumettre}>
      <div className="formulaire-recherche__grille">
        <div className="formulaire-recherche__champ formulaire-recherche__lieu">
          <label htmlFor="recherche-lieu">Où cherchez-vous ?</label>
          <div className="formulaire-recherche__lieu-controles">
            {geo ? (
              <div className="formulaire-recherche__position">
                <span><LocateFixed aria-hidden="true" /> {chargement ? 'Localisation en cours…' : 'Ma position actuelle'}</span>
                <button
                  type="button"
                  aria-label="Saisir une ville"
                  onClick={() => {
                    setGeo(false);
                    setErreur('');
                  }}
                >
                  <X aria-hidden="true" />
                </button>
              </div>
            ) : (
              <input
                id="recherche-lieu"
                value={q}
                onChange={(event) => {
                  setQ(event.target.value);
                  setErreur('');
                }}
                placeholder="Ville ou code postal"
                autoComplete="postal-code address-level2"
              />
            )}
            {!geo && (
              <button type="button" className="formulaire-recherche__localiser" onClick={demanderLocalisation}>
                <LocateFixed aria-hidden="true" /> <span>Me localiser</span>
              </button>
            )}
          </div>
        </div>

        <div className="formulaire-recherche__champ">
          <label htmlFor="recherche-carburant">Carburant</label>
          <div className="formulaire-recherche__select">
            <Fuel aria-hidden="true" />
            <select
              id="recherche-carburant"
              value={carburant}
              onChange={(event) => setCarburant(event.target.value)}
            >
              {CARBURANTS.map(({ code, libelle }) => <option key={code} value={code}>{libelle}</option>)}
            </select>
          </div>
        </div>

        <div className="formulaire-recherche__champ formulaire-recherche__rayon">
          <label htmlFor="recherche-rayon">Rayon <span>{rayon} km</span></label>
          <input
            id="recherche-rayon"
            type="range"
            min="1"
            max="50"
            value={rayon}
            onChange={(event) => setRayon(Number(event.target.value))}
          />
        </div>

        <button type="submit" className="formulaire-recherche__submit" disabled={chargement}>
          <Search aria-hidden="true" /> Rechercher
        </button>
      </div>
      {(erreur || erreurGeo) && (
        <p className="formulaire-recherche__erreur" role="alert">
          <AlertCircle aria-hidden="true" /> {erreur || erreurGeo}
        </p>
      )}
    </form>
  );
}

export default FormulaireRecherche;

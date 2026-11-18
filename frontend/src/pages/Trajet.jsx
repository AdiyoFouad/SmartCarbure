import { useCallback, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import CarteTrajet from '../components/CarteTrajet';
import { prixCarburant, formaterPrixCarte } from '../components/CarteStations';
import MessageEtat, { TYPES_MESSAGE } from '../components/MessageEtat';
import { CARBURANTS, CARBURANT_PAR_DEFAUT } from '../constants/carburants';
import { MESSAGES_ERREUR } from '../services/api';
import { rechercherTrajet } from '../services/trajetService';
import './Trajet.css';

/** Nombre de stations les moins chères listées sous le formulaire. */
const NOMBRE_STATIONS_LISTEES = 5;

/**
 * Message d'erreur affiché : pour un 400, le message du backend (par exemple « Le trajet dépasse 500 km. »
 * ou « Aucun itinéraire routier trouvé entre ces deux points. »), sinon le message général du code.
 */
function messageErreur(erreur) {
  if (erreur.statut === 400) {
    return erreur.details?.message || MESSAGES_ERREUR[400];
  }
  return erreur.message || MESSAGES_ERREUR.inconnu;
}

/**
 * Recherche des stations le long d'un trajet (/trajet) (F6).
 * Un clic sur la carte place le départ, un second l'arrivée ; les marqueurs sont déplaçables.
 * Le calcul n'est lancé qu'au clic sur « Rechercher sur ce trajet » (pour ménager le quota du service d'itinéraire).
 */
function Trajet() {
  const [depart, setDepart] = useState(null);
  const [arrivee, setArrivee] = useState(null);
  const [carburant, setCarburant] = useState(CARBURANT_PAR_DEFAUT);
  const [resultat, setResultat] = useState(null);
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState(null);
  const numeroRecherche = useRef(0);

  /** Oublie le résultat affiché : il ne correspond plus aux marqueurs ou au carburant. */
  const oublierResultat = useCallback(() => {
    numeroRecherche.current += 1;
    setResultat(null);
    setErreur(null);
    setChargement(false);
  }, []);

  const changerDepart = useCallback((position) => {
    setDepart(position);
    oublierResultat();
  }, [oublierResultat]);

  const changerArrivee = useCallback((position) => {
    setArrivee(position);
    oublierResultat();
  }, [oublierResultat]);

  function changerCarburant(evenement) {
    setCarburant(evenement.target.value);
    oublierResultat();
  }

  function recommencer() {
    setDepart(null);
    setArrivee(null);
    oublierResultat();
  }

  async function rechercher() {
    const numero = ++numeroRecherche.current;
    setChargement(true);
    setErreur(null);
    setResultat(null);
    try {
      const reponse = await rechercherTrajet({ depart, arrivee, carburant });
      if (numero === numeroRecherche.current) {
        setResultat(reponse);
      }
    } catch (e) {
      if (numero === numeroRecherche.current) {
        setErreur(e);
      }
    } finally {
      if (numero === numeroRecherche.current) {
        setChargement(false);
      }
    }
  }

  const pret = Boolean(depart && arrivee);
  let consigne = 'Cliquez sur la carte pour placer le départ.';
  if (depart && !arrivee) {
    consigne = "Cliquez sur la carte pour placer l'arrivée.";
  } else if (pret) {
    consigne = 'Les marqueurs sont déplaçables. Lancez la recherche quand le trajet vous convient.';
  }

  const stations = resultat?.stations ?? [];
  const moinsCheres = stations
    .filter((station) => prixCarburant(station, carburant) != null)
    .slice(0, NOMBRE_STATIONS_LISTEES);

  return (
    <section className="page-trajet" aria-labelledby="trajet-titre">
      <div className="page-trajet__carte">
        <CarteTrajet
          depart={depart}
          arrivee={arrivee}
          onChangerDepart={changerDepart}
          onChangerArrivee={changerArrivee}
          trajet={resultat?.trajet}
          stations={stations}
          carburant={carburant}
        />
      </div>

      <div className="page-trajet__panneau">
        <h1 id="trajet-titre">Trajet</h1>
        <p className="page-trajet__consigne">{consigne}</p>

        <div className="page-trajet__champs">
          <label className="visuellement-cache" htmlFor="trajet-carburant">Carburant</label>
          <select id="trajet-carburant" value={carburant} onChange={changerCarburant}>
            {CARBURANTS.map(({ code, libelle }) => (
              <option key={code} value={code}>{libelle}</option>
            ))}
          </select>
          <button
            type="button"
            className="page-trajet__recommencer"
            onClick={recommencer}
            disabled={!depart && !arrivee}
            aria-label="Effacer le départ et l'arrivée"
            title="Effacer le départ et l'arrivée"
          >
            ↺
          </button>
        </div>

        <button
          type="button"
          className="page-trajet__rechercher"
          onClick={rechercher}
          disabled={!pret || chargement}
        >
          Rechercher sur ce trajet
        </button>

        {chargement && (
          <MessageEtat
            type={TYPES_MESSAGE.CHARGEMENT}
            message="Calcul du trajet en cours… Cela peut prendre jusqu'à 30 secondes pour un long trajet."
          />
        )}

        {erreur && (
          <MessageEtat
            type={TYPES_MESSAGE.ERREUR}
            message={messageErreur(erreur)}
            onReessayer={erreur.statut === 400 ? undefined : rechercher}
          />
        )}

        {resultat && (
          <div className="page-trajet__resultat">
            <p>
              Distance totale : <strong>{resultat.trajet.distanceKm.toLocaleString('fr-FR')} km</strong>
            </p>
            {stations.length === 0 ? (
              <MessageEtat
                type={TYPES_MESSAGE.VIDE}
                message="Aucune station trouvée le long de ce trajet pour ce carburant."
              />
            ) : (
              <>
                <p>
                  {stations.length} station{stations.length > 1 ? 's' : ''} le long du trajet.
                  {moinsCheres.length > 0 && ' Les moins chères :'}
                </p>
                <ol className="page-trajet__liste">
                  {moinsCheres.map((station) => (
                    <li key={station.id}>
                      <Link to={`/stations/${station.id}`}>
                        {station.adresse || station.nom || `Station ${station.id}`}
                        {station.ville ? `, ${station.ville}` : ''}
                      </Link>
                      <span className="page-trajet__prix">
                        {formaterPrixCarte(prixCarburant(station, carburant))}
                      </span>
                      <span className="page-trajet__distance">
                        à {station.distanceKm.toLocaleString('fr-FR')} km de la route
                      </span>
                    </li>
                  ))}
                </ol>
              </>
            )}
          </div>
        )}
      </div>
    </section>
  );
}

export default Trajet;

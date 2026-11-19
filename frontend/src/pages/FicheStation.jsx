import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, Clock, Fuel, MapPin, PiggyBank } from 'lucide-react';
import BadgeAutomate from '../components/BadgeAutomate';
import BadgeEmplacement from '../components/BadgeEmplacement';
import CalculEconomie, { lireLitres } from '../components/CalculEconomie';
import CarteStations, { positionValide } from '../components/CarteStations';
import Horaires from '../components/Horaires';
import MessageEtat, { TYPES_MESSAGE } from '../components/MessageEtat';
import TableauCarburants from '../components/TableauCarburants';
import { useRequete } from '../hooks/useRequete';
import { MESSAGES_ERREUR } from '../services/api';
import { calculerEconomie } from '../services/economieService';
import { obtenirStation } from '../services/stationService';
import './FicheStation.css';

/** Volume proposé par défaut pour le calcul d'économie. */
export const LITRES_PAR_DEFAUT = '50';

/** Délai entre la dernière saisie et l'appel au calcul d'économie (évite un appel par touche). */
export const DELAI_CALCUL_MS = 400;

/**
 * Titre de la fiche : le nom de la station s'il existe (l'API n'en fournit pas aujourd'hui),
 * sinon son adresse, sinon son identifiant. N'affiche jamais « null ».
 * @param {object} station
 * @returns {string}
 */
export function titreStation(station) {
  return station.nom || station.adresse || `Station ${station.id}`;
}

/** « 12 boulevard de la Liberté, 35000 Rennes », sans valeur vide ni « null ». */
export function ligneAdresse(station) {
  const lieu = [station.codePostal, station.ville].filter(Boolean).join(' ');
  return [station.adresse, lieu].filter(Boolean).join(', ');
}

/** « Ille-et-Vilaine (35), Bretagne », sans valeur vide ni « null ». */
export function ligneZone(station) {
  const { departement, region } = station;
  let texteDepartement = departement?.nom || '';
  if (texteDepartement && departement.code) {
    texteDepartement += ` (${departement.code})`;
  }
  return [texteDepartement, region?.nom].filter(Boolean).join(', ');
}

/** Carburants proposés au calcul d'économie : disponibles et avec un prix (ruptures exclues). */
export function carburantsDisponibles(station) {
  return (station?.carburants ?? []).filter(
    (c) => c.disponible && typeof c.prix === 'number' && Number.isFinite(c.prix) && c.prix > 0
  );
}

/**
 * Appelle economieService quand la station, le carburant ou le volume changent, après DELAI_CALCUL_MS.
 * Pas d'appel si le volume est invalide ; une réponse arrivée après un changement est ignorée.
 * @returns {{resultat: object|null, chargement: boolean, erreur: Error|null, relancer: () => void}}
 */
function useCalculEconomie(idStation, carburant, litres) {
  const [etat, setEtat] = useState({ resultat: null, chargement: false, erreur: null });
  const [tentative, setTentative] = useState(0);
  const volume = lireLitres(litres);

  useEffect(() => {
    if (!idStation || !carburant || volume === null) {
      setEtat({ resultat: null, chargement: false, erreur: null });
      return undefined;
    }
    let actif = true;
    setEtat((precedent) => ({ ...precedent, chargement: true, erreur: null }));
    const minuteur = setTimeout(() => {
      calculerEconomie({ idStation, carburant, litres: volume }).then(
        (resultat) => actif && setEtat({ resultat, chargement: false, erreur: null }),
        (erreur) => actif && setEtat({ resultat: null, chargement: false, erreur })
      );
    }, DELAI_CALCUL_MS);
    return () => {
      actif = false;
      clearTimeout(minuteur);
    };
  }, [idStation, carburant, volume, tentative]);

  return { ...etat, relancer: () => setTentative((n) => n + 1) };
}

/** Lien « Retour » : page précédente de l'application, ou l'accueil s'il n'y a pas d'historique. */
function BoutonRetour() {
  const navigate = useNavigate();
  const location = useLocation();
  // location.key vaut « default » pour la première page ouverte (lien direct, nouvel onglet).
  const retour = () => (location.key !== 'default' ? navigate(-1) : navigate('/'));

  return (
    <button type="button" className="page-fiche-station__retour" onClick={retour}>
      <ArrowLeft size={18} aria-hidden="true" />
      Retour
    </button>
  );
}

/** Message d'erreur affiché selon le code HTTP (messages du contrat d'API). */
function AffichageErreur({ erreur, relancer }) {
  if (erreur.statut === 404) {
    return (
      <MessageEtat type={TYPES_MESSAGE.ERREUR} message="Station introuvable.">
        <Link to="/">Retour à l'accueil</Link>
      </MessageEtat>
    );
  }
  const message = erreur.statut === 503 ? MESSAGES_ERREUR[503] : MESSAGES_ERREUR.inconnu;
  return <MessageEtat type={TYPES_MESSAGE.ERREUR} message={message} onReessayer={relancer} />;
}

/** Titre de carte (h2) précédé de son icône. */
function TitreCarte({ id, icone: Icone, children }) {
  return (
    <h2 id={id} className="page-fiche-station__titre-carte">
      <Icone size={20} aria-hidden="true" />
      {children}
    </h2>
  );
}

/**
 * Fiche détaillée d'une station (/stations/:id) : adresse, prix, date de mise à jour (F7), ruptures (F8),
 * calcul d'économie (F15), services, carte et horaires. Données : GET /api/stations/{id}.
 */
function FicheStation() {
  const { id } = useParams();
  const { donnees: station, chargement, erreur, relancer } = useRequete(() => obtenirStation(id), [id]);

  // Calcul d'économie : premier carburant disponible par défaut, 50 litres.
  const disponibles = carburantsDisponibles(station);
  const [carburantChoisi, setCarburantChoisi] = useState(null);
  const [litres, setLitres] = useState(LITRES_PAR_DEFAUT);
  const carburant = disponibles.some((c) => c.type === carburantChoisi)
    ? carburantChoisi
    : (disponibles[0]?.type ?? null);
  const economie = useCalculEconomie(station?.id, carburant, litres);

  if (chargement) {
    return (
      <section className="page-fiche-station">
        <MessageEtat type={TYPES_MESSAGE.CHARGEMENT} message="Chargement de la station…" />
      </section>
    );
  }

  if (erreur || !station) {
    return (
      <section className="page-fiche-station">
        <BoutonRetour />
        <h1 className="visuellement-cache">Fiche station</h1>
        <AffichageErreur erreur={erreur ?? { statut: 404 }} relancer={relancer} />
      </section>
    );
  }

  const adresse = ligneAdresse(station);
  const zone = ligneZone(station);

  return (
    <article className="page-fiche-station">
      <BoutonRetour />

      <header className="page-fiche-station__entete">
        {(station.automate24h || station.emplacement === 'AUTOROUTE') && (
          <div className="page-fiche-station__badges">
            {station.automate24h && <BadgeAutomate plein />}
            <BadgeEmplacement emplacement={station.emplacement} />
          </div>
        )}
        <h1 className="page-fiche-station__titre">{titreStation(station)}</h1>
        {adresse && (
          <p className="page-fiche-station__adresse">
            <MapPin size={18} aria-hidden="true" />
            <span>{adresse}</span>
          </p>
        )}
        {zone && <p className="page-fiche-station__zone">{zone}</p>}
        {/* L5-07 (Kalil) : <BoutonFavori idStation={station.id} /> quand le backend des favoris existera. */}
      </header>

      <div className="page-fiche-station__cartes">
        <section className="page-fiche-station__carte-bloc" aria-labelledby="titre-carburants">
          <TitreCarte id="titre-carburants" icone={Fuel}>
            Prix des carburants
          </TitreCarte>
          <TableauCarburants carburants={station.carburants ?? []} />
        </section>

        {disponibles.length > 0 && (
          <section
            className="page-fiche-station__carte-bloc page-fiche-station__carte-bloc--economie"
            aria-labelledby="titre-economie"
          >
            <TitreCarte id="titre-economie" icone={PiggyBank}>
              Calcul d'économie
            </TitreCarte>
            <CalculEconomie
              carburants={disponibles}
              carburant={carburant}
              litres={litres}
              onChangerCarburant={setCarburantChoisi}
              onChangerLitres={setLitres}
              resultat={economie.resultat}
              chargement={economie.chargement}
              erreur={economie.erreur}
              onReessayer={economie.relancer}
              ville={station.ville}
            />
          </section>
        )}

        {station.services?.length > 0 && (
          <section className="page-fiche-station__carte-bloc" aria-labelledby="titre-services">
            <h2 id="titre-services" className="page-fiche-station__titre-carte">
              Services
            </h2>
            <ul className="page-fiche-station__services">
              {station.services.map((service) => (
                <li key={service}>{service}</li>
              ))}
            </ul>
          </section>
        )}

        {positionValide(station.position) && (
          <section className="page-fiche-station__localisation" aria-labelledby="titre-carte">
            <h2 id="titre-carte" className="visuellement-cache">
              Localisation
            </h2>
            <CarteStations stations={[station]} compacte />
          </section>
        )}

        <section className="page-fiche-station__carte-bloc" aria-labelledby="titre-horaires">
          <TitreCarte id="titre-horaires" icone={Clock}>
            Horaires d'ouverture
          </TitreCarte>
          <Horaires horaires={station.horaires ?? []} />
          {station.automate24h && (
            <p className="page-fiche-station__automate">Automate carte bancaire disponible 24h/24.</p>
          )}
        </section>
      </div>
    </article>
  );
}

export default FicheStation;

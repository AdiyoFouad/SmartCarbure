import { Link, useParams } from 'react-router-dom';
import BadgeAutomate from '../components/BadgeAutomate';
import BadgeEmplacement from '../components/BadgeEmplacement';
import CarteStations, { positionValide } from '../components/CarteStations';
import Horaires from '../components/Horaires';
import MessageEtat, { TYPES_MESSAGE } from '../components/MessageEtat';
import TableauCarburants from '../components/TableauCarburants';
import { useRequete } from '../hooks/useRequete';
import { MESSAGES_ERREUR } from '../services/api';
import { obtenirStation } from '../services/stationService';
import './FicheStation.css';

/**
 * Titre de la fiche : le nom de la station s'il existe (l'API n'en fournit pas aujourd'hui),
 * sinon son adresse, sinon son identifiant. N'affiche jamais « null ».
 * @param {object} station
 * @returns {string}
 */
export function titreStation(station) {
  return station.nom || station.adresse || `Station ${station.id}`;
}

/** « 35000 Rennes », sans valeur vide ni « null ». */
function lieuStation(station) {
  return [station.codePostal, station.ville].filter(Boolean).join(' ');
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

/**
 * Fiche détaillée d'une station (/stations/:id) : adresse, horaires, services, prix,
 * date de mise à jour (F7) et ruptures (F8). Données : GET /api/stations/{id}.
 */
function FicheStation() {
  const { id } = useParams();
  const { donnees: station, chargement, erreur, relancer } = useRequete(() => obtenirStation(id), [id]);

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
        <h1 className="visuellement-cache">Fiche station</h1>
        <AffichageErreur erreur={erreur ?? { statut: 404 }} relancer={relancer} />
      </section>
    );
  }

  const lieu = lieuStation(station);
  const departement = station.departement?.nom;

  return (
    <article className="page-fiche-station">
      <header className="page-fiche-station__entete">
        <h1 className="page-fiche-station__titre">{titreStation(station)}</h1>
        {(lieu || departement) && (
          <p className="page-fiche-station__lieu">
            {lieu}
            {lieu && departement && ' · '}
            {departement}
          </p>
        )}
        <div className="page-fiche-station__badges">
          <BadgeEmplacement emplacement={station.emplacement} />
          {station.automate24h && <BadgeAutomate />}
        </div>
        {/* L5-07 (Kalil) : <BoutonFavori idStation={station.id} /> */}
      </header>

      <div className="page-fiche-station__grille">
        <section className="page-fiche-station__bloc" aria-labelledby="titre-carburants">
          <h2 id="titre-carburants">Carburants</h2>
          <TableauCarburants carburants={station.carburants ?? []} />
        </section>

        <section className="page-fiche-station__bloc" aria-labelledby="titre-horaires">
          <h2 id="titre-horaires">Horaires d'ouverture</h2>
          <Horaires horaires={station.horaires ?? []} />
        </section>

        {station.services?.length > 0 && (
          <section className="page-fiche-station__bloc" aria-labelledby="titre-services">
            <h2 id="titre-services">Services</h2>
            <ul className="page-fiche-station__services">
              {station.services.map((service) => (
                <li key={service}>{service}</li>
              ))}
            </ul>
          </section>
        )}

        {/* L5-03 (Kalil) : section « Économie sur un plein » avec CalculEconomie ;
            la page appelle economieService et passe le résultat au composant. */}

        {positionValide(station.position) && (
          <section className="page-fiche-station__bloc page-fiche-station__carte" aria-labelledby="titre-carte">
            <h2 id="titre-carte">Localisation</h2>
            <CarteStations stations={[station]} compacte />
          </section>
        )}
      </div>
    </article>
  );
}

export default FicheStation;

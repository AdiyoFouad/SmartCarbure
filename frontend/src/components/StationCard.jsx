import './StationCard.css';
import { Link } from 'react-router-dom';
import { Clock, MapPin } from 'lucide-react';
import { CARBURANT_PAR_DEFAUT, libelleCarburant } from '../constants/carburants';
import { formaterDateMaj, formaterDistance } from '../utils/format';
import BadgeAutomate from './BadgeAutomate';
import BadgeEmplacement from './BadgeEmplacement';
import BadgePrix from './BadgePrix';
import BadgeRupture from './BadgeRupture';
import BoutonFavori from './BoutonFavori';

/**
 * Résumé d'une station dans une liste, avec un lien vers sa fiche.
 * @param {object} props
 * @param {object} props.station station au format du contrat d'API
 * @param {string} [props.carburantRecherche] code du carburant à mettre en avant
 * @param {'bas'|'moyen'|'haut'} [props.niveauPrix] niveau de prix calculé par la liste
 * @param {boolean} [props.moinsCher] indique que c'est le résultat le moins cher
 * @param {boolean} [props.actif] indique que la station est sélectionnée sur la carte
 * @param {(id: string|null) => void} [props.onSurvol] synchronise le survol avec la carte
 * @param {boolean} [props.estFavori] état du favori, contrôlé par le parent
 * @param {(id: string) => void} [props.onBasculerFavori] action de bascule du favori
 */
function StationCard({
  station,
  carburantRecherche = CARBURANT_PAR_DEFAUT,
  niveauPrix = 'moyen',
  moinsCher = false,
  actif = false,
  onSurvol,
  estFavori = false,
  onBasculerFavori,
}) {
  const carburant = station.carburants?.find((item) => item.type === carburantRecherche);
  const enRupture = carburant != null && !carburant.disponible;
  const distance = formaterDistance(station.distanceKm);
  const miseAJour = formaterDateMaj(carburant?.dateMaj);

  return (
    <article
      className={`station-card${actif ? ' station-card--actif' : ''}${moinsCher ? ' station-card--moins-cher' : ''}${enRupture ? ' station-card--rupture' : ''}`}
      onMouseEnter={() => onSurvol?.(station.id)}
      onMouseLeave={() => onSurvol?.(null)}
    >
      <div className="station-card__layout">
        <Link
          to={`/stations/${encodeURIComponent(station.id)}`}
          className="station-card__lien"
          aria-label={`Voir la fiche de ${station.nom || 'la station-service'} à ${station.ville}`}
        >
          <div className="station-card__badges" aria-label="Informations sur la station">
            {moinsCher && <span className="station-card__badge station-card__badge--moins-cher">Le moins cher</span>}
            {station.automate24h && <BadgeAutomate />}
            <BadgeEmplacement emplacement={station.emplacement} />
            {enRupture && (
              <BadgeRupture typeRupture={carburant.typeRupture} debutRupture={carburant.debutRupture} />
            )}
          </div>
          <h2 className="station-card__nom">{station.nom || 'Station-service'}</h2>
          <p className="station-card__adresse">
            <MapPin size={16} aria-hidden="true" />
            <span>{station.adresse}, {station.codePostal} {station.ville}</span>
          </p>

          <div className="station-card__details">
            {distance && <span className="station-card__distance">{distance}</span>}
            {miseAJour && (
              <span className="station-card__mise-a-jour">
                <Clock size={14} aria-hidden="true" />
                {miseAJour}
              </span>
            )}
          </div>
        </Link>

        <aside className="station-card__colonne-prix" aria-label="Prix du carburant">
          {onBasculerFavori && (
            <BoutonFavori
              estFavori={estFavori}
              onClick={() => onBasculerFavori(station.id)}
              nomStation={station.nom || station.adresse}
            />
          )}
          <div className="station-card__prix">
          <span className="station-card__carburant">{libelleCarburant(carburantRecherche)} / litre</span>
          {carburant ? (
            <BadgePrix prix={carburant.prix} niveau={niveauPrix} barre={enRupture} />
          ) : (
            <span className="station-card__prix-absent">Non renseigné</span>
          )}
          </div>
        </aside>
      </div>
    </article>
  );
}

export default StationCard;

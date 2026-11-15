import { LIBELLES_EMPLACEMENT, TYPES_EMPLACEMENT } from '../constants/emplacements';
import './BadgeEmplacement.css';

/**
 * Badge « Autoroute » pour une station d'autoroute. N'affiche rien pour une station de route
 * (le cas le plus courant) ou si l'emplacement est inconnu.
 * @param {object} props
 * @param {'ROUTE'|'AUTOROUTE'|null} [props.emplacement] emplacement de la station
 */
function BadgeEmplacement({ emplacement }) {
  if (emplacement !== TYPES_EMPLACEMENT.AUTOROUTE) {
    return null;
  }
  return <span className="badge-emplacement">{LIBELLES_EMPLACEMENT.AUTOROUTE}</span>;
}

export default BadgeEmplacement;

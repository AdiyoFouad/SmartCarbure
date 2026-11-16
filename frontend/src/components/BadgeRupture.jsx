import { LIBELLES_RUPTURE, TYPES_RUPTURE } from '../constants/ruptures';
import { formaterDate } from '../utils/format';
import './BadgeRupture.css';

/**
 * Badge « Rupture » (temporaire ou définitive) d'un carburant (F8) :
 * « Rupture temporaire depuis le 26/10/2026 ». Sans type connu : « En rupture ».
 * @param {object} props
 * @param {'TEMPORAIRE'|'DEFINITIVE'|null} [props.typeRupture] type de rupture
 * @param {string|null} [props.debutRupture] date ISO 8601 du début de la rupture
 */
function BadgeRupture({ typeRupture, debutRupture }) {
  const libelle = LIBELLES_RUPTURE[typeRupture] ?? 'En rupture';
  const depuis = formaterDate(debutRupture);
  const modificateur = typeRupture === TYPES_RUPTURE.DEFINITIVE ? 'definitive' : 'temporaire';

  return (
    <span className={`badge-rupture badge-rupture--${modificateur}`}>
      {libelle}
      {depuis && ` depuis le ${depuis}`}
    </span>
  );
}

export default BadgeRupture;

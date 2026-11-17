import './BadgePrix.css';
import { formaterPrix } from '../utils/format';

/**
 * Prix coloré selon sa position par rapport aux autres résultats.
 * @param {object} props
 * @param {number|null} props.prix prix en euros par litre
 * @param {'bas'|'moyen'|'haut'} [props.niveau] niveau du prix
 * @param {boolean} [props.barre] indique que le prix concerne un carburant en rupture
 */
function BadgePrix({ prix, niveau = 'moyen', barre = false }) {
  const classeNiveau = ['bas', 'moyen', 'haut'].includes(niveau) ? niveau : 'moyen';

  return (
    <span
      className={`badge-prix badge-prix--${classeNiveau}${barre ? ' badge-prix--barre' : ''}`}
      aria-label={prix == null ? 'Prix indisponible' : `${formaterPrix(prix)} par litre`}
    >
      {formaterPrix(prix)}
    </span>
  );
}

export default BadgePrix;

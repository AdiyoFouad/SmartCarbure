import './BoutonFavori.css';
import { Star } from 'lucide-react';

/**
 * Bouton contrôlé d'ajout ou de retrait d'une station des favoris.
 * @param {object} props
 * @param {boolean} [props.estFavori] état du favori
 * @param {() => void} props.onClick action fournie par le parent
 * @param {string} props.nomStation nom à annoncer aux technologies d'assistance
 */
function BoutonFavori({ estFavori = false, onClick, nomStation }) {
  return (
    <button
      type="button"
      className={`bouton-favori${estFavori ? ' bouton-favori--actif' : ''}`}
      aria-label={`${estFavori ? 'Retirer' : 'Ajouter'} ${nomStation} ${estFavori ? 'des' : 'aux'} favoris`}
      aria-pressed={estFavori}
      onClick={(event) => {
        event.preventDefault();
        event.stopPropagation();
        onClick();
      }}
    >
      <Star size={19} aria-hidden="true" />
    </button>
  );
}

export default BoutonFavori;

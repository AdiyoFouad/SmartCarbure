import { Download } from 'lucide-react';
import { genererCsv, nomFichierCsv, telechargerCsv } from '../utils/csv';
import './BoutonExportCsv.css';

/**
 * Bouton « Exporter en CSV » des résultats affichés (F18).
 * Le fichier est généré dans le navigateur, au format du contrat d'API (utils/csv.js) :
 * aucun appel au backend. Le bouton est désactivé tant qu'il n'y a aucune station à exporter.
 *
 * @param {object} props
 * @param {Array<object>} props.stations stations affichées, dans l'ordre de la liste
 * @param {string} props.carburant code du carburant recherché (GAZOLE, SP98…)
 * @param {string} [props.className] classe CSS ajoutée par la page (ex. « resultats__export »)
 */
function BoutonExportCsv({ stations, carburant, className = '' }) {
  const liste = stations ?? [];

  function exporter() {
    telechargerCsv(genererCsv(liste, carburant), nomFichierCsv());
  }

  return (
    <button
      type="button"
      className={`bouton-export-csv ${className}`.trim()}
      onClick={exporter}
      disabled={liste.length === 0}
    >
      <Download size={17} aria-hidden="true" /> Exporter en CSV
    </button>
  );
}

export default BoutonExportCsv;

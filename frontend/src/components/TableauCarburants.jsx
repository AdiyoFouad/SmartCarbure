import { libelleCarburant } from '../constants/carburants';
import { formaterDateMaj, formaterPrix } from '../utils/format';
import BadgeRupture from './BadgeRupture';
import './TableauCarburants.css';

/**
 * Tableau des carburants d'une station : prix, date de mise à jour et disponibilité (F7, F8).
 * Un carburant par ligne, dans l'ordre reçu. Un carburant en rupture affiche un BadgeRupture ;
 * un prix inconnu affiche « Prix indisponible ».
 * @param {object} props
 * @param {Array<object>} [props.carburants=[]] carburants au format du contrat d'API
 * @param {Date} [props.maintenant] date de référence pour « mis à jour il y a … » (tests)
 */
function TableauCarburants({ carburants = [], maintenant }) {
  if (carburants.length === 0) {
    return <p className="tableau-carburants__vide">Aucun carburant renseigné pour cette station.</p>;
  }

  return (
    <table className="tableau-carburants">
      <caption className="visuellement-cache">Prix et disponibilité des carburants</caption>
      <thead>
        <tr>
          <th scope="col">Carburant</th>
          <th scope="col">Prix au litre</th>
          <th scope="col">Mise à jour</th>
        </tr>
      </thead>
      <tbody>
        {carburants.map((carburant) => (
          <tr
            key={carburant.type}
            className={carburant.disponible ? undefined : 'tableau-carburants__ligne--rupture'}
          >
            <th scope="row">{libelleCarburant(carburant.type)}</th>
            <td>
              <span
                className={
                  carburant.prix == null ? 'tableau-carburants__prix--indisponible' : 'tableau-carburants__prix'
                }
              >
                {formaterPrix(carburant.prix)}
              </span>
              {!carburant.disponible && (
                <>
                  {' '}
                  <BadgeRupture typeRupture={carburant.typeRupture} debutRupture={carburant.debutRupture} />
                </>
              )}
            </td>
            <td className="tableau-carburants__maj">{formaterDateMaj(carburant.dateMaj, maintenant)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export default TableauCarburants;

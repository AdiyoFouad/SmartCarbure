import { libelleCarburant } from '../constants/carburants';
import { formaterDateMaj, formaterPrix } from '../utils/format';
import BadgeRupture from './BadgeRupture';
import './TableauCarburants.css';

/**
 * Prix affiché pour un carburant :
 * - disponible : prix en gros, avec la date de mise à jour en petit dessous (F7) ;
 * - en rupture avec un dernier prix connu : ce prix barré et grisé (ce n'est plus un prix actuel) ;
 * - en rupture sans prix (prix null) : rien ;
 * - disponible sans prix (cas anormal) : « Prix indisponible ».
 */
function CellulePrix({ carburant, maintenant }) {
  const prixConnu = typeof carburant.prix === 'number' && Number.isFinite(carburant.prix);

  if (!carburant.disponible) {
    return prixConnu ? (
      <del className="tableau-carburants__prix-barre">
        <span className="visuellement-cache">Dernier prix connu : </span>
        {formaterPrix(carburant.prix)}
      </del>
    ) : null;
  }

  if (!prixConnu) {
    return <span className="tableau-carburants__prix--indisponible">{formaterPrix(carburant.prix)}</span>;
  }

  const miseAJour = formaterDateMaj(carburant.dateMaj, maintenant);
  return (
    <>
      <span className="tableau-carburants__prix">{formaterPrix(carburant.prix)}</span>
      {miseAJour && <span className="tableau-carburants__maj">{miseAJour}</span>}
    </>
  );
}

/**
 * Tableau des carburants d'une station, en deux colonnes : carburant et prix au litre (F7, F8).
 * Un carburant par ligne, dans l'ordre reçu. Un carburant en rupture a une ligne grisée et un
 * BadgeRupture sous son nom ; son dernier prix connu, s'il existe, est barré.
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
          <th scope="col" className="tableau-carburants__colonne-prix">
            Prix au litre
          </th>
        </tr>
      </thead>
      <tbody>
        {carburants.map((carburant) => (
          <tr
            key={carburant.type}
            className={carburant.disponible ? undefined : 'tableau-carburants__ligne--rupture'}
          >
            <th scope="row">
              <span className="tableau-carburants__nom">{libelleCarburant(carburant.type)}</span>
              {!carburant.disponible && (
                <>
                  {/* Espace pour que le nom accessible soit « SP98 Rupture… » et non « SP98Rupture… ». */}{' '}
                  <BadgeRupture typeRupture={carburant.typeRupture} debutRupture={carburant.debutRupture} />
                </>
              )}
            </th>
            <td className="tableau-carburants__colonne-prix">
              <CellulePrix carburant={carburant} maintenant={maintenant} />
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export default TableauCarburants;

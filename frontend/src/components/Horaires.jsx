import './Horaires.css';

const NON_COMMUNIQUES = 'Horaires non communiqués';

/**
 * Garde « HH:mm » d'une heure : accepte aussi « 07:00:00 » (format actuel du backend).
 * @param {string|null|undefined} heure
 * @returns {string|null}
 */
function heureCourte(heure) {
  return typeof heure === 'string' && heure.length >= 5 ? heure.slice(0, 5) : null;
}

/**
 * Texte affiché pour un jour : « 07:00 – 22:00 », « Fermé » ou « Horaires non communiqués ».
 * @param {{ferme?: boolean, ouverture: string|null, fermeture: string|null}} horaire
 * @returns {string}
 */
export function texteHoraire(horaire) {
  if (horaire.ferme === true) {
    return 'Fermé';
  }
  const ouverture = heureCourte(horaire.ouverture);
  const fermeture = heureCourte(horaire.fermeture);
  if (!ouverture || !fermeture) {
    return NON_COMMUNIQUES;
  }
  return `${ouverture} – ${fermeture}`;
}

/**
 * Horaires d'ouverture d'une station, jour par jour (format du contrat d'API) :
 * ferme = true → « Fermé » ; ferme = false sans heures → « Horaires non communiqués » ;
 * liste vide → « Horaires non communiqués ».
 * @param {object} props
 * @param {Array<object>} [props.horaires=[]] horaires des sept jours
 */
function Horaires({ horaires = [] }) {
  if (horaires.length === 0) {
    return <p className="horaires horaires--inconnus">{NON_COMMUNIQUES}</p>;
  }

  return (
    <table className="horaires">
      <caption className="visuellement-cache">Horaires d'ouverture par jour</caption>
      <tbody>
        {horaires.map((horaire) => {
          const texte = texteHoraire(horaire);
          return (
            <tr key={horaire.jour}>
              <th scope="row" className="horaires__jour">
                {horaire.jour.charAt(0).toUpperCase() + horaire.jour.slice(1)}
              </th>
              <td className={texte === 'Fermé' ? 'horaires__ferme' : undefined}>{texte}</td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}

export default Horaires;

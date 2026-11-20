import { useId } from 'react';
import { libelleCarburant } from '../constants/carburants';
import { MESSAGES_ERREUR } from '../services/api';
import { formaterPrix } from '../utils/format';
import MessageEtat, { TYPES_MESSAGE } from './MessageEtat';
import './CalculEconomie.css';

/** Message affiché quand le volume saisi n'est pas utilisable. */
export const MESSAGE_SAISIE_LITRES = 'Saisissez un volume de plein supérieur à 0 litre.';

/**
 * Lit le volume saisi (« 50 », « 40,5 », « 40.5 »).
 * @param {string|number} litres valeur du champ
 * @returns {number|null} le volume, ou null s'il est vide, non numérique ou inférieur ou égal à 0
 */
export function lireLitres(litres) {
  const texte = String(litres ?? '').trim().replace(',', '.');
  if (texte === '') {
    return null;
  }
  const volume = Number(texte);
  return Number.isFinite(volume) && volume > 0 ? volume : null;
}

/**
 * Phrase principale du résultat : économie, surcoût ou même prix.
 * @param {number} montant montant du contrat (positif = économie, négatif = surcoût)
 * @returns {string}
 */
export function texteMontant(montant) {
  if (montant > 0) {
    return `Vous économisez ${formaterPrix(montant, 2)} sur ce plein`;
  }
  if (montant < 0) {
    return `Vous payez ${formaterPrix(Math.abs(montant), 2)} de plus sur ce plein`;
  }
  return 'Même prix que la moyenne';
}

/** Message d'erreur du calcul selon le code HTTP ; « Réessayer » seulement si un nouvel essai peut aboutir. */
function ErreurCalcul({ erreur, onReessayer }) {
  if (erreur.statut === 400) {
    return (
      <MessageEtat type={TYPES_MESSAGE.ERREUR} message="Calcul impossible pour ce carburant dans cette station." />
    );
  }
  if (erreur.statut === 404) {
    return <MessageEtat type={TYPES_MESSAGE.ERREUR} message="Station introuvable." />;
  }
  const message = erreur.statut === 503 ? MESSAGES_ERREUR[503] : MESSAGES_ERREUR.inconnu;
  return <MessageEtat type={TYPES_MESSAGE.ERREUR} message={message} onReessayer={onReessayer} />;
}

/**
 * Saisie du volume d'un plein et affichage de l'économie (ou du surcoût) par rapport au prix moyen
 * local (F15). Le calcul est fait par le backend : la page FicheStation appelle economieService
 * et transmet le résultat à ce composant, qui n'appelle pas le backend lui-même.
 *
 * @param {object} props
 * @param {Array<{type: string}>} props.carburants carburants disponibles de la station (liste déroulante)
 * @param {string} props.carburant code du carburant choisi
 * @param {string} props.litres volume saisi, tel qu'il est dans le champ
 * @param {(code: string) => void} props.onChangerCarburant appelée quand l'utilisateur change de carburant
 * @param {(litres: string) => void} props.onChangerLitres appelée à chaque saisie du volume
 * @param {{prixStation: number, prixMoyenLocal: number, litres: number, montant: number}|null} [props.resultat]
 *   réponse de GET /api/economie
 * @param {boolean} [props.chargement=false] calcul en cours
 * @param {{statut: number}|null} [props.erreur] ApiError du dernier calcul
 * @param {() => void} [props.onReessayer] relance le calcul (erreur 503 ou inattendue)
 * @param {string|null} [props.ville] ville de la station, pour la phrase du prix moyen
 */
function CalculEconomie({
  carburants,
  carburant,
  litres,
  onChangerCarburant,
  onChangerLitres,
  resultat = null,
  chargement = false,
  erreur = null,
  onReessayer,
  ville,
}) {
  const id = useId();
  const saisieValide = lireLitres(litres) !== null;

  let contenu = null;
  if (!saisieValide) {
    contenu = <p className="calcul-economie__saisie">{MESSAGE_SAISIE_LITRES}</p>;
  } else if (!chargement && !erreur && resultat) {
    const sens = resultat.montant > 0 ? 'economie' : resultat.montant < 0 ? 'surcout' : 'egal';
    contenu = (
      <>
        <p className={`calcul-economie__montant calcul-economie__montant--${sens}`}>
          {texteMontant(resultat.montant)}
        </p>
        <p className="calcul-economie__moyenne">
          {ville ? `Par rapport au prix moyen à ${ville}` : 'Par rapport au prix moyen local'} :{' '}
          {formaterPrix(resultat.prixMoyenLocal)} / litre
        </p>
      </>
    );
  }

  return (
    <div className="calcul-economie">
      <div className="calcul-economie__champs">
        <div className="calcul-economie__champ">
          <label htmlFor={`${id}-carburant`}>Carburant</label>
          <select
            id={`${id}-carburant`}
            value={carburant}
            onChange={(evenement) => onChangerCarburant(evenement.target.value)}
          >
            {carburants.map((c) => (
              <option key={c.type} value={c.type}>
                {libelleCarburant(c.type)}
              </option>
            ))}
          </select>
        </div>
        <div className="calcul-economie__champ">
          <label htmlFor={`${id}-litres`}>Plein de</label>
          <div className="calcul-economie__litres">
            <input
              id={`${id}-litres`}
              type="number"
              inputMode="decimal"
              min="1"
              step="any"
              value={litres}
              aria-describedby={`${id}-unite`}
              aria-invalid={!saisieValide}
              onChange={(evenement) => onChangerLitres(evenement.target.value)}
            />
            <span id={`${id}-unite`}>litres</span>
          </div>
        </div>
      </div>

      <div className="calcul-economie__resultat" aria-live="polite">
        {contenu}
      </div>

      {saisieValide && chargement && <MessageEtat type={TYPES_MESSAGE.CHARGEMENT} message="Calcul en cours…" />}
      {saisieValide && !chargement && erreur && <ErreurCalcul erreur={erreur} onReessayer={onReessayer} />}
    </div>
  );
}

export default CalculEconomie;

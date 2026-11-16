import './MessageEtat.css';

/** Types de message d'état. */
export const TYPES_MESSAGE = {
  CHARGEMENT: 'chargement',
  VIDE: 'vide',
  ERREUR: 'erreur',
};

/**
 * Message d'état (chargement, liste vide, erreur avec bouton « Réessayer »), annoncé avec role="status".
 *
 * Exemples :
 * - <MessageEtat type="chargement" message="Chargement des stations…" />
 * - <MessageEtat type="vide" message="Aucune station trouvée, élargissez le rayon." />
 * - <MessageEtat type="erreur" message={erreur.message} onReessayer={relancer} />
 *
 * @param {object} props
 * @param {'chargement'|'vide'|'erreur'} [props.type='chargement'] type de message (change le style)
 * @param {string} props.message texte affiché
 * @param {() => void} [props.onReessayer] si fourni, affiche un bouton « Réessayer » qui l'appelle
 * @param {React.ReactNode} [props.children] contenu en plus (par exemple un lien de retour)
 */
function MessageEtat({ type = TYPES_MESSAGE.CHARGEMENT, message, onReessayer, children }) {
  return (
    <div className={`message-etat message-etat--${type}`} role="status" aria-live="polite">
      {type === TYPES_MESSAGE.CHARGEMENT && <span className="message-etat__indicateur" aria-hidden="true" />}
      <p className="message-etat__texte">{message}</p>
      {onReessayer && (
        <button type="button" className="message-etat__bouton" onClick={onReessayer}>
          Réessayer
        </button>
      )}
      {children}
    </div>
  );
}

export default MessageEtat;

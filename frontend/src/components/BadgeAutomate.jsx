import './BadgeAutomate.css';

/**
 * Badge « Automate 24h/24 ». À afficher seulement si la station a un automate (automate24h = true).
 * @param {object} props
 * @param {boolean} [props.plein=false] fond vert plein et texte blanc (fiche station) ; sinon fond vert clair
 */
function BadgeAutomate({ plein = false }) {
  return <span className={`badge-automate${plein ? ' badge-automate--plein' : ''}`}>Automate 24h/24</span>;
}

export default BadgeAutomate;

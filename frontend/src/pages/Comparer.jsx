import './Comparer.css';
import { useState } from 'react';
import { ArrowRightLeft } from 'lucide-react';
import { CARBURANT_PAR_DEFAUT, CARBURANTS } from '../constants/carburants';
import { comparerZones } from '../services/comparaisonService';
import { formaterPrix } from '../utils/format';
import MessageEtat, { TYPES_MESSAGE } from '../components/MessageEtat';

/**
 * Comparaison des prix moyens entre deux villes ou deux départements (/comparer) (F16).
 */
function Comparer() {
  const [type, setType] = useState('DEPARTEMENT');
  const [zone1, setZone1] = useState('35');
  const [zone2, setZone2] = useState('44');
  const [carburant, setCarburant] = useState(CARBURANT_PAR_DEFAUT);
  const [resultat, setResultat] = useState(null);
  const [chargement, setChargement] = useState(false);
  const [erreur, setErreur] = useState('');

  async function chargerComparaison() {
    setChargement(true);
    setErreur('');
    try {
      setResultat(await comparerZones({ type, zone1, zone2, carburant }));
    } catch (cause) {
      setResultat(null);
      setErreur(cause.message || 'La comparaison est momentanément indisponible.');
    } finally {
      setChargement(false);
    }
  }

  function lancerComparaison(event) {
    event.preventDefault();
    chargerComparaison();
  }

  function changerType(nouveauType) {
    setType(nouveauType);
    setZone1(nouveauType === 'VILLE' ? 'Rennes' : '35');
    setZone2(nouveauType === 'VILLE' ? 'Nantes' : '44');
    setResultat(null);
    setErreur('');
  }

  function permuterZones() {
    setZone1(zone2);
    setZone2(zone1);
  }

  const moyenne = (zone, code = carburant) => {
    if (zone?.moyennes && Object.hasOwn(zone.moyennes, code)) {
      return zone.moyennes[code];
    }
    return code === carburant ? zone?.prixMoyen ?? null : null;
  };
  const prixSelectionne1 = moyenne(resultat?.zone1);
  const prixSelectionne2 = moyenne(resultat?.zone2);
  const ecart = prixSelectionne1 != null && prixSelectionne2 != null
    ? prixSelectionne2 - prixSelectionne1
    : null;
  const pourcentage = ecart == null || Math.max(prixSelectionne1, prixSelectionne2) === 0
    ? null
    : (Math.abs(ecart) / Math.max(prixSelectionne1, prixSelectionne2)) * 100;

  return (
    <section className="page-comparer">
      <header className="comparer__entete">
        <h1>Comparer deux zones</h1>
        <p>Prix moyens par carburant, côte à côte.</p>
      </header>

      <form className="comparer__formulaire" onSubmit={lancerComparaison}>
        <div className="comparer__modes" role="group" aria-label="Type de zone">
          {[
            ['VILLE', 'Ville'],
            ['DEPARTEMENT', 'Département'],
          ].map(([valeur, libelle]) => (
            <button
              key={valeur}
              type="button"
              className={type === valeur ? 'actif' : ''}
              aria-pressed={type === valeur}
              onClick={() => changerType(valeur)}
            >
              {libelle}
            </button>
          ))}
        </div>

        <div className="comparer__champs">
          <label className="comparer__champ">
            <span>Zone A</span>
            <input
              required
              value={zone1}
              onChange={(event) => setZone1(event.target.value)}
              placeholder={type === 'VILLE' ? 'Ex. Rennes' : 'Ex. 35'}
              aria-label="Zone A"
            />
          </label>
          <button
            className="comparer__permuter"
            type="button"
            onClick={permuterZones}
            aria-label="Permuter les zones"
            title="Permuter les zones"
          >
            <ArrowRightLeft size={18} aria-hidden="true" />
          </button>
          <label className="comparer__champ">
            <span>Zone B</span>
            <input
              required
              value={zone2}
              onChange={(event) => setZone2(event.target.value)}
              placeholder={type === 'VILLE' ? 'Ex. Nantes' : 'Ex. 44'}
              aria-label="Zone B"
            />
          </label>
          <label className="comparer__champ">
            <span>Carburant</span>
            <select value={carburant} onChange={(event) => setCarburant(event.target.value)}>
              {CARBURANTS.map((item) => (
                <option key={item.code} value={item.code}>{item.libelle}</option>
              ))}
            </select>
          </label>
          <button className="comparer__bouton" type="submit" disabled={chargement}>
            {chargement ? 'Comparaison…' : 'Comparer'}
          </button>
        </div>
      </form>

      <div className="comparer__resultats" aria-live="polite">
        {chargement && <MessageEtat message="Comparaison des prix en cours…" />}
        {!chargement && erreur && (
          <MessageEtat type={TYPES_MESSAGE.ERREUR} message={erreur} onReessayer={chargerComparaison} />
        )}
        {!chargement && resultat && (
          <>
            {ecart != null && (
              <section className="comparer__ecart" aria-label="Écart de prix moyen">
                <p>Écart sur le {CARBURANTS.find((item) => item.code === carburant)?.libelle}</p>
                <strong>
                  {Math.abs(ecart) < 0.0005
                    ? 'Aucun écart'
                    : `${formaterPrix(Math.abs(ecart))} / L`}
                  {pourcentage != null && Math.abs(ecart) >= 0.0005 && (
                    <span>({pourcentage.toFixed(1).replace('.', ',')} %)</span>
                  )}
                </strong>
                {Math.abs(ecart) >= 0.0005 && (
                  <p className="comparer__moins-cher">
                    {ecart > 0 ? resultat.zone1.nom : resultat.zone2.nom} est moins cher en moyenne
                  </p>
                )}
              </section>
            )}
            <div className="comparer__cartes">
              {[resultat.zone1, resultat.zone2].map((zone, index) => {
                const autre = index === 0 ? resultat.zone2 : resultat.zone1;
                const moinsChere = zone.prixMoyen != null && autre.prixMoyen != null
                  && zone.prixMoyen < autre.prixMoyen;
                const plusChere = zone.prixMoyen != null && autre.prixMoyen != null
                  && zone.prixMoyen > autre.prixMoyen;
                return (
                  <article className="comparer__carte" key={`${zone.nom}-${index}`}>
                    <h2>{zone.nom}</h2>
                    <p className="comparer__nombre-stations">
                      {zone.nombreStationsTotal ?? zone.nombreStations} {(zone.nombreStationsTotal ?? zone.nombreStations) > 1 ? 'stations' : 'station'}
                    </p>
                    <div className="comparer__liste-prix">
                      {CARBURANTS.map((item) => {
                        const prixZone = moyenne(zone, item.code);
                        const prixAutre = moyenne(autre, item.code);
                        const carburantMoinsCher = prixZone != null && prixAutre != null
                          && prixZone < prixAutre;
                        const carburantPlusCher = prixZone != null && prixAutre != null
                          && prixZone > prixAutre;
                        return (
                          <div
                            className={`comparer__prix${item.code === carburant ? ' selectionne' : ''}`}
                            key={item.code}
                            aria-current={item.code === carburant ? 'true' : undefined}
                          >
                            <span>{item.libelle === 'E10' ? 'SP95-E10' : item.libelle}</span>
                            <strong className={carburantMoinsCher ? 'prix-bas' : carburantPlusCher ? 'prix-haut' : ''}>
                              {prixZone == null ? 'Non proposé' : formaterPrix(prixZone)}
                            </strong>
                          </div>
                        );
                      })}
                    </div>
                    <div className="comparer__resume">
                      <p>Moyenne sur le {CARBURANTS.find((item) => item.code === carburant)?.libelle === 'E10' ? 'SP95-E10' : CARBURANTS.find((item) => item.code === carburant)?.libelle}</p>
                      <strong>{zone.nombreStationsParCarburant?.[carburant] ?? zone.nombreStations} prix disponibles</strong>
                    </div>
                  </article>
                );
              })}
            </div>
            {ecart == null && (
              <MessageEtat type={TYPES_MESSAGE.VIDE} message="Impossible de calculer l’écart : le prix moyen manque pour au moins une zone." />
            )}
          </>
        )}
      </div>
    </section>
  );
}

export default Comparer;

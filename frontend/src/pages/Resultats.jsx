import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Download, List, Map as MapIcon } from 'lucide-react';
import { CARBURANT_PAR_DEFAUT, CODES_CARBURANTS, libelleCarburant } from '../constants/carburants';
import FormulaireRecherche from '../components/FormulaireRecherche';
import StationCard from '../components/StationCard';
import CarteStations from '../components/CarteStations';
import MessageEtat from '../components/MessageEtat';
import { useRequete } from '../hooks/useRequete';
import { MESSAGES_ERREUR } from '../services/api';
import { rechercherStations } from '../services/stationService';
import './Resultats.css';

const CENTRES_VILLES = {
  rennes: { latitude: 48.1173, longitude: -1.6778, nom: 'Rennes', codePostal: '35000' },
  nantes: { latitude: 47.2184, longitude: -1.5536, nom: 'Nantes', codePostal: '44000' },
  'cesson-sevigne': { latitude: 48.121, longitude: -1.603, nom: 'Cesson-Sévigné', codePostal: '35510' },
};

function normaliser(texte) {
  return texte
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim()
    .toLocaleLowerCase('fr-FR');
}

function prixDe(station, carburant) {
  return station.carburants?.find((item) => item.type === carburant) ?? null;
}

function niveauDePrix(prix, prixDisponibles) {
  if (prixDisponibles.length < 2) return 'bas';
  const min = Math.min(...prixDisponibles);
  const max = Math.max(...prixDisponibles);
  if (max === min) return 'bas';
  const ratio = (prix - min) / (max - min);
  return ratio < 1 / 3 ? 'bas' : ratio < 2 / 3 ? 'moyen' : 'haut';
}

function Resultats() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [onglet, setOnglet] = useState('liste');
  const [actif, setActif] = useState(null);
  const [favoris, setFavoris] = useState(() => new Set());

  const mode = searchParams.get('mode') === 'geo' ? 'geo' : 'ville';
  const q = searchParams.get('q') ?? '';
  const carburant = CODES_CARBURANTS.includes(searchParams.get('carburant'))
    ? searchParams.get('carburant')
    : CARBURANT_PAR_DEFAUT;
  const rayon = Math.min(50, Math.max(1, Number(searchParams.get('rayon')) || 10));
  const tri = searchParams.get('tri') === 'distance' ? 'distance' : 'prix';
  const latitudeInitiale = searchParams.get('lat');
  const longitudeInitiale = searchParams.get('lon');
  const latitude = Number(latitudeInitiale);
  const longitude = Number(longitudeInitiale);
  const positionValide = Number.isFinite(latitude) && Number.isFinite(longitude);
  const centreVille = Object.values(CENTRES_VILLES).find(
    (ville) => normaliser(ville.nom) === normaliser(q) || ville.codePostal.startsWith(q.trim()),
  );
  const centre = mode === 'geo' && positionValide
    ? { latitude, longitude }
    : centreVille ? { latitude: centreVille.latitude, longitude: centreVille.longitude } : undefined;
  const lieu = mode === 'geo'
    ? 'votre position'
    : centreVille ? `${centreVille.nom} (${centreVille.codePostal})` : q;
  const criteres = mode === 'geo'
    ? { latitude: latitudeInitiale, longitude: longitudeInitiale, rayon, carburant, tri: tri.toUpperCase() }
    : { ville: q.trim(), rayon, carburant, tri: tri.toUpperCase() };
  const rechercheValide = mode === 'geo' ? positionValide : Boolean(q.trim());
  const appelRecherche = () => rechercheValide ? rechercherStations(criteres) : Promise.resolve([]);
  const { donnees, chargement, erreur, relancer } = useRequete(appelRecherche, [
    mode, q, latitudeInitiale, longitudeInitiale, carburant, rayon, tri,
  ]);
  const resultats = Array.isArray(donnees) ? donnees : [];

  const prixDisponibles = resultats
    .map((station) => prixDe(station, carburant))
    .filter((item) => item?.disponible && Number.isFinite(item.prix))
    .map((item) => item.prix);
  const tries = resultats;

  function exporterCSV() {
    const lignes = [
      ['Station', 'Adresse', 'Code postal', 'Ville', 'Distance (km)', 'Carburant', 'Prix (€/L)', 'Disponible'],
      ...tries.map((station) => {
        const item = prixDe(station, carburant);
        return [
          station.nom || 'Station-service',
          station.adresse,
          station.codePostal,
          station.ville,
          station.distanceKm == null ? '' : station.distanceKm.toFixed(1).replace('.', ','),
          libelleCarburant(carburant),
          item?.prix == null ? '' : item.prix.toFixed(3).replace('.', ','),
          item?.disponible ? 'Oui' : 'Rupture',
        ];
      }),
    ];
    const contenu = '\uFEFF' + lignes.map((ligne) => ligne.map((cellule) => `"${String(cellule).replace(/"/g, '""')}"`).join(';')).join('\n');
    const url = URL.createObjectURL(new Blob([contenu], { type: 'text/csv;charset=utf-8' }));
    const lien = document.createElement('a');
    lien.href = url;
    lien.download = `smartcarbure-${carburant.toLowerCase()}.csv`;
    lien.click();
    URL.revokeObjectURL(url);
  }

  const moinsCher = prixDisponibles.length > 0
    ? tries.find((station) => {
      const item = prixDe(station, carburant);
      return item?.disponible && item.prix === Math.min(...prixDisponibles);
    })?.id
    : null;

  function changerTri(nouveauTri) {
    const prochainsParametres = new URLSearchParams(searchParams);
    prochainsParametres.set('tri', nouveauTri);
    setSearchParams(prochainsParametres);
  }

  const pointsCarte = tries.map((station) => ({
    ...station,
    nom: station.nom || `Station-service, ${station.ville}`,
  }));

  let contenuListe;
  if (chargement) {
    contenuListe = <MessageEtat type="chargement" message="Chargement des stations…" />;
  } else if (mode === 'geo' && !positionValide) {
    contenuListe = <MessageEtat type="erreur" message="Position indisponible. Relancez la recherche avec la localisation activée." />;
  } else if (erreur) {
    contenuListe = (
      <MessageEtat
        type="erreur"
        message={erreur.message || MESSAGES_ERREUR.inconnu}
        onReessayer={relancer}
      />
    );
  } else if (mode === 'ville' && !q.trim()) {
    contenuListe = <MessageEtat type="vide" message="Saisissez une ville ou un code postal pour afficher les stations." />;
  } else if (tries.length === 0) {
    contenuListe = <MessageEtat type="vide" message={mode === 'geo'
      ? `Aucune station trouvée à moins de ${rayon} km de ${lieu}. Essayez un rayon plus large.`
      : `Aucune station pour « ${q} », vérifiez la saisie. Essayez Rennes, Nantes ou un code postal comme 35000.`} />;
  } else {
    contenuListe = (
      <div className="resultats__liste">
        {tries.map((station) => {
          const item = prixDe(station, carburant);
          return (
            <StationCard
              key={station.id}
              station={station}
              carburantRecherche={carburant}
              moinsCher={station.id === moinsCher}
              actif={actif === station.id}
              onSurvol={setActif}
              niveauPrix={item?.disponible ? niveauDePrix(item.prix, prixDisponibles) : 'haut'}
              estFavori={favoris.has(station.id)}
              onBasculerFavori={(id) => setFavoris((courants) => {
                const prochains = new Set(courants);
                if (prochains.has(id)) prochains.delete(id);
                else prochains.add(id);
                return prochains;
              })}
            />
          );
        })}
      </div>
    );
  }

  return (
    <section className="page-resultats" aria-labelledby="resultats-titre">
      <title>Résultats de recherche – SmartCarbure</title>
      <FormulaireRecherche
        key={searchParams.toString()}
        compact
        initial={{ mode, q, carburant, rayon, lat: latitudeInitiale, lon: longitudeInitiale }}
      />

      <div className="resultats__entete">
        <div>
          <h1 id="resultats-titre">
            {chargement ? 'Recherche en cours…' : erreur || (mode === 'geo' && !positionValide)
              ? 'Résultats de recherche'
              : `${tries.length} station${tries.length > 1 ? 's' : ''} trouvée${tries.length > 1 ? 's' : ''}`}
          </h1>
          {!chargement && !erreur && rechercheValide && (
            <p>{libelleCarburant(carburant)} {mode === 'geo' ? `à moins de ${rayon} km de ${lieu}` : `à ${lieu}`}</p>
          )}
        </div>

        <div className="resultats__actions">
          <div className="resultats__tri" role="group" aria-label="Trier les résultats">
            <button type="button" className={tri === 'prix' ? 'actif' : ''} onClick={() => changerTri('prix')}>
              Prix le plus bas
            </button>
            <button type="button" className={tri === 'distance' ? 'actif' : ''} onClick={() => changerTri('distance')}>
              Distance la plus proche
            </button>
          </div>
          <button type="button" className="resultats__export" onClick={exporterCSV} disabled={chargement || Boolean(erreur) || tries.length === 0}>
            <Download size={17} aria-hidden="true" /> Exporter en CSV
          </button>
        </div>
      </div>

      <div className="resultats__onglets" role="tablist" aria-label="Affichage des résultats">
        <button type="button" role="tab" aria-selected={onglet === 'liste'} className={onglet === 'liste' ? 'actif' : ''} onClick={() => setOnglet('liste')}>
          <List size={17} aria-hidden="true" /> Liste
        </button>
        <button type="button" role="tab" aria-selected={onglet === 'carte'} className={onglet === 'carte' ? 'actif' : ''} onClick={() => setOnglet('carte')}>
          <MapIcon size={17} aria-hidden="true" /> Carte
        </button>
      </div>

      <div className="resultats__contenu">
        <div className={`resultats__panneau-liste${onglet === 'carte' ? ' masque-mobile' : ''}`}>
          {contenuListe}
        </div>
        <div className={`resultats__panneau-carte${onglet === 'liste' ? ' masque-mobile' : ''}`}>
          <CarteStations
            stations={pointsCarte}
            carburant={carburant}
            centre={centre}
          />
        </div>
      </div>
    </section>
  );
}

export default Resultats;

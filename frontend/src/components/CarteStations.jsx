import { useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { MapContainer, Marker, Popup, TileLayer, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { libelleCarburant } from '../constants/carburants';
import './CarteStations.css';

/** Centre par défaut de la carte (Rennes), utilisé quand il n'y a aucune station. */
export const CENTRE_PAR_DEFAUT = { latitude: 48.1173, longitude: -1.6778 };

const ZOOM_PAR_DEFAUT = 12;
const ZOOM_UNE_STATION = 15;

/** Niveaux de prix, utilisés pour la couleur des marqueurs : vert, orange, rouge, gris. */
export const NIVEAUX_PRIX = {
  BAS: 'bas',
  MOYEN: 'moyen',
  ELEVE: 'eleve',
  INDISPONIBLE: 'indisponible',
};

/**
 * Indique si une position est exploitable (latitude et longitude numériques).
 * @param {{latitude: number, longitude: number} | null | undefined} position
 * @returns {boolean}
 */
export function positionValide(position) {
  return (
    position != null &&
    Number.isFinite(position.latitude) &&
    Number.isFinite(position.longitude)
  );
}

/**
 * Renvoie le prix d'un carburant dans une station, ou null s'il n'est pas vendu ou en rupture.
 * @param {object} station station au format du contrat d'API
 * @param {string} carburant code du carburant (ex. « GAZOLE »)
 * @returns {number | null}
 */
export function prixCarburant(station, carburant) {
  const c = station.carburants?.find((item) => item.type === carburant);
  return c && c.disponible && c.prix != null ? c.prix : null;
}

/**
 * Classe un prix par rapport aux autres prix affichés : le tiers le moins cher est « bas »,
 * le tiers du milieu « moyen », le tiers le plus cher « eleve ».
 * @param {number | null} prix prix à classer
 * @param {number[]} tousLesPrix prix de toutes les stations affichées
 * @returns {string} une valeur de NIVEAUX_PRIX
 */
export function niveauPrix(prix, tousLesPrix) {
  if (prix == null) {
    return NIVEAUX_PRIX.INDISPONIBLE;
  }
  const min = Math.min(...tousLesPrix);
  const max = Math.max(...tousLesPrix);
  if (max === min) {
    return NIVEAUX_PRIX.BAS;
  }
  const position = (prix - min) / (max - min);
  if (position < 1 / 3) {
    return NIVEAUX_PRIX.BAS;
  }
  if (position < 2 / 3) {
    return NIVEAUX_PRIX.MOYEN;
  }
  return NIVEAUX_PRIX.ELEVE;
}

/**
 * Formate un prix au litre avec trois décimales (1.748 devient « 1,748 € »).
 * À remplacer par formaterPrix de utils/format.js quand elle sera implémentée.
 * @param {number | null} prix
 * @returns {string}
 */
export function formaterPrixCarte(prix) {
  if (prix == null) {
    return 'Prix indisponible';
  }
  return `${prix.toLocaleString('fr-FR', { minimumFractionDigits: 3, maximumFractionDigits: 3 })} €`;
}

/** Pictogramme de pompe à essence affiché dans les marqueurs. */
const PICTO_POMPE =
  '<svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" ' +
  'stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 22h12"/>' +
  '<path d="M4 9h10"/><path d="M14 22V4a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v18"/>' +
  '<path d="M14 13h2a2 2 0 0 1 2 2v2a2 2 0 0 0 2 2a2 2 0 0 0 2-2V9.83a2 2 0 0 0-.59-1.42L18 5"/></svg>';

/**
 * Crée l'icône d'un marqueur : une épingle colorée selon le niveau de prix.
 * On utilise une divIcon (HTML + CSS) : elle remplace les images par défaut de Leaflet,
 * qui ne sont pas trouvées avec Vite.
 * @param {string} niveau une valeur de NIVEAUX_PRIX
 */
function iconeStation(niveau) {
  return L.divIcon({
    className: 'carte-stations__icone',
    html: `<span class="carte-stations__epingle carte-stations__epingle--${niveau}">${PICTO_POMPE}</span>`,
    iconSize: [34, 42],
    iconAnchor: [17, 42],
    popupAnchor: [0, -38],
  });
}

/**
 * Ajuste la vue pour que toutes les stations soient visibles.
 * Composant interne : il doit être placé dans un MapContainer.
 */
function AjusterVue({ positions, centre }) {
  const carte = useMap();

  useEffect(() => {
    if (positions.length === 0) {
      carte.setView([centre.latitude, centre.longitude], ZOOM_PAR_DEFAUT);
    } else if (positions.length === 1) {
      carte.setView(positions[0], ZOOM_UNE_STATION);
    } else {
      carte.fitBounds(positions, { padding: [40, 40] });
    }
  }, [carte, positions, centre]);

  return null;
}

/**
 * Contenu de la bulle d'une station : nom, prix et lien vers la fiche.
 * Avec un carburant choisi, affiche son prix en grand ; sinon, tous les carburants.
 */
function BulleStation({ station, carburant, niveau }) {
  const carburantChoisi = carburant
    ? station.carburants?.find((c) => c.type === carburant)
    : null;

  return (
    <div className="carte-stations__bulle">
      <p className="carte-stations__bulle-nom">{station.nom}</p>

      {carburantChoisi ? (
        <p className="carte-stations__bulle-prix">
          {carburantChoisi.disponible ? (
            <strong className={`carte-stations__prix carte-stations__prix--${niveau}`}>
              {formaterPrixCarte(carburantChoisi.prix)}
            </strong>
          ) : (
            <span className="carte-stations__rupture">En rupture</span>
          )}{' '}
          <span className="carte-stations__bulle-carburant">
            {libelleCarburant(carburantChoisi.type)}
          </span>
        </p>
      ) : (
        <ul className="carte-stations__bulle-liste">
          {(station.carburants ?? []).map((c) => (
            <li key={c.type}>
              <span>{libelleCarburant(c.type)}</span>
              {c.disponible ? (
                <strong>{formaterPrixCarte(c.prix)}</strong>
              ) : (
                <span className="carte-stations__rupture">En rupture</span>
              )}
            </li>
          ))}
        </ul>
      )}

      <Link className="carte-stations__bulle-lien" to={`/stations/${station.id}`}>
        Voir la fiche
      </Link>
    </div>
  );
}

/**
 * Carte Leaflet des stations (F9), sur fond OpenStreetMap.
 *
 * Affiche une épingle par station ayant une position valide, colorée selon son prix pour le
 * carburant recherché (vert : bas, orange : moyen, rouge : élevé, gris : rupture). Un clic ouvre
 * une bulle avec le nom, le prix et un lien vers la fiche. La vue s'ajuste aux stations affichées.
 *
 * Utilisation :
 * - page Résultats : <CarteStations stations={stations} carburant={carburant} />
 * - page Fiche station : <CarteStations stations={[station]} compacte />
 *
 * @param {object} props
 * @param {Array<object>} [props.stations=[]] stations au format du contrat d'API (modèle Station)
 * @param {string} [props.carburant] code du carburant recherché ; sans lui, toutes les épingles
 *   sont vertes et la bulle liste tous les carburants
 * @param {boolean} [props.compacte=false] carte plus petite (page Fiche station)
 * @param {{latitude: number, longitude: number}} [props.centre] centre quand il n'y a aucune station
 */
function CarteStations({ stations = [], carburant, compacte = false, centre = CENTRE_PAR_DEFAUT }) {
  const stationsAffichees = useMemo(
    () => stations.filter((station) => positionValide(station.position)),
    [stations]
  );

  const tousLesPrix = useMemo(
    () =>
      carburant
        ? stationsAffichees.map((s) => prixCarburant(s, carburant)).filter((p) => p != null)
        : [],
    [stationsAffichees, carburant]
  );

  const positions = useMemo(
    () => stationsAffichees.map((s) => [s.position.latitude, s.position.longitude]),
    [stationsAffichees]
  );

  return (
    <section
      className={`carte-stations${compacte ? ' carte-stations--compacte' : ''}`}
      aria-label="Carte des stations"
    >
      <MapContainer
        className="carte-stations__leaflet"
        center={[centre.latitude, centre.longitude]}
        zoom={ZOOM_PAR_DEFAUT}
        scrollWheelZoom
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">contributeurs OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {stationsAffichees.map((station) => {
          const niveau = carburant
            ? niveauPrix(prixCarburant(station, carburant), tousLesPrix)
            : NIVEAUX_PRIX.BAS;
          return (
            <Marker
              key={station.id}
              position={[station.position.latitude, station.position.longitude]}
              icon={iconeStation(niveau)}
              title={station.nom}
            >
              <Popup>
                <BulleStation station={station} carburant={carburant} niveau={niveau} />
              </Popup>
            </Marker>
          );
        })}

        <AjusterVue positions={positions} centre={centre} />
      </MapContainer>

      {stationsAffichees.length === 0 && (
        <p className="carte-stations__message">Aucune station à afficher sur la carte.</p>
      )}
    </section>
  );
}

export default CarteStations;

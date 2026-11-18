import { useEffect, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { MapContainer, Marker, Polyline, Popup, TileLayer, useMap, useMapEvents, ZoomControl } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { libelleCarburant } from '../constants/carburants';
import {
  formaterPrixCarte,
  niveauPrix,
  NIVEAUX_PRIX,
  positionValide,
  prixCarburant,
} from './CarteStations';
import './CarteStations.css';
import './CarteTrajet.css';

/** Vue de départ : l'ouest de la France, centrée entre Rennes et Nantes. */
const CENTRE_INITIAL = [47.7, -1.4];
const ZOOM_INITIAL = 7;

/** Arrondit une coordonnée à 5 décimales (environ 1 m), pour des paramètres d'URL lisibles. */
function arrondirCoordonnee(valeur) {
  return Math.round(valeur * 100000) / 100000;
}

/** Convertit une position Leaflet ({ lat, lng }) en position du contrat ({ latitude, longitude }). */
export function versPosition(latLng) {
  return { latitude: arrondirCoordonnee(latLng.lat), longitude: arrondirCoordonnee(latLng.lng) };
}

/** Marqueur « Départ » (vert) ou « Arrivée » (orange), avec son étiquette. */
function iconeExtremite(type, libelle) {
  return L.divIcon({
    className: 'carte-trajet__icone',
    html: `<span class="carte-trajet__etiquette carte-trajet__etiquette--${type}">${libelle}</span>`
      + `<span class="carte-trajet__point carte-trajet__point--${type}"></span>`,
    iconSize: [80, 44],
    iconAnchor: [40, 38],
  });
}

const ICONE_DEPART = iconeExtremite('depart', 'Départ');
const ICONE_ARRIVEE = iconeExtremite('arrivee', 'Arrivée');

/** Icône d'une station : même épingle colorée que sur la carte des résultats. */
function iconeStation(niveau) {
  return L.divIcon({
    className: 'carte-stations__icone',
    html: `<span class="carte-stations__epingle carte-stations__epingle--${niveau}"></span>`,
    iconSize: [34, 42],
    iconAnchor: [17, 42],
    popupAnchor: [0, -38],
  });
}

/** Un clic sur la carte place le départ, puis l'arrivée. Ensuite, on déplace les marqueurs. */
function ClicSurLaCarte({ depart, arrivee, onChangerDepart, onChangerArrivee }) {
  useMapEvents({
    click(evenement) {
      const position = versPosition(evenement.latlng);
      if (!depart) {
        onChangerDepart(position);
      } else if (!arrivee) {
        onChangerArrivee(position);
      }
    },
  });
  return null;
}

/** Ajuste la vue sur l'itinéraire quand un nouveau trajet est affiché. */
function AjusterSurLeTrajet({ points }) {
  const carte = useMap();
  useEffect(() => {
    if (points.length > 1) {
      carte.fitBounds(points, { padding: [40, 40] });
    }
  }, [carte, points]);
  return null;
}

/** Marqueur déplaçable de départ ou d'arrivée. */
function MarqueurExtremite({ position, icone, titre, onDeplacer }) {
  const gestionnaires = useMemo(
    () => ({
      dragend(evenement) {
        onDeplacer(versPosition(evenement.target.getLatLng()));
      },
    }),
    [onDeplacer]
  );
  return (
    <Marker
      position={[position.latitude, position.longitude]}
      icon={icone}
      title={titre}
      draggable
      eventHandlers={gestionnaires}
    />
  );
}

/**
 * Carte du trajet (F6) : un clic place le départ puis l'arrivée, les deux marqueurs sont déplaçables,
 * puis la carte trace l'itinéraire et affiche les stations trouvées le long de la route.
 *
 * @param {object} props
 * @param {{latitude: number, longitude: number}|null} props.depart position de départ, ou null
 * @param {{latitude: number, longitude: number}|null} props.arrivee position d'arrivée, ou null
 * @param {(position: object) => void} props.onChangerDepart appelée quand le départ est placé ou déplacé
 * @param {(position: object) => void} props.onChangerArrivee appelée quand l'arrivée est placée ou déplacée
 * @param {{points: Array<{latitude: number, longitude: number}>}|null} [props.trajet] itinéraire à tracer
 * @param {Array<object>} [props.stations=[]] stations trouvées (modèle Station du contrat)
 * @param {string} [props.carburant] carburant recherché, pour la couleur des épingles et le prix affiché
 */
function CarteTrajet({ depart, arrivee, onChangerDepart, onChangerArrivee, trajet, stations = [], carburant }) {
  const points = useMemo(
    () => (trajet?.points ?? []).filter(positionValide).map((p) => [p.latitude, p.longitude]),
    [trajet]
  );
  const stationsAffichees = useMemo(() => stations.filter((s) => positionValide(s.position)), [stations]);
  const tousLesPrix = useMemo(
    () => stationsAffichees.map((s) => prixCarburant(s, carburant)).filter((p) => p != null),
    [stationsAffichees, carburant]
  );

  return (
    <section className="carte-trajet" aria-label="Carte du trajet">
      <MapContainer
        className="carte-trajet__leaflet"
        center={CENTRE_INITIAL}
        zoom={ZOOM_INITIAL}
        scrollWheelZoom
        zoomControl={false}
      >
        {/* Zoom en haut à droite : le panneau de recherche occupe le coin en haut à gauche. */}
        <ZoomControl position="topright" />
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">contributeurs OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        <ClicSurLaCarte
          depart={depart}
          arrivee={arrivee}
          onChangerDepart={onChangerDepart}
          onChangerArrivee={onChangerArrivee}
        />

        {points.length > 1 && (
          <Polyline positions={points} pathOptions={{ className: 'carte-trajet__trace', weight: 5 }} />
        )}

        {stationsAffichees.map((station) => {
          const prix = prixCarburant(station, carburant);
          const niveau = niveauPrix(prix, tousLesPrix);
          return (
            <Marker
              key={station.id}
              position={[station.position.latitude, station.position.longitude]}
              icon={iconeStation(niveau)}
              title={station.adresse || station.nom || `Station ${station.id}`}
            >
              <Popup>
                <div className="carte-stations__bulle">
                  <p className="carte-stations__bulle-nom">{station.adresse || station.nom}</p>
                  <p className="carte-stations__bulle-prix">
                    {niveau === NIVEAUX_PRIX.INDISPONIBLE ? (
                      <span className="carte-stations__rupture">En rupture</span>
                    ) : (
                      <strong className={`carte-stations__prix carte-stations__prix--${niveau}`}>
                        {formaterPrixCarte(prix)}
                      </strong>
                    )}{' '}
                    <span className="carte-stations__bulle-carburant">{libelleCarburant(carburant)}</span>
                  </p>
                  {station.distanceKm != null && (
                    <p>à {station.distanceKm.toLocaleString('fr-FR')} km de la route</p>
                  )}
                  <Link className="carte-stations__bulle-lien" to={`/stations/${station.id}`}>
                    Voir la fiche
                  </Link>
                </div>
              </Popup>
            </Marker>
          );
        })}

        {depart && (
          <MarqueurExtremite position={depart} icone={ICONE_DEPART} titre="Départ" onDeplacer={onChangerDepart} />
        )}
        {arrivee && (
          <MarqueurExtremite position={arrivee} icone={ICONE_ARRIVEE} titre="Arrivée" onDeplacer={onChangerArrivee} />
        )}

        <AjusterSurLeTrajet points={points} />
      </MapContainer>
    </section>
  );
}

export default CarteTrajet;

import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import CarteTrajet, { versPosition } from './CarteTrajet';

/*
 * Leaflet ne dessine pas de carte dans jsdom : react-leaflet est remplacé par de simples éléments HTML.
 * Le clic sur la carte et le déplacement d'un marqueur sont simulés par des boutons de test.
 */
const gestionnairesCarte = vi.hoisted(() => ({ courant: null }));

vi.mock('react-leaflet', () => ({
  MapContainer: ({ children }) => (
    <div data-testid="carte">
      <button type="button" onClick={() => gestionnairesCarte.courant.click({ latlng: { lat: 48.111111, lng: -1.666666 } })}>
        clic-carte
      </button>
      {children}
    </div>
  ),
  TileLayer: () => null,
  ZoomControl: () => null,
  Marker: ({ children, position, title, eventHandlers }) => (
    <div data-testid="marqueur" data-position={position.join(',')} title={title}>
      {eventHandlers && (
        <button
          type="button"
          onClick={() => eventHandlers.dragend({ target: { getLatLng: () => ({ lat: 47.5, lng: -1.5 }) } })}
        >
          deplacer-{title}
        </button>
      )}
      {children}
    </div>
  ),
  Popup: ({ children }) => <div data-testid="bulle">{children}</div>,
  Polyline: ({ positions }) => <div data-testid="trace" data-nombre-points={positions.length} />,
  useMap: () => ({ fitBounds: vi.fn(), setView: vi.fn() }),
  useMapEvents: (gestionnaires) => {
    gestionnairesCarte.courant = gestionnaires;
    return null;
  },
}));

const RENNES = { latitude: 48.1173, longitude: -1.6778 };
const NANTES = { latitude: 47.2184, longitude: -1.5536 };

function station(id, latitude, prix) {
  return {
    id,
    nom: null,
    adresse: `Adresse ${id}`,
    position: { latitude, longitude: -1.6 },
    carburants: [{ type: 'GAZOLE', prix, disponible: prix != null }],
    distanceKm: 1.2,
  };
}

let onChangerDepart;
let onChangerArrivee;

beforeEach(() => {
  onChangerDepart = vi.fn();
  onChangerArrivee = vi.fn();
});

function afficher(props) {
  return render(
    <MemoryRouter>
      <CarteTrajet onChangerDepart={onChangerDepart} onChangerArrivee={onChangerArrivee} {...props} />
    </MemoryRouter>
  );
}

describe('CarteTrajet', () => {
  test('le premier clic place le départ', () => {
    afficher({ depart: null, arrivee: null });
    fireEvent.click(screen.getByText('clic-carte'));
    expect(onChangerDepart).toHaveBeenCalledWith({ latitude: 48.11111, longitude: -1.66667 });
    expect(onChangerArrivee).not.toHaveBeenCalled();
  });

  test("le clic suivant place l'arrivée", () => {
    afficher({ depart: RENNES, arrivee: null });
    fireEvent.click(screen.getByText('clic-carte'));
    expect(onChangerArrivee).toHaveBeenCalled();
    expect(onChangerDepart).not.toHaveBeenCalled();
  });

  test('une fois les deux placés, un clic ne change plus rien', () => {
    afficher({ depart: RENNES, arrivee: NANTES });
    fireEvent.click(screen.getByText('clic-carte'));
    expect(onChangerDepart).not.toHaveBeenCalled();
    expect(onChangerArrivee).not.toHaveBeenCalled();
  });

  test('affiche les marqueurs Départ et Arrivée, déplaçables', () => {
    afficher({ depart: RENNES, arrivee: NANTES });
    expect(screen.getByTitle('Départ')).toHaveAttribute('data-position', '48.1173,-1.6778');
    expect(screen.getByTitle('Arrivée')).toHaveAttribute('data-position', '47.2184,-1.5536');

    fireEvent.click(screen.getByText('deplacer-Arrivée'));
    expect(onChangerArrivee).toHaveBeenCalledWith({ latitude: 47.5, longitude: -1.5 });
  });

  test("trace l'itinéraire et affiche les stations", () => {
    afficher({
      depart: RENNES,
      arrivee: NANTES,
      trajet: { points: [RENNES, { latitude: 47.8, longitude: -1.6 }, NANTES] },
      stations: [station('1', 47.9, 1.7), station('2', 47.6, 1.8)],
      carburant: 'GAZOLE',
    });
    expect(screen.getByTestId('trace')).toHaveAttribute('data-nombre-points', '3');
    expect(screen.getByTitle('Adresse 1')).toBeInTheDocument();
    expect(screen.getByTitle('Adresse 2')).toBeInTheDocument();
    expect(screen.getAllByText('Voir la fiche')[0]).toHaveAttribute('href', '/stations/1');
    expect(screen.getAllByText('à 1,2 km de la route')).toHaveLength(2);
  });

  test('signale une station en rupture', () => {
    afficher({ depart: RENNES, arrivee: NANTES, stations: [station('1', 47.9, null)], carburant: 'GAZOLE' });
    expect(screen.getByText('En rupture')).toBeInTheDocument();
  });

  test('ne trace rien sans trajet', () => {
    afficher({ depart: RENNES, arrivee: NANTES });
    expect(screen.queryByTestId('trace')).not.toBeInTheDocument();
  });

  test('versPosition arrondit à 5 décimales', () => {
    expect(versPosition({ lat: 48.1234567, lng: -1.7654321 })).toEqual({ latitude: 48.12346, longitude: -1.76543 });
  });
});

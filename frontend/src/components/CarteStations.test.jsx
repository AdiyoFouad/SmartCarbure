import { render, screen, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, test, vi } from 'vitest';
import CarteStations, {
  formaterPrixCarte,
  niveauPrix,
  NIVEAUX_PRIX,
  positionValide,
  prixCarburant,
} from './CarteStations';

/*
 * Leaflet ne peut pas dessiner une vraie carte dans jsdom : on remplace react-leaflet
 * par de simples éléments HTML pour tester ce que NOTRE composant affiche.
 * Le marqueur expose le niveau de prix lu dans le HTML de son icône.
 */
vi.mock('react-leaflet', () => ({
  MapContainer: ({ children }) => <div data-testid="conteneur-carte">{children}</div>,
  TileLayer: () => null,
  Marker: ({ children, position, title, icon }) => {
    const niveau = icon.options.html.match(/epingle--(\w+)/)[1];
    return (
      <div data-testid="marqueur" data-position={position.join(',')} data-niveau={niveau} title={title}>
        {children}
      </div>
    );
  },
  Popup: ({ children }) => <div data-testid="bulle">{children}</div>,
  useMap: () => ({ setView: vi.fn(), fitBounds: vi.fn() }),
}));

/** Crée une station au format du contrat d'API. */
function station(id, nom, latitude, longitude, carburants) {
  return {
    id,
    nom,
    adresse: '1 rue de test',
    ville: 'Rennes',
    codePostal: '35000',
    departement: { code: '35', nom: 'Ille-et-Vilaine' },
    region: { code: '53', nom: 'Bretagne' },
    position: latitude == null ? null : { latitude, longitude },
    emplacement: 'ROUTE',
    automate24h: true,
    horaires: [],
    services: [],
    carburants,
    distanceKm: 2.5,
  };
}

function gazole(prix, disponible = true) {
  return {
    type: 'GAZOLE',
    prix,
    dateMaj: '2026-11-12T10:00:00Z',
    disponible,
    typeRupture: disponible ? null : 'TEMPORAIRE',
    debutRupture: disponible ? null : '2026-11-11T08:00:00Z',
  };
}

const LECLERC = station('35000001', 'Leclerc Saint-Grégoire', 48.15, -1.68, [
  gazole(1.621),
  { type: 'SP95', prix: 1.789, dateMaj: '2026-11-12T10:00:00Z', disponible: true, typeRupture: null, debutRupture: null },
]);
const AVIA = station('35000002', 'Avia Saint-Grégoire', 48.13, -1.69, [gazole(1.701)]);
const TOTAL = station('35000003', 'Total Energies A84 Cesson', 48.12, -1.6, [gazole(1.838)]);
const EN_RUPTURE = station('35000004', 'Esso en rupture', 48.1, -1.65, [gazole(1.7, false)]);
const SANS_POSITION = station('35000099', 'Station sans coordonnées', null, null, [gazole(1.6)]);

function afficherCarte(props) {
  return render(
    <MemoryRouter>
      <CarteStations {...props} />
    </MemoryRouter>
  );
}

function marqueurDe(nom) {
  return screen.getAllByTestId('marqueur').find((m) => m.getAttribute('title') === nom);
}

describe('Composant CarteStations', () => {
  test('affiche un marqueur par station, à ses coordonnées', () => {
    afficherCarte({ stations: [LECLERC, AVIA, TOTAL], carburant: 'GAZOLE' });
    expect(screen.getAllByTestId('marqueur')).toHaveLength(3);
    expect(marqueurDe('Leclerc Saint-Grégoire')).toHaveAttribute('data-position', '48.15,-1.68');
  });

  test('ignore les stations sans position', () => {
    afficherCarte({ stations: [LECLERC, SANS_POSITION], carburant: 'GAZOLE' });
    expect(screen.getAllByTestId('marqueur')).toHaveLength(1);
    expect(screen.queryByText('Station sans coordonnées')).not.toBeInTheDocument();
  });

  test('colore les marqueurs selon le prix : vert, orange, rouge', () => {
    afficherCarte({ stations: [LECLERC, AVIA, TOTAL], carburant: 'GAZOLE' });
    expect(marqueurDe('Leclerc Saint-Grégoire')).toHaveAttribute('data-niveau', 'bas');
    expect(marqueurDe('Avia Saint-Grégoire')).toHaveAttribute('data-niveau', 'moyen');
    expect(marqueurDe('Total Energies A84 Cesson')).toHaveAttribute('data-niveau', 'eleve');
  });

  test('met en gris une station en rupture sur le carburant recherché', () => {
    afficherCarte({ stations: [LECLERC, EN_RUPTURE], carburant: 'GAZOLE' });
    expect(marqueurDe('Esso en rupture')).toHaveAttribute('data-niveau', 'indisponible');
    const bulle = within(marqueurDe('Esso en rupture')).getByTestId('bulle');
    expect(within(bulle).getByText('En rupture')).toBeInTheDocument();
  });

  test('la bulle affiche le nom et le prix du carburant recherché', () => {
    afficherCarte({ stations: [LECLERC], carburant: 'GAZOLE' });
    const bulle = screen.getByTestId('bulle');
    expect(within(bulle).getByText('Leclerc Saint-Grégoire')).toBeInTheDocument();
    expect(within(bulle).getByText('1,621 €')).toBeInTheDocument();
    expect(within(bulle).getByText('Gazole')).toBeInTheDocument();
    expect(within(bulle).queryByText('1,789 €')).not.toBeInTheDocument();
  });

  test('sans carburant choisi (fiche station), la bulle liste tous les carburants', () => {
    afficherCarte({ stations: [LECLERC], compacte: true });
    const bulle = screen.getByTestId('bulle');
    expect(within(bulle).getByText('1,621 €')).toBeInTheDocument();
    expect(within(bulle).getByText('1,789 €')).toBeInTheDocument();
  });

  test('la bulle contient un lien vers la fiche de la station', () => {
    afficherCarte({ stations: [LECLERC], carburant: 'GAZOLE' });
    expect(screen.getByRole('link', { name: 'Voir la fiche' })).toHaveAttribute(
      'href',
      '/stations/35000001'
    );
  });

  test('affiche un message quand il n’y a aucune station', () => {
    afficherCarte({ stations: [] });
    expect(screen.queryByTestId('marqueur')).not.toBeInTheDocument();
    expect(screen.getByText('Aucune station à afficher sur la carte.')).toBeInTheDocument();
  });

  test('a un nom accessible', () => {
    afficherCarte({ stations: [LECLERC] });
    expect(screen.getByRole('region', { name: 'Carte des stations' })).toBeInTheDocument();
  });
});

describe('Fonctions utilitaires de CarteStations', () => {
  test('positionValide refuse une position absente ou incomplète', () => {
    expect(positionValide({ latitude: 48.1, longitude: -1.6 })).toBe(true);
    expect(positionValide(null)).toBe(false);
    expect(positionValide({ latitude: null, longitude: -1.6 })).toBe(false);
  });

  test('prixCarburant renvoie null si le carburant est absent ou en rupture', () => {
    expect(prixCarburant(LECLERC, 'GAZOLE')).toBe(1.621);
    expect(prixCarburant(LECLERC, 'E85')).toBeNull();
    expect(prixCarburant(EN_RUPTURE, 'GAZOLE')).toBeNull();
  });

  test('niveauPrix classe un prix par tiers', () => {
    const prix = [1.6, 1.7, 1.8];
    expect(niveauPrix(1.6, prix)).toBe(NIVEAUX_PRIX.BAS);
    expect(niveauPrix(1.7, prix)).toBe(NIVEAUX_PRIX.MOYEN);
    expect(niveauPrix(1.8, prix)).toBe(NIVEAUX_PRIX.ELEVE);
    expect(niveauPrix(1.7, [1.7])).toBe(NIVEAUX_PRIX.BAS);
    expect(niveauPrix(null, prix)).toBe(NIVEAUX_PRIX.INDISPONIBLE);
  });

  test('formaterPrixCarte affiche trois décimales avec une virgule', () => {
    expect(formaterPrixCarte(1.7)).toBe('1,700 €');
    expect(formaterPrixCarte(null)).toBe('Prix indisponible');
  });
});

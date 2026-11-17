import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';
import { AuthProvider } from '../context/AuthContext';
import FormulaireRecherche from '../components/FormulaireRecherche';
import { STATIONS } from '../data/stations';
import { rechercherStations } from '../services/stationService';
import Resultats from './Resultats';

vi.mock('../services/stationService', () => ({ rechercherStations: vi.fn() }));

vi.mock('../components/CarteStations', () => ({
  default: ({ stations = [] }) => <div data-testid="carte-stations">{stations.length} repères</div>,
}));

function afficherRecherche(path = '/') {
  return render(
    <AuthProvider>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/" element={<FormulaireRecherche />} />
          <Route path="/resultats" element={<Resultats />} />
        </Routes>
      </MemoryRouter>
    </AuthProvider>,
  );
}

function distanceEntre(a, b) {
  const radians = (degres) => (degres * Math.PI) / 180;
  const latitude = radians(b.latitude - a.latitude);
  const longitude = radians(b.longitude - a.longitude);
  const valeur = Math.sin(latitude / 2) ** 2 +
    Math.cos(radians(a.latitude)) * Math.cos(radians(b.latitude)) * Math.sin(longitude / 2) ** 2;
  return 6371 * 2 * Math.atan2(Math.sqrt(valeur), Math.sqrt(1 - valeur));
}

beforeEach(() => {
  vi.clearAllMocks();
  rechercherStations.mockImplementation(({ ville, latitude, longitude, rayon, carburant }) => {
    let stations = STATIONS;
    const position = latitude != null && longitude != null
      ? { latitude: Number(latitude), longitude: Number(longitude) }
      : null;

    if (ville) {
      stations = stations.filter((station) =>
        station.ville.toLocaleLowerCase('fr-FR') === ville.toLocaleLowerCase('fr-FR') ||
        station.codePostal.startsWith(ville),
      );
    }

    return Promise.resolve(stations
      .filter((station) => station.carburants.some((item) => item.type === carburant))
      .map((station) => ({
        ...station,
        distanceKm: position ? distanceEntre(position, station.position) : null,
      }))
      .filter((station) => !position || station.distanceKm <= rayon)
      .sort((a, b) => {
        const prixA = a.carburants.find((item) => item.type === carburant);
        const prixB = b.carburants.find((item) => item.type === carburant);
        if (!prixA.disponible) return prixB.disponible ? 1 : 0;
        if (!prixB.disponible) return -1;
        return prixA.prix - prixB.prix;
      }));
  });
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe('Page Resultats', () => {
  test('la recherche de l’accueil appelle le service et affiche Rennes', async () => {
    afficherRecherche();

    fireEvent.change(screen.getByLabelText('Où cherchez-vous ?'), { target: { value: 'Rennes' } });
    fireEvent.click(screen.getByRole('button', { name: /rechercher/i }));

    expect(await screen.findByRole('heading', { name: '3 stations trouvées' })).toBeInTheDocument();
    expect(screen.getByText('Gazole à Rennes (35000)')).toBeInTheDocument();
    expect(screen.getByText('12 boulevard de la Liberté, 35000 Rennes')).toBeInTheDocument();
    expect(screen.getByTestId('carte-stations')).toHaveTextContent('3 repères');
    expect(rechercherStations).toHaveBeenCalledWith({
      ville: 'Rennes', rayon: 10, carburant: 'GAZOLE', tri: 'PRIX',
    });
  });

  test('filtre par carburant et affiche les ruptures présentes dans les données locales', async () => {
    afficherRecherche('/resultats?q=Rennes&carburant=SP98&rayon=10');

    expect(await screen.findByRole('heading', { name: '2 stations trouvées' })).toBeInTheDocument();
    expect(screen.getByText('Rupture définitive depuis le 27/03/2026')).toBeInTheDocument();
  });

  test('conserve les coordonnées géolocalisées lors d’une nouvelle recherche', async () => {
    afficherRecherche('/resultats?mode=geo&lat=48.1173&lon=-1.6778&carburant=GAZOLE&rayon=10');

    expect(await screen.findByRole('heading', { name: '4 stations trouvées' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /rechercher/i }));

    expect(await screen.findByRole('heading', { name: '4 stations trouvées' })).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  test('recherche un code postal et affiche les stations correspondantes', async () => {
    afficherRecherche('/resultats?q=35510&carburant=GAZOLE&rayon=10');

    expect(await screen.findByRole('heading', { name: '1 station trouvée' })).toBeInTheDocument();
    expect(screen.getByText('Aire de Cesson-Sévigné, A84, 35510 Cesson-Sévigné')).toBeInTheDocument();
  });

  test('affiche un état vide pour une ville sans station', async () => {
    afficherRecherche('/resultats?q=Lyon&carburant=GAZOLE&rayon=10');

    expect(await screen.findByText(/Aucune station pour « Lyon »/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /exporter en csv/i })).toBeDisabled();
  });

  test('affiche une erreur et permet de relancer la recherche', async () => {
    rechercherStations.mockRejectedValueOnce(new Error('Service indisponible.'));
    afficherRecherche('/resultats?q=Rennes&carburant=GAZOLE&rayon=10');

    expect(await screen.findByRole('status')).toHaveTextContent('Service indisponible.');
    fireEvent.click(screen.getByRole('button', { name: 'Réessayer' }));

    expect(await screen.findByRole('heading', { name: '3 stations trouvées' })).toBeInTheDocument();
  });

  test('exporte une recherche par ville dont la distance est absente', async () => {
    const createObjectURL = vi.fn(() => 'blob:resultats');
    const revokeObjectURL = vi.fn();
    vi.stubGlobal('URL', { createObjectURL, revokeObjectURL });
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});
    afficherRecherche('/resultats?q=Rennes&carburant=GAZOLE&rayon=10');

    await screen.findByRole('heading', { name: '3 stations trouvées' });
    fireEvent.click(screen.getByRole('button', { name: /exporter en csv/i }));

    expect(createObjectURL).toHaveBeenCalledOnce();
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:resultats');
  });
});

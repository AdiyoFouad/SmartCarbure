import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { STATIONS } from '../data/stations';
import { ApiError } from '../services/api';
import { obtenirStation } from '../services/stationService';
import FicheStation from './FicheStation';

/*
 * Le service est simulé : la page reçoit les stations fictives de data/stations.js (celles du mode
 * démonstration) ou une ApiError. Leaflet ne peut pas dessiner de carte dans jsdom : CarteStations
 * est remplacée par un simple élément, en gardant sa fonction positionValide.
 */
vi.mock('../services/stationService', () => ({ obtenirStation: vi.fn() }));
vi.mock('../components/CarteStations', async (importOriginal) => ({
  ...(await importOriginal()),
  default: ({ stations, compacte }) => (
    <div data-testid="carte" data-compacte={String(compacte)}>
      {stations.length} station
    </div>
  ),
}));

function stationFictive(id) {
  return structuredClone(STATIONS.find((s) => s.id === id));
}

function afficherFiche(id) {
  return render(
    <MemoryRouter initialEntries={[`/stations/${id}`]}>
      <Routes>
        <Route path="/stations/:id" element={<FicheStation />} />
        <Route path="/" element={<h1>Accueil</h1>} />
      </Routes>
    </MemoryRouter>
  );
}

function texteDuJour(jour) {
  return within(screen.getByRole('rowheader', { name: jour }).closest('tr')).getByRole('cell').textContent;
}

beforeEach(() => {
  vi.mocked(obtenirStation).mockReset();
});

describe('Page FicheStation', () => {
  test('affiche un message de chargement puis la station demandée', async () => {
    vi.mocked(obtenirStation).mockResolvedValue(stationFictive('35000101'));
    afficherFiche('35000101');

    expect(screen.getByRole('status')).toHaveTextContent('Chargement de la station…');
    expect(await screen.findByRole('heading', { level: 1, name: '12 boulevard de la Liberté' })).toBeInTheDocument();
    expect(obtenirStation).toHaveBeenCalledWith('35000101');
  });

  test('affiche le lieu, les badges, les carburants, les horaires, les services et la carte', async () => {
    vi.mocked(obtenirStation).mockResolvedValue(stationFictive('35000101'));
    afficherFiche('35000101');
    await screen.findByRole('heading', { level: 1 });

    expect(screen.getByText('35000 Rennes · Ille-et-Vilaine')).toBeInTheDocument();
    expect(screen.getByText('Automate 24h/24')).toBeInTheDocument();
    expect(screen.queryByText('Autoroute')).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: 'Carburants' })).toBeInTheDocument();
    expect(screen.getByText('1,749 €')).toBeInTheDocument();
    expect(texteDuJour('Lundi')).toBe('07:00 – 21:00');
    expect(screen.getByRole('heading', { level: 2, name: 'Services' })).toBeInTheDocument();
    expect(screen.getByText('Station de gonflage')).toBeInTheDocument();
    expect(screen.getByTestId('carte')).toHaveAttribute('data-compacte', 'true');
  });

  test('n’affiche jamais « null » ni « undefined » (nom toujours null)', async () => {
    vi.mocked(obtenirStation).mockResolvedValue(stationFictive('35200102'));
    const { container } = afficherFiche('35200102');
    await screen.findByRole('heading', { level: 1 });

    expect(container.textContent).not.toMatch(/null|undefined/);
  });

  test('affiche les ruptures : badge, date de début et « Prix indisponible » (F8)', async () => {
    vi.mocked(obtenirStation).mockResolvedValue(stationFictive('35200102'));
    afficherFiche('35200102');
    await screen.findByRole('heading', { level: 1 });

    expect(screen.getByText('Rupture temporaire depuis le 12/11/2026')).toBeInTheDocument();
    expect(screen.getByText('Rupture définitive depuis le 27/03/2026')).toBeInTheDocument();
    expect(screen.getByText('Prix indisponible')).toBeInTheDocument();
    // E85 en rupture temporaire garde son dernier prix
    expect(screen.getByText('0,899 €')).toBeInTheDocument();
  });

  test('station d’autoroute aux horaires non communiqués', async () => {
    vi.mocked(obtenirStation).mockResolvedValue(stationFictive('35510103'));
    afficherFiche('35510103');
    await screen.findByRole('heading', { level: 1 });

    expect(screen.getByText('Autoroute')).toBeInTheDocument();
    expect(screen.getByText('Horaires non communiqués')).toBeInTheDocument();
  });

  test('jour fermé, jour non communiqué et pas de section Services si la liste est vide', async () => {
    vi.mocked(obtenirStation).mockResolvedValue(stationFictive('35700104'));
    afficherFiche('35700104');
    await screen.findByRole('heading', { level: 1 });

    expect(texteDuJour('Dimanche')).toBe('Fermé');
    expect(texteDuJour('Samedi')).toBe('Horaires non communiqués');
    expect(screen.queryByRole('heading', { name: 'Services' })).not.toBeInTheDocument();
    expect(screen.queryByText('Automate 24h/24')).not.toBeInTheDocument();
  });

  test('pas de carte pour une station sans coordonnées', async () => {
    const station = stationFictive('35000101');
    station.position = { latitude: null, longitude: null };
    vi.mocked(obtenirStation).mockResolvedValue(station);
    afficherFiche('35000101');
    await screen.findByRole('heading', { level: 1 });

    expect(screen.queryByTestId('carte')).not.toBeInTheDocument();
  });

  test('accepte le format actuel du backend (nom absent, heures « 07:00:00 », sans ferme)', async () => {
    const station = stationFictive('35000101');
    delete station.nom;
    delete station.distanceKm;
    station.horaires = station.horaires.map(({ jour }) => ({ jour, ouverture: '07:00:00', fermeture: '22:00:00' }));
    vi.mocked(obtenirStation).mockResolvedValue(station);
    const { container } = afficherFiche('35000101');
    await screen.findByRole('heading', { level: 1, name: '12 boulevard de la Liberté' });

    expect(texteDuJour('Lundi')).toBe('07:00 – 22:00');
    expect(container.textContent).not.toMatch(/null|undefined/);
  });

  test('404 : « Station introuvable. » avec un lien vers l’accueil, sans « Réessayer »', async () => {
    vi.mocked(obtenirStation).mockRejectedValue(new ApiError(404));
    afficherFiche('99999999');

    expect(await screen.findByText('Station introuvable.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: "Retour à l'accueil" })).toHaveAttribute('href', '/');
    expect(screen.queryByRole('button', { name: 'Réessayer' })).not.toBeInTheDocument();
  });

  test('503 : message du contrat, puis « Réessayer » affiche la station', async () => {
    vi.mocked(obtenirStation)
      .mockRejectedValueOnce(new ApiError(503))
      .mockResolvedValueOnce(stationFictive('35000101'));
    afficherFiche('35000101');

    expect(
      await screen.findByText('Service temporairement indisponible. Réessayez dans quelques instants.')
    ).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Réessayer' }));

    expect(await screen.findByRole('heading', { level: 1, name: '12 boulevard de la Liberté' })).toBeInTheDocument();
    expect(obtenirStation).toHaveBeenCalledTimes(2);
  });

  test('autre erreur : message générique avec « Réessayer »', async () => {
    vi.mocked(obtenirStation).mockRejectedValue(new ApiError(500));
    afficherFiche('35000101');

    expect(await screen.findByText('Une erreur inattendue est survenue. Réessayez plus tard.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Réessayer' })).toBeInTheDocument();
  });
});

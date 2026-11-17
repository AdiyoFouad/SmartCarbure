import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { describe, expect, test, vi } from 'vitest';
import { AuthProvider } from '../context/AuthContext';
import FormulaireRecherche from '../components/FormulaireRecherche';
import Resultats from './Resultats';

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

describe('Page Resultats', () => {
  test('la recherche de l’accueil ouvre les résultats locaux de Rennes', async () => {
    afficherRecherche();

    fireEvent.change(screen.getByLabelText('Où cherchez-vous ?'), { target: { value: 'Rennes' } });
    fireEvent.click(screen.getByRole('button', { name: /rechercher/i }));

    expect(await screen.findByRole('heading', { name: '4 stations trouvées' })).toBeInTheDocument();
    expect(screen.getByText('Gazole à moins de 10 km de Rennes (35000)')).toBeInTheDocument();
    expect(screen.getByText('12 boulevard de la Liberté, 35000 Rennes')).toBeInTheDocument();
    expect(screen.getByTestId('carte-stations')).toHaveTextContent('4 repères');
  });

  test('filtre par carburant et affiche les ruptures présentes dans les données locales', async () => {
    afficherRecherche('/resultats?q=Rennes&carburant=SP98&rayon=10');

    expect(await screen.findByRole('heading', { name: '3 stations trouvées' })).toBeInTheDocument();
    expect(screen.getByText('Rupture définitive depuis le 27/03/2026')).toBeInTheDocument();
  });

  test('conserve les coordonnées géolocalisées lors d’une nouvelle recherche', async () => {
    afficherRecherche('/resultats?mode=geo&lat=48.1173&lon=-1.6778&carburant=GAZOLE&rayon=10');

    expect(await screen.findByRole('heading', { name: '4 stations trouvées' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /rechercher/i }));

    expect(await screen.findByRole('heading', { name: '4 stations trouvées' })).toBeInTheDocument();
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  test('recherche un code postal de Cesson-Sévigné dans les données locales', async () => {
    afficherRecherche('/resultats?q=35510&carburant=GAZOLE&rayon=10');

    expect(await screen.findByRole('heading', { name: '4 stations trouvées' })).toBeInTheDocument();
    expect(screen.getByText('Aire de Cesson-Sévigné, A84, 35510 Cesson-Sévigné')).toBeInTheDocument();
  });

  test('signale les villes absentes des données fictives', async () => {
    afficherRecherche('/resultats?q=Lyon&carburant=GAZOLE&rayon=10');

    expect(await screen.findByText(/Les données de démonstration couvrent Rennes, Nantes et Cesson-Sévigné/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /exporter en csv/i })).toBeDisabled();
  });
});

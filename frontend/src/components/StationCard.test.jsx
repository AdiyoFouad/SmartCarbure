import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, test, vi } from 'vitest';
import { STATIONS } from '../data/stations';
import StationCard from './StationCard';

function afficherCarte(props = {}) {
  return render(
    <MemoryRouter>
      <StationCard station={STATIONS[1]} carburantRecherche="E85" {...props} />
    </MemoryRouter>,
  );
}

describe('StationCard', () => {
  test('affiche les informations de la station et le prix du carburant recherché', () => {
    afficherCarte({ moinsCher: true });

    expect(screen.getByRole('heading', { name: 'Station-service' })).toBeInTheDocument();
    expect(screen.getByText('2 avenue des Pays-Bas, 35200 Rennes')).toBeInTheDocument();
    expect(screen.getByText('E85 / litre')).toBeInTheDocument();
    expect(screen.getByText('0,899 €')).toBeInTheDocument();
    expect(screen.getByText('Le moins cher')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Voir la fiche de la station-service à Rennes' })).toHaveAttribute(
      'href',
      '/stations/35200102',
    );
  });

  test('signale une rupture et barre le prix', () => {
    afficherCarte({ carburantRecherche: 'SP98' });

    expect(screen.getByText('Rupture définitive depuis le 27/03/2026')).toBeInTheDocument();
    expect(screen.getByText('Prix indisponible')).toBeInTheDocument();
    expect(screen.getByRole('article')).toHaveClass('station-card--rupture');
  });

  test('transmet les actions de survol et de favori au parent', () => {
    const onSurvol = vi.fn();
    const onBasculerFavori = vi.fn();
    afficherCarte({ onSurvol, onBasculerFavori, estFavori: true });

    fireEvent.mouseEnter(screen.getByRole('article'));
    fireEvent.click(screen.getByRole('button', { name: 'Retirer 2 avenue des Pays-Bas des favoris' }));

    expect(onSurvol).toHaveBeenCalledWith('35200102');
    expect(onBasculerFavori).toHaveBeenCalledWith('35200102');
  });
});
import { fireEvent, render, screen, within } from '@testing-library/react';
import { beforeEach, expect, test, vi } from 'vitest';
import Comparer from './Comparer';
import { comparerZones } from '../services/comparaisonService';

vi.mock('../services/comparaisonService', () => ({
  comparerZones: vi.fn(),
}));

beforeEach(() => {
  comparerZones.mockResolvedValue({
    type: 'VILLE',
    carburant: 'GAZOLE',
    zone1: {
      nom: 'Rennes',
      prixMoyen: 1.7,
      nombreStations: 2,
      nombreStationsTotal: 3,
      moyennes: { GAZOLE: 1.7, SP95: 1.8, SP98: null, E10: 1.75, E85: 0.8, GPLC: null },
      nombreStationsParCarburant: { GAZOLE: 2, SP95: 1, SP98: 0, E10: 1, E85: 1, GPLC: 0 },
    },
    zone2: {
      nom: 'Nantes',
      prixMoyen: 1.75,
      nombreStations: 2,
      nombreStationsTotal: 3,
      moyennes: { GAZOLE: 1.75, SP95: 1.78, SP98: 1.9, E10: 1.76, E85: null, GPLC: 0.98 },
      nombreStationsParCarburant: { GAZOLE: 2, SP95: 2, SP98: 1, E10: 1, E85: 0, GPLC: 1 },
    },
    ecart: -0.05,
  });
});

test('met à jour les données comparées quand le carburant sélectionné change', async () => {
  render(<Comparer />);
  fireEvent.click(screen.getByRole('button', { name: 'Comparer' }));

  expect(await screen.findByText('Écart sur le Gazole')).toBeInTheDocument();

  fireEvent.change(screen.getByRole('combobox', { name: 'Carburant' }), {
    target: { value: 'SP95' },
  });

  const cartes = screen.getAllByRole('article');
  expect(within(cartes[0]).getByText('1,800 €')).toBeInTheDocument();
  expect(within(cartes[1]).getByText('1,780 €')).toBeInTheDocument();
  expect(screen.getByText('Écart sur le SP95')).toBeInTheDocument();
});

test('conserve un carburant non proposé comme indisponible', async () => {
  render(<Comparer />);
  fireEvent.click(screen.getByRole('button', { name: 'Comparer' }));
  await screen.findByText('Écart sur le Gazole');

  fireEvent.change(screen.getByRole('combobox', { name: 'Carburant' }), {
    target: { value: 'SP98' },
  });

  const cartes = screen.getAllByRole('article');
  const ligneSp98 = within(cartes[0]).getByText('SP98').parentElement;
  expect(within(ligneSp98).getByText('Non proposé')).toBeInTheDocument();
  expect(within(cartes[1]).getByText('1,900 €')).toBeInTheDocument();
});
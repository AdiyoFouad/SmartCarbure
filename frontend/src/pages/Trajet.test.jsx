import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { ApiError } from '../services/api';
import { rechercherTrajet } from '../services/trajetService';
import Trajet from './Trajet';

vi.mock('../services/trajetService', () => ({ rechercherTrajet: vi.fn() }));

/** La carte est remplacée par deux boutons qui placent le départ et l'arrivée. */
vi.mock('../components/CarteTrajet', () => ({
  default: ({ onChangerDepart, onChangerArrivee, trajet, stations = [] }) => (
    <div data-testid="carte-trajet" data-stations={stations.length} data-trace={trajet ? 'oui' : 'non'}>
      <button type="button" onClick={() => onChangerDepart({ latitude: 48.1173, longitude: -1.6778 })}>
        placer-depart
      </button>
      <button type="button" onClick={() => onChangerArrivee({ latitude: 48.8566, longitude: 2.3522 })}>
        placer-arrivee
      </button>
    </div>
  ),
}));

function station(id, prix, distanceKm) {
  return {
    id,
    nom: null,
    adresse: `Adresse ${id}`,
    ville: 'Rennes',
    position: { latitude: 48, longitude: -1 },
    carburants: [{ type: 'GAZOLE', prix, disponible: prix != null }],
    distanceKm,
  };
}

const REPONSE = {
  trajet: { depart: {}, arrivee: {}, points: [], distanceKm: 354.6 },
  stations: [station('1', 2.01, 4.3), station('2', 2.05, 0.8), station('3', null, 1.0)],
};

function afficher() {
  render(
    <MemoryRouter>
      <Trajet />
    </MemoryRouter>
  );
}

function placerDepartEtArrivee() {
  fireEvent.click(screen.getByText('placer-depart'));
  fireEvent.click(screen.getByText('placer-arrivee'));
}

const bouton = () => screen.getByRole('button', { name: 'Rechercher sur ce trajet' });

beforeEach(() => {
  vi.clearAllMocks();
});

describe('Page Trajet', () => {
  test("le bouton de recherche est désactivé tant que le départ et l'arrivée ne sont pas placés", () => {
    afficher();
    expect(screen.getByText('Cliquez sur la carte pour placer le départ.')).toBeInTheDocument();
    expect(bouton()).toBeDisabled();

    fireEvent.click(screen.getByText('placer-depart'));
    expect(screen.getByText("Cliquez sur la carte pour placer l'arrivée.")).toBeInTheDocument();
    expect(bouton()).toBeDisabled();

    fireEvent.click(screen.getByText('placer-arrivee'));
    expect(bouton()).toBeEnabled();
  });

  test('lance la recherche avec les positions et le carburant choisi, puis affiche le résultat', async () => {
    rechercherTrajet.mockResolvedValue(REPONSE);
    afficher();
    placerDepartEtArrivee();
    fireEvent.change(screen.getByLabelText('Carburant'), { target: { value: 'GAZOLE' } });
    fireEvent.click(bouton());

    expect(screen.getByText(/Calcul du trajet en cours/)).toBeInTheDocument();
    expect(await screen.findByText('354,6 km')).toBeInTheDocument();
    expect(rechercherTrajet).toHaveBeenCalledWith({
      depart: { latitude: 48.1173, longitude: -1.6778 },
      arrivee: { latitude: 48.8566, longitude: 2.3522 },
      carburant: 'GAZOLE',
    });
    expect(screen.getByText(/3 stations le long du trajet/)).toBeInTheDocument();
    expect(screen.getByTestId('carte-trajet')).toHaveAttribute('data-stations', '3');
    expect(screen.getByTestId('carte-trajet')).toHaveAttribute('data-trace', 'oui');
  });

  test('liste les stations disponibles les moins chères, avec un lien vers leur fiche', async () => {
    rechercherTrajet.mockResolvedValue(REPONSE);
    afficher();
    placerDepartEtArrivee();
    fireEvent.click(bouton());

    const liens = await screen.findAllByRole('link');
    expect(liens).toHaveLength(2);
    expect(liens[0]).toHaveAttribute('href', '/stations/1');
    expect(screen.getByText('2,010 €')).toBeInTheDocument();
    expect(screen.getByText('à 4,3 km de la route')).toBeInTheDocument();
  });

  test('passe le carburant choisi à la recherche', async () => {
    rechercherTrajet.mockResolvedValue({ ...REPONSE, stations: [] });
    afficher();
    placerDepartEtArrivee();
    fireEvent.change(screen.getByLabelText('Carburant'), { target: { value: 'SP95' } });
    fireEvent.click(bouton());

    expect(await screen.findByText(/Aucune station trouvée le long de ce trajet/)).toBeInTheDocument();
    expect(rechercherTrajet.mock.calls[0][0].carburant).toBe('SP95');
  });

  test('affiche le message du backend pour une erreur 400, sans bouton Réessayer', async () => {
    rechercherTrajet.mockRejectedValue(
      new ApiError(400, undefined, { statut: 400, message: 'Le trajet dépasse 500 km.' })
    );
    afficher();
    placerDepartEtArrivee();
    fireEvent.click(bouton());

    expect(await screen.findByText('Le trajet dépasse 500 km.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Réessayer' })).not.toBeInTheDocument();
  });

  test('propose de réessayer quand le service est indisponible', async () => {
    rechercherTrajet.mockRejectedValueOnce(new ApiError(503)).mockResolvedValueOnce(REPONSE);
    afficher();
    placerDepartEtArrivee();
    fireEvent.click(bouton());

    expect(await screen.findByText(/Service temporairement indisponible/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Réessayer' }));
    expect(await screen.findByText('354,6 km')).toBeInTheDocument();
  });

  test('déplacer un marqueur efface le résultat devenu faux', async () => {
    rechercherTrajet.mockResolvedValue(REPONSE);
    afficher();
    placerDepartEtArrivee();
    fireEvent.click(bouton());
    expect(await screen.findByText('354,6 km')).toBeInTheDocument();

    fireEvent.click(screen.getByText('placer-arrivee'));
    expect(screen.queryByText('354,6 km')).not.toBeInTheDocument();
    expect(screen.getByTestId('carte-trajet')).toHaveAttribute('data-trace', 'non');
  });

  test('le bouton ↺ efface le départ et l’arrivée', () => {
    afficher();
    placerDepartEtArrivee();
    fireEvent.click(screen.getByRole('button', { name: "Effacer le départ et l'arrivée" }));
    expect(bouton()).toBeDisabled();
    expect(screen.getByText('Cliquez sur la carte pour placer le départ.')).toBeInTheDocument();
  });
});

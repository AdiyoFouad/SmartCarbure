import { beforeEach, describe, expect, test, vi } from 'vitest';
import { requete } from './api';
import { DELAI_TRAJET_MS, formaterPosition, rechercherTrajet } from './trajetService';

vi.mock('./api', () => ({ requete: vi.fn() }));
vi.mock('./mock', () => ({ MODE_DEMO: false, reponseSimulee: (fabrique) => Promise.resolve().then(fabrique) }));

const RENNES = { latitude: 48.1173, longitude: -1.6778 };
const PARIS = { latitude: 48.8566, longitude: 2.3522 };

beforeEach(() => {
  vi.clearAllMocks();
});

describe('trajetService', () => {
  test('appelle GET /api/trajet avec les positions au format latitude,longitude', async () => {
    requete.mockResolvedValue({ trajet: {}, stations: [] });

    await rechercherTrajet({ depart: RENNES, arrivee: PARIS, carburant: 'GAZOLE' });

    expect(requete).toHaveBeenCalledWith('/trajet', {
      params: {
        depart: '48.1173,-1.6778',
        arrivee: '48.8566,2.3522',
        carburant: 'GAZOLE',
        rayon: undefined,
      },
      delai: DELAI_TRAJET_MS,
    });
  });

  test('attend 30 secondes au lieu de 10', () => {
    expect(DELAI_TRAJET_MS).toBe(30000);
  });

  test('transmet le rayon quand il est fourni', async () => {
    requete.mockResolvedValue({ trajet: {}, stations: [] });

    await rechercherTrajet({ depart: RENNES, arrivee: PARIS, carburant: 'SP95', rayon: 8 });

    expect(requete.mock.calls[0][1].params.rayon).toBe(8);
  });

  test('renvoie la réponse du backend telle quelle', async () => {
    const reponse = { trajet: { distanceKm: 354.6 }, stations: [{ id: '1' }] };
    requete.mockResolvedValue(reponse);

    await expect(rechercherTrajet({ depart: RENNES, arrivee: PARIS, carburant: 'GAZOLE' })).resolves.toBe(reponse);
  });

  test('formaterPosition écrit latitude puis longitude', () => {
    expect(formaterPosition(RENNES)).toBe('48.1173,-1.6778');
  });
});

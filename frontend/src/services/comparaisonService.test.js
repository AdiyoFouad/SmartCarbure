import { afterEach, describe, expect, test, vi } from 'vitest';

async function chargerService(modeDemo) {
  vi.stubEnv('VITE_USE_MOCK', modeDemo ? 'true' : 'false');
  vi.resetModules();
  const service = await import('./comparaisonService');
  const mock = await import('./mock');
  mock.reinitialiserPanneSimulee();
  return service;
}

function reponse(statut, corps) {
  return {
    ok: statut >= 200 && statut < 300,
    status: statut,
    text: async () => JSON.stringify(corps),
  };
}

afterEach(() => {
  vi.unstubAllEnvs();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', '/');
});

describe('comparerZones', () => {
  test('appelle le endpoint de comparaison avec les critères attendus', async () => {
    const resultat = {
      type: 'DEPARTEMENT',
      carburant: 'GAZOLE',
      zone1: { nom: 'Ille-et-Vilaine (35)', prixMoyen: 1.7, nombreStations: 3 },
      zone2: { nom: 'Loire-Atlantique (44)', prixMoyen: 1.75, nombreStations: 2 },
      ecart: -0.05,
    };
    const fetch = vi.fn().mockResolvedValue(reponse(200, resultat));
    vi.stubGlobal('fetch', fetch);
    const { comparerZones } = await chargerService(false);

    await expect(comparerZones({
      type: 'DEPARTEMENT', zone1: '35', zone2: '44', carburant: 'GAZOLE',
    })).resolves.toEqual(resultat);

    expect(fetch.mock.calls[0][0]).toBe('/api/comparer?type=DEPARTEMENT&zone1=35&zone2=44&carburant=GAZOLE');
  });

  test('calcule les moyennes des stations fictives par département', async () => {
    const { comparerZones } = await chargerService(true);

    await expect(comparerZones({
      type: 'DEPARTEMENT', zone1: '35', zone2: '44', carburant: 'GAZOLE',
    })).resolves.toMatchObject({
      zone1: { nom: 'Ille-et-Vilaine (35)', prixMoyen: 1.764, nombreStations: 4 },
      zone2: { nom: 'Loire-Atlantique (44)', prixMoyen: 1.759, nombreStations: 2 },
      ecart: 0.005,
    });

    const resultat = await comparerZones({
      type: 'DEPARTEMENT', zone1: '35', zone2: '44', carburant: 'GAZOLE',
    });
    expect(resultat.zone1.moyennes.SP95).toBe(1.909);
    expect(resultat.zone1.nombreStationsParCarburant.SP95).toBe(2);
    expect(resultat.zone1.nombreStationsTotal).toBe(4);
  });
});
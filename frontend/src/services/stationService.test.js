import { afterEach, describe, expect, test, vi } from 'vitest';

/** Charge stationService (et mock.js) avec la valeur voulue de VITE_USE_MOCK. */
async function chargerService(modeDemo) {
  vi.stubEnv('VITE_USE_MOCK', modeDemo ? 'true' : 'false');
  vi.resetModules();
  const service = await import('./stationService');
  const mock = await import('./mock');
  mock.reinitialiserPanneSimulee();
  return { ...service, ...mock };
}

/** Fausse réponse fetch minimale. */
function reponse(statut, corps) {
  return {
    ok: statut >= 200 && statut < 300,
    status: statut,
    text: async () => (corps === undefined ? '' : JSON.stringify(corps)),
  };
}

afterEach(() => {
  vi.unstubAllEnvs();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', '/');
});

describe('rechercherStations, mode réel', () => {
  test('appelle GET /api/stations avec les critères de ville', async () => {
    const stations = [{ id: '35000001', distanceKm: null }];
    const fetch = vi.fn().mockResolvedValue(reponse(200, stations));
    vi.stubGlobal('fetch', fetch);
    const { rechercherStations } = await chargerService(false);

    await expect(rechercherStations({
      ville: 'Rennes', rayon: 10, carburant: 'GAZOLE', tri: 'PRIX',
    })).resolves.toEqual(stations);

    expect(fetch.mock.calls[0][0]).toBe('/api/stations?ville=Rennes&rayon=10&carburant=GAZOLE&tri=PRIX');
    expect(fetch.mock.calls[0][1].method).toBe('GET');
  });

  test('transmet les coordonnées et le rayon en recherche géolocalisée', async () => {
    const fetch = vi.fn().mockResolvedValue(reponse(200, []));
    vi.stubGlobal('fetch', fetch);
    const { rechercherStations } = await chargerService(false);

    await rechercherStations({
      latitude: '48.1173', longitude: '-1.6778', rayon: 10, carburant: 'GAZOLE', tri: 'DISTANCE',
    });

    expect(fetch.mock.calls[0][0]).toBe('/api/stations?lat=48.1173&lon=-1.6778&rayon=10&carburant=GAZOLE&tri=DISTANCE');
  });

  test('propage une indisponibilité du backend sous forme ApiError 503', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(reponse(503, { message: 'Prix indisponibles.' })));
    const { rechercherStations } = await chargerService(false);

    await expect(rechercherStations({ ville: 'Rennes' })).rejects.toMatchObject({
      name: 'ApiError', statut: 503,
    });
  });
});

describe('rechercherStations, mode démonstration', () => {
  test('filtre les stations fictives par ville sans appeler le backend', async () => {
    const fetch = vi.fn();
    vi.stubGlobal('fetch', fetch);
    const { rechercherStations } = await chargerService(true);

    const stations = await rechercherStations({ ville: 'Rennes', carburant: 'GAZOLE' });

    expect(stations).toHaveLength(3);
    expect(stations.every((station) => station.ville === 'Rennes')).toBe(true);
    expect(fetch).not.toHaveBeenCalled();
  });
});

describe('obtenirStation, mode réel', () => {
  test('appelle GET /api/stations/{id} et renvoie la station', async () => {
    const fetch = vi.fn().mockResolvedValue(reponse(200, { id: '35000001', nom: null }));
    vi.stubGlobal('fetch', fetch);
    const { obtenirStation } = await chargerService(false);

    const station = await obtenirStation('35000001');

    expect(station).toEqual({ id: '35000001', nom: null });
    expect(fetch.mock.calls[0][0]).toBe('/api/stations/35000001');
    expect(fetch.mock.calls[0][1].method).toBe('GET');
  });

  test('station inconnue : ApiError 404', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(reponse(404, { statut: 404, message: 'Station introuvable : 1.' })));
    const { obtenirStation } = await chargerService(false);

    await expect(obtenirStation('1')).rejects.toMatchObject({ name: 'ApiError', statut: 404 });
  });

  test('panne réseau : ApiError 503', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));
    const { obtenirStation } = await chargerService(false);

    await expect(obtenirStation('1')).rejects.toMatchObject({ statut: 503 });
  });
});

describe('obtenirStation, mode démonstration', () => {
  test('renvoie une copie de la station fictive', async () => {
    const fetch = vi.fn();
    vi.stubGlobal('fetch', fetch);
    const { obtenirStation } = await chargerService(true);
    const { STATIONS } = await import('../data/stations');

    const station = await obtenirStation('35200102');

    expect(station).toEqual(STATIONS.find((s) => s.id === '35200102'));
    expect(station).not.toBe(STATIONS.find((s) => s.id === '35200102'));
    expect(fetch).not.toHaveBeenCalled();
  });

  test('id inconnu : ApiError 404 fictive', async () => {
    const { obtenirStation } = await chargerService(true);

    await expect(obtenirStation('99999999')).rejects.toMatchObject({ statut: 404 });
  });

  test('?simuler=503 dans l’URL : ApiError 503', async () => {
    window.history.replaceState({}, '', '/stations/35000101?simuler=503');
    const { obtenirStation } = await chargerService(true);

    await expect(obtenirStation('35000101')).rejects.toMatchObject({ statut: 503 });
  });
});

describe('panneSimulee', () => {
  test('dure 3 secondes à partir du premier échec, puis les appels réussissent', async () => {
    window.history.replaceState({}, '', '/?simuler=503');
    const { panneSimulee, DUREE_PANNE_SIMULEE_MS } = await chargerService(true);

    expect(panneSimulee(1000)).toBe(true);
    expect(panneSimulee(1000 + DUREE_PANNE_SIMULEE_MS - 1)).toBe(true);
    expect(panneSimulee(1000 + DUREE_PANNE_SIMULEE_MS)).toBe(false);
  });

  test('aucune panne sans le paramètre', async () => {
    const { panneSimulee } = await chargerService(true);

    expect(panneSimulee()).toBe(false);
  });
});

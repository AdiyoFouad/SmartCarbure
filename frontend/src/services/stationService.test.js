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

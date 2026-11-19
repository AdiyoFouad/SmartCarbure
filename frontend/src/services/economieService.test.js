import { afterEach, describe, expect, test, vi } from 'vitest';

/** Charge economieService (et mock.js) avec la valeur voulue de VITE_USE_MOCK. */
async function chargerService(modeDemo) {
  vi.stubEnv('VITE_USE_MOCK', modeDemo ? 'true' : 'false');
  vi.resetModules();
  const service = await import('./economieService');
  const mock = await import('./mock');
  mock.reinitialiserPanneSimulee();
  return service;
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

describe('calculerEconomie, mode réel', () => {
  test('appelle GET /api/economie avec la station, le carburant et les litres', async () => {
    const resultat = { prixStation: 1.749, prixMoyenLocal: 1.771, litres: 50.0, montant: 1.1 };
    const fetch = vi.fn().mockResolvedValue(reponse(200, resultat));
    vi.stubGlobal('fetch', fetch);
    const { calculerEconomie } = await chargerService(false);

    await expect(calculerEconomie({ idStation: '35000001', carburant: 'GAZOLE', litres: 50 }))
      .resolves.toEqual(resultat);

    expect(fetch.mock.calls[0][0]).toBe('/api/economie?idStation=35000001&carburant=GAZOLE&litres=50');
    expect(fetch.mock.calls[0][1].method).toBe('GET');
  });

  test.each([400, 404, 503])('propage une erreur %i sous forme d’ApiError', async (statut) => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(reponse(statut, { statut, message: 'Erreur.' })));
    const { calculerEconomie } = await chargerService(false);

    await expect(calculerEconomie({ idStation: '35000001', carburant: 'GAZOLE', litres: 50 }))
      .rejects.toMatchObject({ name: 'ApiError', statut });
  });

  test('panne réseau : ApiError 503', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));
    const { calculerEconomie } = await chargerService(false);

    await expect(calculerEconomie({ idStation: '35000001', carburant: 'GAZOLE', litres: 50 }))
      .rejects.toMatchObject({ statut: 503 });
  });
});

describe('calculerEconomie, mode démonstration (data/stations.js)', () => {
  // Gazole à Rennes : 1,749 (35000101), 1,689 (35200102), 1,719 (35700104) → moyenne 1,719.

  test('surcoût : station plus chère que la moyenne de sa ville', async () => {
    const fetch = vi.fn();
    vi.stubGlobal('fetch', fetch);
    const { calculerEconomie } = await chargerService(true);

    await expect(calculerEconomie({ idStation: '35000101', carburant: 'GAZOLE', litres: 50 }))
      .resolves.toEqual({ prixStation: 1.749, prixMoyenLocal: 1.719, litres: 50, montant: -1.5 });
    expect(fetch).not.toHaveBeenCalled();
  });

  test('économie : station moins chère que la moyenne de sa ville', async () => {
    const { calculerEconomie } = await chargerService(true);

    await expect(calculerEconomie({ idStation: '35200102', carburant: 'GAZOLE', litres: '50' }))
      .resolves.toEqual({ prixStation: 1.689, prixMoyenLocal: 1.719, litres: 50, montant: 1.5 });
  });

  test('seule station de sa ville : montant 0', async () => {
    const { calculerEconomie } = await chargerService(true);

    await expect(calculerEconomie({ idStation: '35510103', carburant: 'GAZOLE', litres: 50 }))
      .resolves.toMatchObject({ prixStation: 1.899, prixMoyenLocal: 1.899, montant: 0 });
  });

  test('volume décimal : montant arrondi au centime', async () => {
    // SP95-E10 à Rennes : 1,799, 1,759, 1,779 → moyenne 1,779 ; (1,779 − 1,799) × 40,5 = −0,81
    const { calculerEconomie } = await chargerService(true);

    await expect(calculerEconomie({ idStation: '35000101', carburant: 'E10', litres: 40.5 }))
      .resolves.toMatchObject({ prixMoyenLocal: 1.779, montant: -0.81 });
  });

  test('les carburants en rupture des autres stations sont exclus de la moyenne', async () => {
    // SP95 à Nantes : 1,859 (44000105) ; 44300106 est en rupture → moyenne 1,859
    const { calculerEconomie } = await chargerService(true);

    await expect(calculerEconomie({ idStation: '44000105', carburant: 'SP95', litres: 50 }))
      .resolves.toMatchObject({ prixMoyenLocal: 1.859, montant: 0 });
  });

  test('404 pour une station inconnue', async () => {
    const { calculerEconomie } = await chargerService(true);

    await expect(calculerEconomie({ idStation: '99999999', carburant: 'GAZOLE', litres: 50 }))
      .rejects.toMatchObject({ statut: 404 });
  });

  test('400 pour un carburant en rupture dans la station', async () => {
    const { calculerEconomie } = await chargerService(true);

    await expect(calculerEconomie({ idStation: '35200102', carburant: 'E85', litres: 50 }))
      .rejects.toMatchObject({ statut: 400 });
  });

  test.each([0, -5, 'abc', ''])('400 pour un volume invalide (%s)', async (litres) => {
    const { calculerEconomie } = await chargerService(true);

    await expect(calculerEconomie({ idStation: '35000101', carburant: 'GAZOLE', litres }))
      .rejects.toMatchObject({ statut: 400 });
  });

  test('?simuler=503 : panne simulée', async () => {
    window.history.replaceState({}, '', '/?simuler=503');
    const { calculerEconomie } = await chargerService(true);

    await expect(calculerEconomie({ idStation: '35000101', carburant: 'GAZOLE', litres: 50 }))
      .rejects.toMatchObject({ statut: 503 });
  });
});

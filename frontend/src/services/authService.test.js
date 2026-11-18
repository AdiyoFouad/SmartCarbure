import { afterEach, describe, expect, test, vi } from 'vitest';
import { ApiError } from './api';

async function chargerService(modeDemo) {
  vi.stubEnv('VITE_USE_MOCK', modeDemo ? 'true' : 'false');
  vi.resetModules();
  const service = await import('./authService');
  const mock = await import('./mock');
  mock.reinitialiserPanneSimulee();
  return { ...service, ...mock };
}

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
});

describe('authService - Mode réel', () => {
  test('connecter envoie POST /api/auth/connexion avec email et motDePasse', async () => {
    const fakeAuthResponse = {
      jeton: 'test-token-jwt',
      utilisateur: { id: 1, nom: 'Camille Martin', email: 'camille@exemple.fr' },
    };
    const fetch = vi.fn().mockResolvedValue(reponse(200, fakeAuthResponse));
    vi.stubGlobal('fetch', fetch);

    const { connecter } = await chargerService(false);
    const resultat = await connecter({ email: 'camille@exemple.fr', motDePasse: 'motdepasse' });

    expect(resultat).toEqual(fakeAuthResponse);
    expect(fetch.mock.calls[0][0]).toBe('/api/auth/connexion');
    expect(fetch.mock.calls[0][1].method).toBe('POST');
    expect(JSON.parse(fetch.mock.calls[0][1].body)).toEqual({
      email: 'camille@exemple.fr',
      motDePasse: 'motdepasse',
    });
  });

  test('inscrire envoie POST /api/auth/inscription avec nom, email et motDePasse', async () => {
    const fakeUser = {
      id: 2,
      nom: 'Nouveau',
      email: 'nouveau@exemple.fr',
      carburantPrefere: null,
      rayonPrefere: null,
    };
    const fetch = vi.fn().mockResolvedValue(reponse(201, fakeUser));
    vi.stubGlobal('fetch', fetch);

    const { inscrire } = await chargerService(false);
    const resultat = await inscrire({ nom: 'Nouveau', email: 'nouveau@exemple.fr', motDePasse: 'secret123' });

    expect(resultat).toEqual(fakeUser);
    expect(fetch.mock.calls[0][0]).toBe('/api/auth/inscription');
    expect(fetch.mock.calls[0][1].method).toBe('POST');
    expect(JSON.parse(fetch.mock.calls[0][1].body)).toEqual({
      nom: 'Nouveau',
      email: 'nouveau@exemple.fr',
      motDePasse: 'secret123',
    });
  });

  test('propage une ApiError 401 si le backend rejette les identifiants', async () => {
    const fetch = vi.fn().mockResolvedValue(reponse(401, { statut: 401, message: 'Email ou mot de passe incorrect' }));
    vi.stubGlobal('fetch', fetch);

    const { connecter } = await chargerService(false);
    await expect(connecter({ email: 'faux@exemple.fr', motDePasse: 'errone' })).rejects.toThrow(ApiError);
  });
});

describe('authService - Mode démo', () => {
  test('connecter valide le compte de test existant', async () => {
    const { connecter } = await chargerService(true);
    const resultat = await connecter({ email: 'demo@smartcarbure.fr', motDePasse: 'motdepasse' });

    expect(resultat.jeton).toBeDefined();
    expect(resultat.utilisateur.email).toBe('demo@smartcarbure.fr');
    expect(resultat.utilisateur.nom).toBe('Camille Martin');
  });

  test('connecter échoue avec une 401 si mauvais mot de passe en mode démo', async () => {
    const { connecter } = await chargerService(true);
    await expect(connecter({ email: 'demo@smartcarbure.fr', motDePasse: 'mauvais' })).rejects.toMatchObject({
      statut: 401,
      message: 'Email ou mot de passe incorrect',
    });
  });

  test('inscrire ajoute un compte en mémoire et refuse un email existant (409)', async () => {
    const { inscrire } = await chargerService(true);
    await expect(inscrire({ nom: 'Doublon', email: 'demo@smartcarbure.fr', motDePasse: 'motdepasse123' })).rejects.toMatchObject({
      statut: 409,
      message: 'Cet email est déjà utilisé',
    });
  });
});

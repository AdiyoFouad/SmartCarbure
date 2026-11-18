import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { AuthProvider, CLE_STOCKAGE_AUTH } from './AuthContext';
import { useAuth } from '../hooks/useAuth';
import * as authService from '../services/authService';
import * as api from '../services/api';

vi.mock('../services/authService', () => ({
  connecter: vi.fn(),
  inscrire: vi.fn(),
}));

vi.mock('../services/api', async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    definirJeton: vi.fn(),
    obtenirJeton: vi.fn(),
  };
});

function ComposantTest() {
  const { estConnecte, utilisateur, jeton, connecter, deconnecter, mettreAJourUtilisateur, inscrire } = useAuth();

  return (
    <div>
      <span data-testid="connecte">{estConnecte ? 'oui' : 'non'}</span>
      <span data-testid="utilisateur">{utilisateur?.nom || 'aucun'}</span>
      <span data-testid="jeton">{jeton || 'aucun'}</span>
      <button type="button" onClick={() => connecter('test@test.fr', 'secret123')}>
        Connecter
      </button>
      <button type="button" onClick={deconnecter}>
        Déconnecter
      </button>
      <button type="button" onClick={() => mettreAJourUtilisateur({ carburantPrefere: 'SP95' })}>
        Changer Carburant
      </button>
      <button type="button" onClick={() => inscrire('Nouveau', 'nouveau@test.fr', 'secret123')}>
        Inscrire
      </button>
    </div>
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  localStorage.clear();
});

describe('AuthContext et AuthProvider', () => {
  test('fournit un état initial non connecté quand le localStorage est vide', () => {
    render(
      <AuthProvider>
        <ComposantTest />
      </AuthProvider>
    );

    expect(screen.getByTestId('connecte')).toHaveTextContent('non');
    expect(screen.getByTestId('utilisateur')).toHaveTextContent('aucun');
    expect(screen.getByTestId('jeton')).toHaveTextContent('aucun');
  });

  test('restaure la session depuis le localStorage au chargement', () => {
    const sessionStockee = {
      jeton: 'jwt-stocke',
      utilisateur: { id: 10, nom: 'Restauré', email: 'restaure@exemple.fr' },
    };
    localStorage.setItem(CLE_STOCKAGE_AUTH, JSON.stringify(sessionStockee));

    render(
      <AuthProvider>
        <ComposantTest />
      </AuthProvider>
    );

    expect(screen.getByTestId('connecte')).toHaveTextContent('oui');
    expect(screen.getByTestId('utilisateur')).toHaveTextContent('Restauré');
    expect(screen.getByTestId('jeton')).toHaveTextContent('jwt-stocke');
    expect(api.definirJeton).toHaveBeenCalledWith('jwt-stocke');
  });

  test('connecter met à jour la session et enregistre dans le localStorage', async () => {
    const user = userEvent.setup();
    vi.mocked(authService.connecter).mockResolvedValueOnce({
      jeton: 'nouveau-jeton',
      utilisateur: { id: 1, nom: 'Camille', email: 'test@test.fr' },
    });

    render(
      <AuthProvider>
        <ComposantTest />
      </AuthProvider>
    );

    await user.click(screen.getByRole('button', { name: /connecter/i }));

    await waitFor(() => {
      expect(screen.getByTestId('connecte')).toHaveTextContent('oui');
    });

    expect(authService.connecter).toHaveBeenCalledWith({ email: 'test@test.fr', motDePasse: 'secret123' });
    expect(api.definirJeton).toHaveBeenCalledWith('nouveau-jeton');
    expect(screen.getByTestId('utilisateur')).toHaveTextContent('Camille');

    const stocke = JSON.parse(localStorage.getItem(CLE_STOCKAGE_AUTH));
    expect(stocke.jeton).toBe('nouveau-jeton');
    expect(stocke.utilisateur.nom).toBe('Camille');
  });

  test('deconnecter efface la session, le localStorage et le jeton api', async () => {
    const user = userEvent.setup();
    localStorage.setItem(
      CLE_STOCKAGE_AUTH,
      JSON.stringify({ jeton: 'jwt', utilisateur: { id: 1, nom: 'Camille' } })
    );

    render(
      <AuthProvider>
        <ComposantTest />
      </AuthProvider>
    );

    expect(screen.getByTestId('connecte')).toHaveTextContent('oui');

    await user.click(screen.getByRole('button', { name: /déconnecter/i }));

    await waitFor(() => {
      expect(screen.getByTestId('connecte')).toHaveTextContent('non');
    });

    expect(api.definirJeton).toHaveBeenCalledWith(null);
    expect(screen.getByTestId('utilisateur')).toHaveTextContent('aucun');
    expect(localStorage.getItem(CLE_STOCKAGE_AUTH)).toBeNull();
  });

  test('mettreAJourUtilisateur modifie les données utilisateur en mémoire et dans le stockage', async () => {
    const user = userEvent.setup();
    localStorage.setItem(
      CLE_STOCKAGE_AUTH,
      JSON.stringify({ jeton: 'jwt', utilisateur: { id: 1, nom: 'Camille', carburantPrefere: 'GAZOLE' } })
    );

    render(
      <AuthProvider>
        <ComposantTest />
      </AuthProvider>
    );

    await user.click(screen.getByRole('button', { name: /changer carburant/i }));

    await waitFor(() => {
      const stocke = JSON.parse(localStorage.getItem(CLE_STOCKAGE_AUTH));
      expect(stocke.utilisateur.carburantPrefere).toBe('SP95');
    });
  });

  test('inscrire appelle authService.inscrire', async () => {
    const user = userEvent.setup();
    vi.mocked(authService.inscrire).mockResolvedValueOnce({ id: 2, nom: 'Nouveau' });

    render(
      <AuthProvider>
        <ComposantTest />
      </AuthProvider>
    );

    await user.click(screen.getByRole('button', { name: /inscrire/i }));

    expect(authService.inscrire).toHaveBeenCalledWith({
      nom: 'Nouveau',
      email: 'nouveau@test.fr',
      motDePasse: 'secret123',
    });
  });

  test('lève une erreur si useAuth est utilisé en dehors de AuthProvider', () => {
    const espionConsole = vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => render(<ComposantTest />)).toThrow('useAuth doit être utilisé dans un AuthProvider.');
    espionConsole.mockRestore();
  });
});

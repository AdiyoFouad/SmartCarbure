import { act, renderHook, waitFor } from '@testing-library/react';
import { describe, expect, test, vi } from 'vitest';
import { useRequete } from './useRequete';

/** Promesse contrôlée depuis le test. */
function promesseControlee() {
  let resoudre;
  let rejeter;
  const promesse = new Promise((res, rej) => {
    resoudre = res;
    rejeter = rej;
  });
  return { promesse, resoudre, rejeter };
}

describe('useRequete', () => {
  test('passe de « chargement » aux données', async () => {
    const { result } = renderHook(() => useRequete(() => Promise.resolve('station'), []));

    expect(result.current.chargement).toBe(true);
    await waitFor(() => expect(result.current.chargement).toBe(false));
    expect(result.current.donnees).toBe('station');
    expect(result.current.erreur).toBeNull();
  });

  test("renvoie l'erreur levée par le service", async () => {
    const erreur = new Error('panne');
    const { result } = renderHook(() => useRequete(() => Promise.reject(erreur), []));

    await waitFor(() => expect(result.current.chargement).toBe(false));
    expect(result.current.erreur).toBe(erreur);
    expect(result.current.donnees).toBeNull();
  });

  test('relancer refait l’appel (bouton « Réessayer »)', async () => {
    const appel = vi.fn().mockRejectedValueOnce(new Error('panne')).mockResolvedValueOnce('ok');
    const { result } = renderHook(() => useRequete(appel, []));
    await waitFor(() => expect(result.current.erreur).not.toBeNull());

    act(() => result.current.relancer());

    await waitFor(() => expect(result.current.donnees).toBe('ok'));
    expect(result.current.erreur).toBeNull();
    expect(appel).toHaveBeenCalledTimes(2);
  });

  test('relance l’appel quand une dépendance change', async () => {
    const appel = vi.fn((id) => Promise.resolve(`station ${id}`));
    const { result, rerender } = renderHook(({ id }) => useRequete(() => appel(id), [id]), {
      initialProps: { id: '1' },
    });
    await waitFor(() => expect(result.current.donnees).toBe('station 1'));

    rerender({ id: '2' });

    await waitFor(() => expect(result.current.donnees).toBe('station 2'));
  });

  test('ignore une réponse arrivée après un changement de dépendance', async () => {
    const lente = promesseControlee();
    const rapide = promesseControlee();
    const appel = vi.fn((id) => (id === '1' ? lente.promesse : rapide.promesse));
    const { result, rerender } = renderHook(({ id }) => useRequete(() => appel(id), [id]), {
      initialProps: { id: '1' },
    });

    rerender({ id: '2' });
    await act(async () => rapide.resoudre('station 2'));
    await act(async () => lente.resoudre('station 1 (trop tard)'));

    expect(result.current.donnees).toBe('station 2');
  });
});

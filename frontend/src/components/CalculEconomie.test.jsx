import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, test, vi } from 'vitest';
import { ApiError } from '../services/api';
import CalculEconomie, { lireLitres, MESSAGE_SAISIE_LITRES, texteMontant } from './CalculEconomie';

const CARBURANTS = [
  { type: 'GAZOLE', prix: 1.749, disponible: true },
  { type: 'E10', prix: 1.799, disponible: true },
];

function afficher(props = {}) {
  const proprietes = {
    carburants: CARBURANTS,
    carburant: 'GAZOLE',
    litres: '50',
    onChangerCarburant: vi.fn(),
    onChangerLitres: vi.fn(),
    ville: 'Rennes',
    ...props,
  };
  render(<CalculEconomie {...proprietes} />);
  return proprietes;
}

describe('lireLitres et texteMontant', () => {
  test('lireLitres accepte un nombre positif, avec virgule ou point', () => {
    expect(lireLitres('50')).toBe(50);
    expect(lireLitres('40,5')).toBe(40.5);
    expect(lireLitres(' 40.5 ')).toBe(40.5);
  });

  test.each(['', '0', '-5', 'abc', null, undefined])('lireLitres refuse « %s »', (valeur) => {
    expect(lireLitres(valeur)).toBeNull();
  });

  test('texteMontant : économie, surcoût, même prix (montants à deux décimales)', () => {
    expect(texteMontant(1.1)).toBe('Vous économisez 1,10 € sur ce plein');
    expect(texteMontant(-1.5)).toBe('Vous payez 1,50 € de plus sur ce plein');
    expect(texteMontant(0)).toBe('Même prix que la moyenne');
  });
});

describe('Composant CalculEconomie', () => {
  test('liste déroulante des carburants reçus et champ des litres, avec leurs labels', () => {
    afficher();

    const liste = screen.getByLabelText('Carburant');
    expect(liste).toHaveValue('GAZOLE');
    expect(screen.getAllByRole('option').map((o) => o.textContent)).toEqual(['Gazole', 'SP95-E10']);
    expect(screen.getByLabelText('Plein de')).toHaveValue(50);
    expect(screen.getByText('litres')).toBeInTheDocument();
  });

  test('économie : montant en vert et prix moyen de la ville', () => {
    afficher({ resultat: { prixStation: 1.689, prixMoyenLocal: 1.661, litres: 50, montant: 1.1 } });

    const montant = screen.getByText('Vous économisez 1,10 € sur ce plein');
    expect(montant).toHaveClass('calcul-economie__montant--economie');
    expect(screen.getByText('Par rapport au prix moyen à Rennes : 1,661 € / litre')).toBeInTheDocument();
    expect(montant.closest('[aria-live="polite"]')).not.toBeNull();
  });

  test('surcoût et même prix', () => {
    afficher({ resultat: { prixStation: 1.749, prixMoyenLocal: 1.719, litres: 50, montant: -1.5 } });
    expect(screen.getByText('Vous payez 1,50 € de plus sur ce plein')).toHaveClass(
      'calcul-economie__montant--surcout'
    );
  });

  test('montant nul : « Même prix que la moyenne »', () => {
    afficher({ resultat: { prixStation: 1.899, prixMoyenLocal: 1.899, litres: 50, montant: 0 }, ville: null });
    expect(screen.getByText('Même prix que la moyenne')).toBeInTheDocument();
    expect(screen.getByText('Par rapport au prix moyen local : 1,899 € / litre')).toBeInTheDocument();
  });

  test.each(['', '0', '-3'])('volume « %s » : message de saisie, pas de résultat', (litres) => {
    afficher({ litres, resultat: { prixStation: 1.7, prixMoyenLocal: 1.7, litres: 50, montant: 0 } });

    expect(screen.getByText(MESSAGE_SAISIE_LITRES)).toBeInTheDocument();
    expect(screen.queryByText('Même prix que la moyenne')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Plein de')).toHaveAttribute('aria-invalid', 'true');
  });

  test('chargement : « Calcul en cours… »', () => {
    afficher({ chargement: true });
    expect(screen.getByRole('status')).toHaveTextContent('Calcul en cours…');
  });

  test('erreur 503 : message du contrat et « Réessayer »', async () => {
    const { onReessayer } = afficher({ erreur: new ApiError(503), onReessayer: vi.fn() });

    expect(
      screen.getByText('Service temporairement indisponible. Réessayez dans quelques instants.')
    ).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Réessayer' }));
    expect(onReessayer).toHaveBeenCalledTimes(1);
  });

  test('erreurs 400 et 404 : message sans « Réessayer »', () => {
    afficher({ erreur: new ApiError(400), onReessayer: vi.fn() });
    expect(screen.getByText('Calcul impossible pour ce carburant dans cette station.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Réessayer' })).not.toBeInTheDocument();
  });

  test('erreur 404', () => {
    afficher({ erreur: new ApiError(404) });
    expect(screen.getByText('Station introuvable.')).toBeInTheDocument();
  });

  test('transmet les changements de carburant et de volume au parent', async () => {
    const { onChangerCarburant, onChangerLitres } = afficher();

    await userEvent.selectOptions(screen.getByLabelText('Carburant'), 'E10');
    expect(onChangerCarburant).toHaveBeenCalledWith('E10');

    fireEvent.change(screen.getByLabelText('Plein de'), { target: { value: '30' } });
    expect(onChangerLitres).toHaveBeenCalledWith('30');
  });
});

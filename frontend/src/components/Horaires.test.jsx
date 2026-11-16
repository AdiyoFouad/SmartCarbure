import { render, screen, within } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import Horaires, { texteHoraire } from './Horaires';

function texteDuJour(jour) {
  return within(screen.getByRole('rowheader', { name: jour }).closest('tr')).getByRole('cell').textContent;
}

describe('Composant Horaires', () => {
  test('affiche les sept jours avec leurs heures, un jour fermé et un jour non communiqué', () => {
    const horaires = [
      { jour: 'lundi', ferme: false, ouverture: '07:00', fermeture: '21:00' },
      { jour: 'mardi', ferme: false, ouverture: '07:00', fermeture: '21:00' },
      { jour: 'mercredi', ferme: false, ouverture: '07:00', fermeture: '21:00' },
      { jour: 'jeudi', ferme: false, ouverture: '07:00', fermeture: '21:00' },
      { jour: 'vendredi', ferme: false, ouverture: '07:00', fermeture: '21:00' },
      { jour: 'samedi', ferme: false, ouverture: null, fermeture: null },
      { jour: 'dimanche', ferme: true, ouverture: null, fermeture: null },
    ];
    render(<Horaires horaires={horaires} />);

    expect(screen.getAllByRole('row')).toHaveLength(7);
    expect(texteDuJour('Lundi')).toBe('07:00 – 21:00');
    expect(texteDuJour('Samedi')).toBe('Horaires non communiqués');
    expect(texteDuJour('Dimanche')).toBe('Fermé');
  });

  test('liste vide : « Horaires non communiqués »', () => {
    render(<Horaires horaires={[]} />);
    expect(screen.getByText('Horaires non communiqués')).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  test('accepte le format actuel du backend : « 07:00:00 » et champ ferme absent', () => {
    expect(texteHoraire({ jour: 'lundi', ouverture: '07:00:00', fermeture: '22:00:00' })).toBe('07:00 – 22:00');
    expect(texteHoraire({ jour: 'lundi', ouverture: null, fermeture: null })).toBe('Horaires non communiqués');
  });
});

import { render, screen, within } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import TableauCarburants from './TableauCarburants';

const MAINTENANT = new Date('2026-11-14T12:00:00Z');

const CARBURANTS = [
  { type: 'GAZOLE', prix: 1.749, dateMaj: '2026-11-14T11:35:00Z', disponible: true, typeRupture: null, debutRupture: null },
  { type: 'E10', prix: 1.799, dateMaj: '2026-11-14T09:00:00Z', disponible: true, typeRupture: null, debutRupture: null },
  { type: 'SP98', prix: null, dateMaj: null, disponible: false, typeRupture: 'DEFINITIVE', debutRupture: '2026-03-27T17:43:44Z' },
  { type: 'E85', prix: 0.899, dateMaj: '2026-11-10T09:00:00Z', disponible: false, typeRupture: 'TEMPORAIRE', debutRupture: '2026-11-12T09:15:00Z' },
];

/** Ligne d'un carburant : le nom de l'en-tête de ligne commence par son libellé (le badge suit). */
function ligne(libelle) {
  return screen.getByRole('rowheader', { name: new RegExp(`^${libelle}\\b`) }).closest('tr');
}

describe('Composant TableauCarburants', () => {
  test('a deux colonnes (Carburant, Prix au litre) et une ligne par carburant', () => {
    render(<TableauCarburants carburants={CARBURANTS} maintenant={MAINTENANT} />);

    expect(screen.getByRole('table', { name: 'Prix et disponibilité des carburants' })).toBeInTheDocument();
    expect(screen.getAllByRole('columnheader').map((th) => th.textContent)).toEqual(['Carburant', 'Prix au litre']);
    expect(screen.getAllByRole('row')).toHaveLength(5);
  });

  test('affiche le libellé, le prix et la date de mise à jour sous le prix', () => {
    render(<TableauCarburants carburants={CARBURANTS} maintenant={MAINTENANT} />);

    const gazole = within(ligne('Gazole'));
    expect(gazole.getByText('1,749 €')).toHaveClass('tableau-carburants__prix');
    expect(gazole.getByText('mis à jour il y a 25 min')).toBeInTheDocument();
    // E10 a pour libellé « SP95-E10 » (constants/carburants.js)
    expect(within(ligne('SP95-E10')).getByText('mis à jour il y a 3 h')).toBeInTheDocument();
  });

  test('rupture sans prix : badge sous le nom, aucun prix ni date', () => {
    render(<TableauCarburants carburants={CARBURANTS} maintenant={MAINTENANT} />);

    const sp98 = ligne('SP98');
    expect(sp98).toHaveClass('tableau-carburants__ligne--rupture');
    expect(within(sp98).getByRole('rowheader')).toHaveTextContent('SP98 Rupture définitive depuis le 27/03/2026');
    expect(within(sp98).getByRole('cell')).toBeEmptyDOMElement();
    expect(within(ligne('Gazole')).queryByText(/Rupture/)).not.toBeInTheDocument();
  });

  test('rupture avec un dernier prix connu : prix barré, sans date de mise à jour', () => {
    render(<TableauCarburants carburants={CARBURANTS} maintenant={MAINTENANT} />);

    const e85 = within(ligne('E85'));
    const prix = e85.getByText('0,899 €');
    expect(prix.tagName).toBe('DEL');
    expect(prix).toHaveClass('tableau-carburants__prix-barre');
    expect(prix).toHaveTextContent('Dernier prix connu : 0,899 €');
    expect(e85.getByText('Rupture temporaire depuis le 12/11/2026')).toBeInTheDocument();
    expect(e85.queryByText(/mis à jour/)).not.toBeInTheDocument();
  });

  test('message si la station n’a aucun carburant', () => {
    render(<TableauCarburants carburants={[]} />);
    expect(screen.getByText('Aucun carburant renseigné pour cette station.')).toBeInTheDocument();
  });
});

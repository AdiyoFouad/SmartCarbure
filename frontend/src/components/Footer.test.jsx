import { render, screen, within } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import Footer from './Footer';

describe('Composant Footer', () => {
  test('est un pied de page (rôle contentinfo) avec les deux lignes de texte', () => {
    render(<Footer />);

    const pied = screen.getByRole('contentinfo');
    const [donnees, projet] = within(pied).getAllByRole('paragraph');
    expect(donnees).toHaveTextContent(
      "Données : prix-carburants.gouv.fr (nouvel onglet) – Ministère de l'Économie, " +
        'Licence Ouverte / Etalab (nouvel onglet)'
    );
    expect(projet).toHaveTextContent('Projet PDL – Master 1 MIAGE, Université de Rennes');
  });

  test.each([
    ['prix-carburants.gouv.fr', 'https://www.prix-carburants.gouv.fr'],
    ['Licence Ouverte / Etalab', 'https://www.etalab.gouv.fr/licence-ouverte-open-licence/'],
  ])('lien « %s » vers %s, ouvert dans un nouvel onglet', (texte, url) => {
    render(<Footer />);

    // Nom accessible : le texte du lien, une espace, puis « (nouvel onglet) » (texte caché pour les lecteurs d'écran).
    const lien = screen.getByRole('link', { name: `${texte} (nouvel onglet)` });
    expect(lien).toHaveAttribute('href', url);
    expect(lien).toHaveAttribute('target', '_blank');
    expect(lien).toHaveAttribute('rel', 'noopener noreferrer');
  });
});

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, test } from 'vitest';
import PageIntrouvable from './PageIntrouvable';

/** Affiche la page 404 sur /inconnue ; `precedente` simule une page visitée avant dans l'application. */
function afficher({ precedente } = {}) {
  const entrees = precedente ? [precedente, '/inconnue'] : ['/inconnue'];
  return render(
    <MemoryRouter initialEntries={entrees} initialIndex={entrees.length - 1}>
      <Routes>
        <Route path="/" element={<h1>Accueil</h1>} />
        <Route path="/comparer" element={<h1>Comparer</h1>} />
        <Route path="*" element={<PageIntrouvable />} />
      </Routes>
    </MemoryRouter>
  );
}

describe('Page PageIntrouvable', () => {
  test('affiche le titre et le texte explicatif', () => {
    afficher();

    expect(screen.getByRole('heading', { level: 1, name: 'Page introuvable' })).toBeInTheDocument();
    expect(screen.getByText("L'adresse demandée n'existe pas ou a été déplacée.")).toBeInTheDocument();
  });

  test('« Retour à l’accueil » est un lien vers /', async () => {
    afficher({ precedente: '/comparer' });

    const lien = screen.getByRole('link', { name: "Retour à l'accueil" });
    expect(lien).toHaveAttribute('href', '/');
    await userEvent.click(lien);
    expect(await screen.findByRole('heading', { level: 1, name: 'Accueil' })).toBeInTheDocument();
  });

  test('« Page précédente » revient à la page précédente de l’application', async () => {
    afficher({ precedente: '/comparer' });

    await userEvent.click(screen.getByRole('button', { name: 'Page précédente' }));
    expect(await screen.findByRole('heading', { level: 1, name: 'Comparer' })).toBeInTheDocument();
  });

  test('« Page précédente » sans historique (lien direct) : va à l’accueil', async () => {
    afficher();

    await userEvent.click(screen.getByRole('button', { name: 'Page précédente' }));
    expect(await screen.findByRole('heading', { level: 1, name: 'Accueil' })).toBeInTheDocument();
  });
});

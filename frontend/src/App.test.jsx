import { render, screen } from '@testing-library/react';
import { afterEach } from 'vitest';
import App from './App';

afterEach(() => {
  // App utilise BrowserRouter, qui lit l'URL de jsdom : on la remet à / pour les autres tests.
  window.history.replaceState({}, '', '/');
});

test("affiche l'application avec la navigation et la page d'accueil", () => {
  render(<App />);
  expect(screen.getByRole('navigation', { name: 'Navigation principale' })).toBeInTheDocument();
  expect(screen.getByRole('heading', { name: /Trouvez le carburant le moins cher près de chez vous/i })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /rechercher/i })).toBeInTheDocument();
});

test('affiche « Page introuvable » et le pied de page pour une adresse inconnue', () => {
  window.history.pushState({}, '', '/nimporte-quoi');
  render(<App />);
  expect(screen.getByRole('heading', { level: 1, name: 'Page introuvable' })).toBeInTheDocument();
  expect(screen.getByRole('contentinfo')).toHaveTextContent('Projet PDL – Master 1 MIAGE, Université de Rennes');
});

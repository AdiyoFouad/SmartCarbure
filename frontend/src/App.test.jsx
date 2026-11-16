import { render, screen } from '@testing-library/react';
import App from './App';

test("affiche l'application avec la navigation et la page d'accueil", () => {
  render(<App />);
  expect(screen.getByRole('navigation', { name: 'Navigation principale' })).toBeInTheDocument();
  expect(screen.getByRole('heading', { name: /Trouvez le carburant le moins cher près de chez vous/i })).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /rechercher/i })).toBeInTheDocument();
});

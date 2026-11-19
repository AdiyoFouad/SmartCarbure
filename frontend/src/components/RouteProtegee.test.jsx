import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import RouteProtegee from './RouteProtegee';

let mockEstConnecte = false;

vi.mock('../hooks/useAuth', () => ({
  useAuth: () => ({
    estConnecte: mockEstConnecte,
  }),
}));

function PageConnexionPourTest() {
  const location = useLocation();
  return (
    <div>
      <h1>Page Connexion</h1>
      <span data-testid="from-path">{location.state?.from?.pathname || 'aucun'}</span>
    </div>
  );
}

function PageProtegeePourTest() {
  return <h1>Contenu Secret Protégé</h1>;
}

function afficherRoute(cheminInitial = '/favoris') {
  return render(
    <MemoryRouter initialEntries={[cheminInitial]}>
      <Routes>
        <Route
          path="/favoris"
          element={
            <RouteProtegee>
              <PageProtegeePourTest />
            </RouteProtegee>
          }
        />
        <Route path="/connexion" element={<PageConnexionPourTest />} />
      </Routes>
    </MemoryRouter>
  );
}

beforeEach(() => {
  mockEstConnecte = false;
});

describe('Composant RouteProtegee', () => {
  test('redirige vers /connexion avec from dans le state si l\'utilisateur n\'est pas connecté', () => {
    mockEstConnecte = false;
    afficherRoute('/favoris');

    expect(screen.getByRole('heading', { name: /page connexion/i })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: /contenu secret protégé/i })).not.toBeInTheDocument();
    expect(screen.getByTestId('from-path')).toHaveTextContent('/favoris');
  });

  test('affiche les enfants protégés si l\'utilisateur est connecté', () => {
    mockEstConnecte = true;
    afficherRoute('/favoris');

    expect(screen.getByRole('heading', { name: /contenu secret protégé/i })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: /page connexion/i })).not.toBeInTheDocument();
  });
});

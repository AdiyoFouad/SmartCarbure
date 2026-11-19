import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { ApiError } from '../services/api';
import Connexion from './Connexion';

const mockConnecter = vi.fn();
const mockInscrire = vi.fn();
const mockDeconnecter = vi.fn();

let mockEstConnecte = false;
let mockUtilisateur = null;

vi.mock('../hooks/useAuth', () => ({
  useAuth: () => ({
    estConnecte: mockEstConnecte,
    utilisateur: mockUtilisateur,
    connecter: mockConnecter,
    inscrire: mockInscrire,
    deconnecter: mockDeconnecter,
  }),
}));

function afficherPageConnexion() {
  return render(
    <MemoryRouter initialEntries={['/connexion']}>
      <Connexion />
    </MemoryRouter>
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  mockEstConnecte = false;
  mockUtilisateur = null;
});

describe('Page Connexion (F11, F12)', () => {
  test('affiche les deux onglets avec « Se connecter » actif par défaut', () => {
    afficherPageConnexion();

    const ongletConnexion = screen.getByRole('tab', { name: /se connecter/i });
    const ongletInscription = screen.getByRole('tab', { name: /créer un compte/i });

    expect(ongletConnexion).toHaveAttribute('aria-selected', 'true');
    expect(ongletInscription).toHaveAttribute('aria-selected', 'false');

    expect(screen.getByLabelText(/adresse email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^mot de passe/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /se connecter/i })).toBeInTheDocument();
  });

  test('permet de basculer vers l\'onglet « Créer un compte » et revenir', async () => {
    const user = userEvent.setup();
    afficherPageConnexion();

    const ongletInscription = screen.getByRole('tab', { name: /créer un compte/i });
    await user.click(ongletInscription);

    expect(ongletInscription).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByLabelText(/nom complet/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/adresse email/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/mot de passe/i)).toBeInTheDocument();

    const ongletConnexion = screen.getByRole('tab', { name: /se connecter/i });
    await user.click(ongletConnexion);
    expect(ongletConnexion).toHaveAttribute('aria-selected', 'true');
  });

  test('U04: connecte l\'utilisateur avec des identifiants valides', async () => {
    const user = userEvent.setup();
    mockConnecter.mockResolvedValueOnce({
      jeton: 'fake-jwt',
      utilisateur: { id: 1, nom: 'Camille Martin', email: 'camille@exemple.fr' },
    });

    afficherPageConnexion();

    await user.type(screen.getByLabelText(/adresse email/i), 'camille@exemple.fr');
    await user.type(screen.getByLabelText(/^mot de passe/i), 'motdepasse123');
    await user.click(screen.getByRole('button', { name: /se connecter/i }));

    expect(mockConnecter).toHaveBeenCalledWith('camille@exemple.fr', 'motdepasse123');
  });

  test('U05: affiche « Email ou mot de passe incorrect » en cas d\'erreur 401 à la connexion', async () => {
    const user = userEvent.setup();
    mockConnecter.mockRejectedValueOnce(new ApiError(401, 'Email ou mot de passe incorrect'));

    afficherPageConnexion();

    await user.type(screen.getByLabelText(/adresse email/i), 'inconnu@exemple.fr');
    await user.type(screen.getByLabelText(/^mot de passe/i), 'mauvaismdp');
    await user.click(screen.getByRole('button', { name: /se connecter/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Email ou mot de passe incorrect');
    });
  });

  test('affiche un message d\'erreur si l\'un des champs de connexion est vide', async () => {
    const user = userEvent.setup();
    afficherPageConnexion();

    await user.click(screen.getByRole('button', { name: /se connecter/i }));

    expect(screen.getByRole('alert')).toHaveTextContent('Veuillez renseigner votre email et votre mot de passe.');
    expect(mockConnecter).not.toHaveBeenCalled();
  });

  test('affiche une erreur 503 claire si le service est indisponible', async () => {
    const user = userEvent.setup();
    mockConnecter.mockRejectedValueOnce(new ApiError(503));

    afficherPageConnexion();

    await user.type(screen.getByLabelText(/adresse email/i), 'camille@exemple.fr');
    await user.type(screen.getByLabelText(/^mot de passe/i), 'motdepasse');
    await user.click(screen.getByRole('button', { name: /se connecter/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Service temporairement indisponible. Réessayez dans quelques instants.');
    });
  });

  test('U03: refuse l\'inscription avec un email invalide comme « test@ »', async () => {
    const user = userEvent.setup();
    afficherPageConnexion();

    await user.click(screen.getByRole('tab', { name: /créer un compte/i }));
    await user.type(screen.getByLabelText(/nom complet/i), 'Test Nom');
    await user.type(screen.getByLabelText(/adresse email/i), 'test@');
    await user.type(screen.getByLabelText(/mot de passe/i), 'motdepasse123');
    await user.click(screen.getByRole('button', { name: /créer un compte/i }));

    expect(screen.getByRole('alert')).toHaveTextContent("Format d'adresse email invalide.");
    expect(mockInscrire).not.toHaveBeenCalled();
  });

  test('refuse l\'inscription si le mot de passe fait moins de 8 caractères', async () => {
    const user = userEvent.setup();
    afficherPageConnexion();

    await user.click(screen.getByRole('tab', { name: /créer un compte/i }));
    await user.type(screen.getByLabelText(/nom complet/i), 'Test Nom');
    await user.type(screen.getByLabelText(/adresse email/i), 'valide@exemple.fr');
    await user.type(screen.getByLabelText(/mot de passe/i), 'court');
    await user.click(screen.getByRole('button', { name: /créer un compte/i }));

    expect(screen.getByRole('alert')).toHaveTextContent('Le mot de passe doit comporter au moins 8 caractères.');
    expect(mockInscrire).not.toHaveBeenCalled();
  });

  test('U01: crée un compte avec succès, affiche le message de confirmation et bascule sur l\'onglet connexion', async () => {
    const user = userEvent.setup();
    mockInscrire.mockResolvedValueOnce({
      id: 3,
      nom: 'Nouveau Membre',
      email: 'nouveau@exemple.fr',
      carburantPrefere: null,
      rayonPrefere: null,
    });

    afficherPageConnexion();

    await user.click(screen.getByRole('tab', { name: /créer un compte/i }));
    await user.type(screen.getByLabelText(/nom complet/i), 'Nouveau Membre');
    await user.type(screen.getByLabelText(/adresse email/i), 'nouveau@exemple.fr');
    await user.type(screen.getByLabelText(/mot de passe/i), 'motdepasseRobuste');
    await user.click(screen.getByRole('button', { name: /créer un compte/i }));

    expect(mockInscrire).toHaveBeenCalledWith('Nouveau Membre', 'nouveau@exemple.fr', 'motdepasseRobuste');

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Votre compte a été créé avec succès ! Vous pouvez maintenant vous connecter.');
    });

    // L'onglet doit être revenu sur Connexion et l'email prérempli
    expect(screen.getByRole('tab', { name: /se connecter/i })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByLabelText(/adresse email/i)).toHaveValue('nouveau@exemple.fr');
  });

  test('U02: affiche « Cet email est déjà utilisé » en cas d\'erreur 409 à l\'inscription', async () => {
    const user = userEvent.setup();
    mockInscrire.mockRejectedValueOnce(new ApiError(409, 'Cet email est déjà utilisé'));

    afficherPageConnexion();

    await user.click(screen.getByRole('tab', { name: /créer un compte/i }));
    await user.type(screen.getByLabelText(/nom complet/i), 'Camille Martin');
    await user.type(screen.getByLabelText(/adresse email/i), 'camille@exemple.fr');
    await user.type(screen.getByLabelText(/mot de passe/i), 'motdepasse');
    await user.click(screen.getByRole('button', { name: /créer un compte/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Cet email est déjà utilisé');
    });
  });

  test('affiche l\'espace utilisateur si déjà connecté', async () => {
    const user = userEvent.setup();
    mockEstConnecte = true;
    mockUtilisateur = { id: 1, nom: 'Camille Martin', email: 'camille@exemple.fr' };

    afficherPageConnexion();

    expect(screen.getByText(/vous êtes connecté en tant que/i)).toBeInTheDocument();
    expect(screen.getByText(/Camille Martin/i)).toBeInTheDocument();

    const boutonDeconnexion = screen.getByRole('button', { name: /se déconnecter/i });
    expect(boutonDeconnexion).toBeInTheDocument();

    await user.click(boutonDeconnexion);
    expect(mockDeconnecter).toHaveBeenCalled();
  });
});

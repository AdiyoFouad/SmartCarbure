import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, test, vi } from 'vitest';
import MessageEtat from './MessageEtat';

describe('Composant MessageEtat', () => {
  test('annonce le message avec role="status"', () => {
    render(<MessageEtat type="chargement" message="Chargement de la station…" />);
    expect(screen.getByRole('status')).toHaveTextContent('Chargement de la station…');
  });

  test('pas de bouton « Réessayer » sans onReessayer', () => {
    render(<MessageEtat type="vide" message="Aucune station trouvée." />);
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  test('le bouton « Réessayer » appelle onReessayer', async () => {
    const onReessayer = vi.fn();
    render(<MessageEtat type="erreur" message="Service indisponible." onReessayer={onReessayer} />);

    await userEvent.click(screen.getByRole('button', { name: 'Réessayer' }));

    expect(onReessayer).toHaveBeenCalledTimes(1);
  });

  test('affiche le contenu supplémentaire (lien de retour)', () => {
    render(
      <MessageEtat type="erreur" message="Station introuvable.">
        <a href="/">Retour à l'accueil</a>
      </MessageEtat>
    );
    expect(screen.getByRole('link', { name: "Retour à l'accueil" })).toBeInTheDocument();
  });
});

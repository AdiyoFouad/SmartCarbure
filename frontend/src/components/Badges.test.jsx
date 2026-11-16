import { render, screen } from '@testing-library/react';
import { describe, expect, test } from 'vitest';
import BadgeAutomate from './BadgeAutomate';
import BadgeEmplacement from './BadgeEmplacement';
import BadgeRupture from './BadgeRupture';

describe('Composant BadgeRupture', () => {
  test('rupture temporaire avec sa date de début', () => {
    render(<BadgeRupture typeRupture="TEMPORAIRE" debutRupture="2026-10-26T13:30:47Z" />);
    expect(screen.getByText('Rupture temporaire depuis le 26/10/2026')).toBeInTheDocument();
  });

  test('rupture définitive', () => {
    render(<BadgeRupture typeRupture="DEFINITIVE" debutRupture="2026-03-27T17:43:44Z" />);
    expect(screen.getByText('Rupture définitive depuis le 27/03/2026')).toBeInTheDocument();
  });

  test('sans type ni date : « En rupture »', () => {
    render(<BadgeRupture typeRupture={null} debutRupture={null} />);
    expect(screen.getByText('En rupture')).toBeInTheDocument();
  });
});

describe('Composant BadgeAutomate', () => {
  test('affiche « Automate 24h/24 »', () => {
    render(<BadgeAutomate />);
    expect(screen.getByText('Automate 24h/24')).toBeInTheDocument();
  });
});

describe('Composant BadgeEmplacement', () => {
  test('affiche « Autoroute » pour une station d’autoroute', () => {
    render(<BadgeEmplacement emplacement="AUTOROUTE" />);
    expect(screen.getByText('Autoroute')).toBeInTheDocument();
  });

  test('n’affiche rien pour une station de route ou un emplacement inconnu', () => {
    const { container, rerender } = render(<BadgeEmplacement emplacement="ROUTE" />);
    expect(container).toBeEmptyDOMElement();
    rerender(<BadgeEmplacement emplacement={null} />);
    expect(container).toBeEmptyDOMElement();
  });
});

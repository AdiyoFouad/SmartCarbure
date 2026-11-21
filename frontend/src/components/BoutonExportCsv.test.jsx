import { fireEvent, render, screen } from '@testing-library/react';
import { afterEach, describe, expect, test, vi } from 'vitest';
import { telechargerCsv } from '../utils/csv';
import BoutonExportCsv from './BoutonExportCsv';

// On garde le vrai genererCsv et le vrai nomFichierCsv ; seul le téléchargement est remplacé,
// pour vérifier le fichier produit sans lancer de vrai téléchargement.
vi.mock('../utils/csv', async (importOriginal) => ({
  ...(await importOriginal()),
  telechargerCsv: vi.fn(),
}));

/** Station au format du contrat d'API (nom null : l'API ne fournit pas de nom). */
const STATION_RENNES = {
  id: '35000001',
  nom: null,
  adresse: '12 boulevard de la Liberté',
  codePostal: '35000',
  ville: 'Rennes',
  distanceKm: null,
  carburants: [{ type: 'GAZOLE', prix: 1.749, disponible: true, dateMaj: '2026-11-03T13:40:00Z' }],
};

afterEach(() => {
  vi.clearAllMocks();
  vi.useRealTimers();
});

describe('BoutonExportCsv', () => {
  /** Sans station à exporter, le bouton est grisé et ne télécharge rien. */
  test('est désactivé quand il n’y a aucune station', () => {
    render(<BoutonExportCsv stations={[]} carburant="GAZOLE" />);

    const bouton = screen.getByRole('button', { name: /exporter en csv/i });
    expect(bouton).toBeDisabled();
    fireEvent.click(bouton);
    expect(telechargerCsv).not.toHaveBeenCalled();
  });

  /** Une liste absente (null) est traitée comme une liste vide. */
  test('est désactivé quand la liste de stations est absente', () => {
    render(<BoutonExportCsv stations={null} carburant="GAZOLE" />);

    expect(screen.getByRole('button', { name: /exporter en csv/i })).toBeDisabled();
  });

  /** Un clic télécharge le fichier du jour, avec une ligne par station au format du contrat. */
  test('télécharge les stations affichées au format du contrat', () => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date(2026, 10, 20, 10, 0));
    render(<BoutonExportCsv stations={[STATION_RENNES]} carburant="GAZOLE" />);

    fireEvent.click(screen.getByRole('button', { name: /exporter en csv/i }));

    expect(telechargerCsv).toHaveBeenCalledTimes(1);
    const [contenu, nomFichier] = telechargerCsv.mock.calls[0];
    expect(nomFichier).toBe('smartcarbure-stations-2026-11-20.csv');
    expect(contenu).toContain(';12 boulevard de la Liberté;35000;Rennes;Gazole;1,749;Oui;;2026-11-03T13:40:00Z\r\n');
  });

  /** La classe donnée par la page s'ajoute à celle du composant (styles de Resultats.css). */
  test('ajoute la classe CSS donnée par la page', () => {
    render(<BoutonExportCsv stations={[STATION_RENNES]} carburant="GAZOLE" className="resultats__export" />);

    expect(screen.getByRole('button', { name: /exporter en csv/i })).toHaveClass('bouton-export-csv', 'resultats__export');
  });
});

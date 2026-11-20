import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, test, vi } from 'vitest';
import { STATIONS } from '../data/stations';
import { ApiError } from '../services/api';
import { calculerEconomie } from '../services/economieService';
import { obtenirStation } from '../services/stationService';
import FicheStation, { DELAI_CALCUL_MS, ligneAdresse, ligneZone } from './FicheStation';

/*
 * Les services sont simulés : la page reçoit les stations fictives de data/stations.js (celles du mode
 * démonstration) ou une ApiError. Leaflet ne peut pas dessiner de carte dans jsdom : CarteStations
 * est remplacée par un simple élément, en gardant sa fonction positionValide.
 */
vi.mock('../services/stationService', () => ({ obtenirStation: vi.fn() }));
vi.mock('../services/economieService', () => ({ calculerEconomie: vi.fn() }));
vi.mock('../components/CarteStations', async (importOriginal) => ({
  ...(await importOriginal()),
  default: ({ stations, compacte }) => (
    <div data-testid="carte" data-compacte={String(compacte)}>
      {stations.length} station
    </div>
  ),
}));

const RESULTAT_SURCOUT = { prixStation: 1.749, prixMoyenLocal: 1.719, litres: 50, montant: -1.5 };

function stationFictive(id) {
  return structuredClone(STATIONS.find((s) => s.id === id));
}

/** Affiche la fiche ; `precedente` simule une page visitée avant (historique de l'application). */
function afficherFiche(id, { precedente } = {}) {
  const entrees = precedente ? [precedente, `/stations/${id}`] : [`/stations/${id}`];
  return render(
    <MemoryRouter initialEntries={entrees} initialIndex={entrees.length - 1}>
      <Routes>
        <Route path="/stations/:id" element={<FicheStation />} />
        <Route path="/resultats" element={<h1>Résultats</h1>} />
        <Route path="/" element={<h1>Accueil</h1>} />
      </Routes>
    </MemoryRouter>
  );
}

function texteDuJour(jour) {
  return within(screen.getByRole('rowheader', { name: jour }).closest('tr')).getByRole('cell').textContent;
}

/** Ligne d'un carburant dans le tableau des prix (le badge de rupture suit le libellé). */
function ligneCarburant(libelle) {
  return screen.getByRole('rowheader', { name: new RegExp(`^${libelle}\\b`) }).closest('tr');
}

/** Attend plus que le délai de saisie, pour vérifier qu'aucun appel n'est parti. */
function attendreDelaiCalcul() {
  return new Promise((resoudre) => setTimeout(resoudre, DELAI_CALCUL_MS + 150));
}

beforeEach(() => {
  vi.mocked(obtenirStation).mockReset();
  vi.mocked(calculerEconomie).mockReset();
  vi.mocked(calculerEconomie).mockResolvedValue(RESULTAT_SURCOUT);
});

describe('ligneAdresse et ligneZone', () => {
  test('assemblent les parties connues, sans « null »', () => {
    const station = stationFictive('35000101');
    expect(ligneAdresse(station)).toBe('12 boulevard de la Liberté, 35000 Rennes');
    expect(ligneZone(station)).toBe('Ille-et-Vilaine (35), Bretagne');

    expect(ligneAdresse({ adresse: null, codePostal: null, ville: 'Rennes' })).toBe('Rennes');
    expect(ligneZone({ departement: { code: null, nom: 'Ille-et-Vilaine' }, region: null })).toBe('Ille-et-Vilaine');
    expect(ligneZone({ departement: null, region: { code: '53', nom: 'Bretagne' } })).toBe('Bretagne');
    expect(ligneZone({ departement: null, region: null })).toBe('');
  });
});

describe('Page FicheStation', () => {
  test('affiche un message de chargement puis la station demandée', async () => {
    vi.mocked(obtenirStation).mockResolvedValue(stationFictive('35000101'));
    afficherFiche('35000101');

    expect(screen.getByRole('status')).toHaveTextContent('Chargement de la station…');
    expect(await screen.findByRole('heading', { level: 1, name: '12 boulevard de la Liberté' })).toBeInTheDocument();
    expect(obtenirStation).toHaveBeenCalledWith('35000101');
  });

  test('en-tête : badge Automate (plein) au-dessus du titre, adresse et département / région', async () => {
    vi.mocked(obtenirStation).mockResolvedValue(stationFictive('35000101'));
    afficherFiche('35000101');
    const titre = await screen.findByRole('heading', { level: 1 });

    const badge = screen.getByText('Automate 24h/24');
    expect(badge).toHaveClass('badge-automate--plein');
    expect(badge.compareDocumentPosition(titre) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(screen.getByText('12 boulevard de la Liberté, 35000 Rennes')).toBeInTheDocument();
    expect(screen.getByText('Ille-et-Vilaine (35), Bretagne')).toBeInTheDocument();
    expect(screen.queryByText('Autoroute')).not.toBeInTheDocument();
  });

  test('cartes de la maquette dans l’ordre, avec les services en pastilles et la carte compacte', async () => {
    vi.mocked(obtenirStation).mockResolvedValue(stationFictive('35000101'));
    afficherFiche('35000101');
    await screen.findByRole('heading', { level: 1 });

    expect(screen.getAllByRole('heading', { level: 2 }).map((h) => h.textContent)).toEqual([
      'Prix des carburants',
      "Calcul d'économie",
      'Services',
      'Localisation',
      "Horaires d'ouverture",
    ]);
    expect(within(ligneCarburant('Gazole')).getByText('1,749 €')).toBeInTheDocument();
    expect(texteDuJour('Lundi')).toBe('07:00 – 21:00');
    expect(screen.getByText('Station de gonflage').tagName).toBe('LI');
    expect(screen.getByTestId('carte')).toHaveAttribute('data-compacte', 'true');
    expect(screen.getByText('Automate carte bancaire disponible 24h/24.')).toBeInTheDocument();
  });

  test('n’affiche jamais « null » ni « undefined » (nom toujours null)', async () => {
    vi.mocked(obtenirStation).mockResolvedValue(stationFictive('35200102'));
    const { container } = afficherFiche('35200102');
    await screen.findByRole('heading', { level: 1 });

    expect(container.textContent).not.toMatch(/null|undefined/);
  });

  test('ruptures (F8) : badge sous le nom, dernier prix connu barré, aucun prix si prix null', async () => {
    vi.mocked(obtenirStation).mockResolvedValue(stationFictive('35200102'));
    afficherFiche('35200102');
    await screen.findByRole('heading', { level: 1 });

    // E85 en rupture temporaire avec un dernier prix connu : barré, pas un prix normal
    const e85 = ligneCarburant('E85');
    expect(within(e85).getByText('Rupture temporaire depuis le 12/11/2026')).toBeInTheDocument();
    const prixBarre = within(e85).getByText('0,899 €');
    expect(prixBarre.tagName).toBe('DEL');
    expect(prixBarre).toHaveClass('tableau-carburants__prix-barre');

    // SP98 en rupture définitive sans prix : seulement le badge
    const sp98 = ligneCarburant('SP98');
    expect(within(sp98).getByText('Rupture définitive depuis le 27/03/2026')).toBeInTheDocument();
    expect(within(sp98).getByRole('cell')).toBeEmptyDOMElement();
  });

  test('station d’autoroute : badge Autoroute à côté du badge Automate, horaires non communiqués', async () => {
    vi.mocked(obtenirStation).mockResolvedValue(stationFictive('35510103'));
    afficherFiche('35510103');
    await screen.findByRole('heading', { level: 1 });

    expect(screen.getByText('Autoroute').parentElement).toBe(screen.getByText('Automate 24h/24').parentElement);
    expect(screen.getByText('Horaires non communiqués')).toBeInTheDocument();
  });

  test('jour fermé, jour non communiqué, ni services ni automate', async () => {
    vi.mocked(obtenirStation).mockResolvedValue(stationFictive('35700104'));
    afficherFiche('35700104');
    await screen.findByRole('heading', { level: 1 });

    expect(texteDuJour('Dimanche')).toBe('Fermé');
    expect(texteDuJour('Samedi')).toBe('Horaires non communiqués');
    expect(screen.queryByRole('heading', { name: 'Services' })).not.toBeInTheDocument();
    expect(screen.queryByText('Automate 24h/24')).not.toBeInTheDocument();
    expect(screen.queryByText('Automate carte bancaire disponible 24h/24.')).not.toBeInTheDocument();
  });

  test('pas de carte pour une station sans coordonnées', async () => {
    const station = stationFictive('35000101');
    station.position = { latitude: null, longitude: null };
    vi.mocked(obtenirStation).mockResolvedValue(station);
    afficherFiche('35000101');
    await screen.findByRole('heading', { level: 1 });

    expect(screen.queryByTestId('carte')).not.toBeInTheDocument();
  });

  test('accepte le format actuel du backend (nom absent, heures « 07:00:00 », sans ferme)', async () => {
    const station = stationFictive('35000101');
    delete station.nom;
    delete station.distanceKm;
    station.horaires = station.horaires.map(({ jour }) => ({ jour, ouverture: '07:00:00', fermeture: '22:00:00' }));
    vi.mocked(obtenirStation).mockResolvedValue(station);
    const { container } = afficherFiche('35000101');
    await screen.findByRole('heading', { level: 1, name: '12 boulevard de la Liberté' });

    expect(texteDuJour('Lundi')).toBe('07:00 – 22:00');
    expect(container.textContent).not.toMatch(/null|undefined/);
  });

  test('404 : « Station introuvable. » avec un lien vers l’accueil, sans « Réessayer »', async () => {
    vi.mocked(obtenirStation).mockRejectedValue(new ApiError(404));
    afficherFiche('99999999');

    expect(await screen.findByText('Station introuvable.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: "Retour à l'accueil" })).toHaveAttribute('href', '/');
    expect(screen.queryByRole('button', { name: 'Réessayer' })).not.toBeInTheDocument();
    expect(calculerEconomie).not.toHaveBeenCalled();
  });

  test('503 : message du contrat, puis « Réessayer » affiche la station', async () => {
    vi.mocked(obtenirStation)
      .mockRejectedValueOnce(new ApiError(503))
      .mockResolvedValueOnce(stationFictive('35000101'));
    afficherFiche('35000101');

    expect(
      await screen.findByText('Service temporairement indisponible. Réessayez dans quelques instants.')
    ).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Réessayer' }));

    expect(await screen.findByRole('heading', { level: 1, name: '12 boulevard de la Liberté' })).toBeInTheDocument();
    expect(obtenirStation).toHaveBeenCalledTimes(2);
  });

  test('autre erreur : message générique avec « Réessayer »', async () => {
    vi.mocked(obtenirStation).mockRejectedValue(new ApiError(500));
    afficherFiche('35000101');

    expect(await screen.findByText('Une erreur inattendue est survenue. Réessayez plus tard.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Réessayer' })).toBeInTheDocument();
  });
});

describe('Page FicheStation : bouton Retour', () => {
  test('revient à la page précédente de l’application', async () => {
    vi.mocked(obtenirStation).mockResolvedValue(stationFictive('35000101'));
    afficherFiche('35000101', { precedente: '/resultats' });
    await screen.findByRole('heading', { level: 1, name: '12 boulevard de la Liberté' });

    await userEvent.click(screen.getByRole('button', { name: 'Retour' }));
    expect(await screen.findByRole('heading', { level: 1, name: 'Résultats' })).toBeInTheDocument();
  });

  test('sans historique (lien direct) : va à l’accueil', async () => {
    vi.mocked(obtenirStation).mockResolvedValue(stationFictive('35000101'));
    afficherFiche('35000101');
    await screen.findByRole('heading', { level: 1, name: '12 boulevard de la Liberté' });

    await userEvent.click(screen.getByRole('button', { name: 'Retour' }));
    expect(await screen.findByRole('heading', { level: 1, name: 'Accueil' })).toBeInTheDocument();
  });
});

describe('Page FicheStation : calcul d’économie (F15)', () => {
  test('premier carburant disponible et 50 litres par défaut, résultat affiché', async () => {
    vi.mocked(obtenirStation).mockResolvedValue(stationFictive('35000101'));
    afficherFiche('35000101');

    expect(await screen.findByText('Vous payez 1,50 € de plus sur ce plein')).toBeInTheDocument();
    expect(screen.getByText('Par rapport au prix moyen à Rennes : 1,719 € / litre')).toBeInTheDocument();
    expect(calculerEconomie).toHaveBeenCalledTimes(1);
    expect(calculerEconomie).toHaveBeenCalledWith({ idStation: '35000101', carburant: 'GAZOLE', litres: 50 });
    expect(screen.getByLabelText('Carburant')).toHaveValue('GAZOLE');
  });

  test('la liste ne contient que les carburants disponibles (ruptures exclues)', async () => {
    vi.mocked(obtenirStation).mockResolvedValue(stationFictive('35200102'));
    afficherFiche('35200102');
    await screen.findByRole('heading', { level: 1 });

    const liste = screen.getByLabelText('Carburant');
    expect(within(liste).getAllByRole('option').map((o) => o.value)).toEqual(['GAZOLE', 'E10']);
  });

  test('recalcule quand le carburant change', async () => {
    vi.mocked(obtenirStation).mockResolvedValue(stationFictive('35000101'));
    afficherFiche('35000101');
    await screen.findByText('Vous payez 1,50 € de plus sur ce plein');

    vi.mocked(calculerEconomie).mockResolvedValueOnce({ ...RESULTAT_SURCOUT, montant: 2.25 });
    await userEvent.selectOptions(screen.getByLabelText('Carburant'), 'E10');

    expect(await screen.findByText('Vous économisez 2,25 € sur ce plein')).toBeInTheDocument();
    expect(calculerEconomie).toHaveBeenLastCalledWith({ idStation: '35000101', carburant: 'E10', litres: 50 });
  });

  test('saisie des litres : un seul appel après le délai, avec la dernière valeur', async () => {
    vi.mocked(obtenirStation).mockResolvedValue(stationFictive('35000101'));
    afficherFiche('35000101');
    await screen.findByText('Vous payez 1,50 € de plus sur ce plein');
    vi.mocked(calculerEconomie).mockClear();

    const champ = screen.getByLabelText('Plein de');
    fireEvent.change(champ, { target: { value: '3' } });
    fireEvent.change(champ, { target: { value: '30' } });
    fireEvent.change(champ, { target: { value: '35' } });

    await waitFor(() => expect(calculerEconomie).toHaveBeenCalledTimes(1));
    expect(calculerEconomie).toHaveBeenCalledWith({ idStation: '35000101', carburant: 'GAZOLE', litres: 35 });
  });

  test.each(['', '0', '-2'])('litres « %s » : message de saisie et aucun appel', async (valeur) => {
    vi.mocked(obtenirStation).mockResolvedValue(stationFictive('35000101'));
    afficherFiche('35000101');
    await screen.findByText('Vous payez 1,50 € de plus sur ce plein');
    vi.mocked(calculerEconomie).mockClear();

    fireEvent.change(screen.getByLabelText('Plein de'), { target: { value: valeur } });

    expect(screen.getByText('Saisissez un volume de plein supérieur à 0 litre.')).toBeInTheDocument();
    await attendreDelaiCalcul();
    expect(calculerEconomie).not.toHaveBeenCalled();
  });

  test('erreur 503 du calcul : message dans la carte, le reste de la fiche reste affiché, « Réessayer »', async () => {
    vi.mocked(obtenirStation).mockResolvedValue(stationFictive('35000101'));
    vi.mocked(calculerEconomie).mockRejectedValueOnce(new ApiError(503));
    afficherFiche('35000101');

    const carte = (await screen.findByRole('heading', { level: 2, name: "Calcul d'économie" })).closest('section');
    expect(
      await within(carte).findByText('Service temporairement indisponible. Réessayez dans quelques instants.')
    ).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: 'Prix des carburants' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 1, name: '12 boulevard de la Liberté' })).toBeInTheDocument();

    await userEvent.click(within(carte).getByRole('button', { name: 'Réessayer' }));
    expect(await screen.findByText('Vous payez 1,50 € de plus sur ce plein')).toBeInTheDocument();
    expect(calculerEconomie).toHaveBeenCalledTimes(2);
  });

  test('erreur 400 du calcul : message sans casser la page', async () => {
    vi.mocked(obtenirStation).mockResolvedValue(stationFictive('35000101'));
    vi.mocked(calculerEconomie).mockRejectedValue(new ApiError(400));
    afficherFiche('35000101');

    expect(await screen.findByText('Calcul impossible pour ce carburant dans cette station.')).toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: "Horaires d'ouverture" })).toBeInTheDocument();
  });

  test('section masquée si aucun carburant n’est disponible', async () => {
    const station = stationFictive('35000101');
    station.carburants = station.carburants.map((c) => ({
      ...c, disponible: false, typeRupture: 'TEMPORAIRE', debutRupture: '2026-11-12T09:15:00Z',
    }));
    vi.mocked(obtenirStation).mockResolvedValue(station);
    afficherFiche('35000101');
    await screen.findByRole('heading', { level: 1 });

    expect(screen.queryByRole('heading', { name: "Calcul d'économie" })).not.toBeInTheDocument();
    await attendreDelaiCalcul();
    expect(calculerEconomie).not.toHaveBeenCalled();
  });
});

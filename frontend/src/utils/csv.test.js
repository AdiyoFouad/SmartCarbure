import { afterEach, describe, expect, it, vi } from 'vitest';
import { genererCsv, nomFichierCsv, telechargerCsv } from './csv';

/** Exemple du contrat d'API : une station de Rennes à 1,4 km. */
const stationRennes = {
  id: '35000001',
  nom: 'Station Rennes Centre',
  adresse: '12 boulevard de la Liberté',
  codePostal: '35000',
  ville: 'Rennes',
  carburants: [
    { type: 'GAZOLE', prix: 1.749, dateMaj: '2026-11-03T13:40:00Z', disponible: true },
    { type: 'SP95', prix: 1.859, dateMaj: '2026-11-03T13:40:00Z', disponible: true },
  ],
  distanceKm: 1.4,
};

/** Lignes du fichier, sans le BOM ni la dernière fin de ligne. */
function lignes(csv) {
  return csv.slice(1).split('\r\n').slice(0, -1);
}

describe('genererCsv', () => {
  /** Le fichier commence par le BOM UTF-8 puis la ligne d'en-tête exacte du contrat. */
  it("commence par le BOM puis l'en-tête du contrat", () => {
    const csv = genererCsv([], 'GAZOLE');

    expect(csv.startsWith('\uFEFF')).toBe(true);
    expect(lignes(csv)).toEqual([
      'Nom;Adresse;Code postal;Ville;Carburant;Prix (€/L);Disponible;Distance (km);Mise à jour',
    ]);
  });

  /** Exemple du contrat : virgule décimale, « Oui », distance et date telle que reçue. */
  it("écrit une ligne par station au format de l'exemple du contrat", () => {
    const csv = genererCsv([stationRennes], 'GAZOLE');

    expect(lignes(csv)[1]).toBe(
      'Station Rennes Centre;12 boulevard de la Liberté;35000;Rennes;Gazole;1,749;Oui;1,4;2026-11-03T13:40:00Z',
    );
  });

  /** Chaque ligne se termine par \r\n, y compris la dernière. */
  it('termine chaque ligne par \\r\\n', () => {
    const csv = genererCsv([stationRennes, stationRennes], 'GAZOLE');

    expect(csv.endsWith('\r\n')).toBe(true);
    expect(csv.split('\r\n')).toHaveLength(4); // en-tête + 2 stations + chaîne vide finale
  });

  /** Seul le prix du carburant recherché est exporté, avec son libellé affiché (E10 → SP95-E10). */
  it('utilise le prix et le libellé du carburant recherché', () => {
    const station = {
      ...stationRennes,
      carburants: [{ type: 'E10', prix: 1.699, dateMaj: '2026-11-03T10:00:00Z', disponible: true }],
    };

    const colonnes = lignes(genererCsv([station], 'E10'))[1].split(';');

    expect(colonnes[4]).toBe('SP95-E10');
    expect(colonnes[5]).toBe('1,699');
  });

  /** Carburant en rupture : la colonne Disponible vaut « Non ». */
  it('écrit « Non » pour un carburant en rupture', () => {
    const station = {
      ...stationRennes,
      carburants: [{ type: 'GAZOLE', prix: 1.749, dateMaj: '2026-11-03T13:40:00Z', disponible: false }],
    };

    expect(lignes(genererCsv([station], 'GAZOLE'))[1].split(';')[6]).toBe('Non');
  });

  /** Recherche par ville : distanceKm vaut null, la colonne Distance reste vide. */
  it('laisse la distance vide pour une recherche par ville', () => {
    const station = { ...stationRennes, distanceKm: null };

    expect(lignes(genererCsv([station], 'GAZOLE'))[1].split(';')[7]).toBe('');
  });

  /** Station sans le carburant recherché : prix, disponibilité et date restent vides. */
  it('laisse les colonnes de prix vides si la station ne vend pas ce carburant', () => {
    const colonnes = lignes(genererCsv([stationRennes], 'E85'))[1].split(';');

    expect(colonnes.slice(5)).toEqual(['', '', '1,4', '']);
  });

  /** Un texte avec ; ou " est entouré de guillemets, et ses guillemets sont doublés. */
  it('met entre guillemets les textes qui contiennent ; ou "', () => {
    const station = { ...stationRennes, nom: 'Station "Le Relais"', adresse: 'Rue A; bâtiment B' };

    const ligne = lignes(genererCsv([station], 'GAZOLE'))[1];

    expect(ligne.startsWith('"Station ""Le Relais""";"Rue A; bâtiment B";')).toBe(true);
  });

  /** Nom absent (l'API ne fournit pas de nom de station) ou liste null : pas d'erreur. */
  it('gère un nom absent et une liste de stations null', () => {
    const station = { ...stationRennes, nom: null };

    expect(lignes(genererCsv([station], 'GAZOLE'))[1].startsWith(';12 boulevard')).toBe(true);
    expect(lignes(genererCsv(null, 'GAZOLE'))).toHaveLength(1);
  });
});

describe('nomFichierCsv', () => {
  /** Le nom contient la date du jour au format AAAA-MM-JJ. */
  it('donne smartcarbure-stations-AAAA-MM-JJ.csv', () => {
    expect(nomFichierCsv(new Date(2026, 10, 3))).toBe('smartcarbure-stations-2026-11-03.csv');
  });
});

describe('telechargerCsv', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    vi.useRealTimers();
    delete URL.createObjectURL;
    delete URL.revokeObjectURL;
  });

  /** Crée un Blob CSV, clique sur un lien de téléchargement invisible, puis libère l'URL. */
  it('fait télécharger le contenu sous le nom donné', () => {
    vi.useFakeTimers();
    // jsdom (le faux navigateur des tests) ne sait pas créer d'URL de fichier : on les simule
    URL.createObjectURL = vi.fn(() => 'blob:fichier-csv');
    URL.revokeObjectURL = vi.fn();
    let lienClique = null;
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function enregistrerLien() {
      lienClique = this;
    });

    telechargerCsv('contenu', 'stations.csv');

    const blob = URL.createObjectURL.mock.calls[0][0];
    expect(blob.type).toBe('text/csv;charset=utf-8');
    expect(lienClique.download).toBe('stations.csv');
    expect(lienClique.href).toBe('blob:fichier-csv');
    expect(document.querySelector('a[download]')).toBeNull(); // le lien a été retiré de la page

    vi.runAllTimers();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:fichier-csv');
  });
});

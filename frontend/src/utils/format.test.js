import { describe, expect, test } from 'vitest';
import { formaterDate, formaterDateMaj, formaterDistance, formaterPrix } from './format';

describe('formaterPrix', () => {
  test('affiche trois décimales avec une virgule et le symbole €', () => {
    expect(formaterPrix(1.789)).toBe('1,789 €');
    expect(formaterPrix(1.7)).toBe('1,700 €');
    expect(formaterPrix(0.899)).toBe('0,899 €');
  });

  test('affiche « Prix indisponible » pour un prix inconnu', () => {
    expect(formaterPrix(null)).toBe('Prix indisponible');
    expect(formaterPrix(undefined)).toBe('Prix indisponible');
    expect(formaterPrix(Number.NaN)).toBe('Prix indisponible');
  });
});

describe('formaterDistance', () => {
  test('en mètres sous 1 km', () => {
    expect(formaterDistance(0.45)).toBe('450 m');
    expect(formaterDistance(0)).toBe('0 m');
  });

  test('en kilomètres avec une décimale au-delà', () => {
    expect(formaterDistance(3.24)).toBe('3,2 km');
    expect(formaterDistance(1)).toBe('1,0 km');
  });

  test('chaîne vide pour une distance inconnue (recherche par ville, fiche)', () => {
    expect(formaterDistance(null)).toBe('');
    expect(formaterDistance(undefined)).toBe('');
  });
});

describe('formaterDateMaj', () => {
  const maintenant = new Date('2026-11-14T12:00:00Z');

  test('minutes, heures et jours', () => {
    expect(formaterDateMaj('2026-11-14T11:35:00Z', maintenant)).toBe('mis à jour il y a 25 min');
    expect(formaterDateMaj('2026-11-14T09:00:00Z', maintenant)).toBe('mis à jour il y a 3 h');
    expect(formaterDateMaj('2026-11-12T08:00:00Z', maintenant)).toBe('mis à jour il y a 2 j');
  });

  test('moins d’une minute ou date future : « à l’instant »', () => {
    expect(formaterDateMaj('2026-11-14T11:59:30Z', maintenant)).toBe("mis à jour à l'instant");
    expect(formaterDateMaj('2026-11-14T12:05:00Z', maintenant)).toBe("mis à jour à l'instant");
  });

  test('chaîne vide pour une date absente ou invalide', () => {
    expect(formaterDateMaj(null, maintenant)).toBe('');
    expect(formaterDateMaj('pas une date', maintenant)).toBe('');
  });
});

describe('formaterDate', () => {
  test('format français jj/mm/aaaa', () => {
    expect(formaterDate('2026-10-26T13:30:47Z')).toBe('26/10/2026');
  });

  test("utilise l'heure de Paris : 23:30 UTC est déjà le lendemain à Paris", () => {
    expect(formaterDate('2026-10-26T23:30:00Z')).toBe('27/10/2026');
  });

  test('chaîne vide pour une date absente', () => {
    expect(formaterDate(null)).toBe('');
  });
});

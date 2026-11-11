package fr.miage.smartcarbure.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.miage.smartcarbure.dto.EconomieResponse;
import fr.miage.smartcarbure.model.PrixCarburant;
import fr.miage.smartcarbure.model.Station;
import fr.miage.smartcarbure.model.enums.Carburant;
import fr.miage.smartcarbure.model.enums.TypeRupture;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests du calcul d'économie sur un plein (F15). Aucun appel réseau : les stations sont construites à la main. */
class EconomieServiceTest {

    private final EconomieService service = new EconomieService();

    // Exemple du contrat d'API : 1.749 dans la station, moyenne (1.749 + 1.779 + 1.785) / 3 = 1.771
    private final Station stationA = station("1", prix(Carburant.GAZOLE, 1.749));
    private final Station stationB = station("2", prix(Carburant.GAZOLE, 1.779));
    private final Station stationC = station("3", prix(Carburant.GAZOLE, 1.785));

    // ------------------------------------------------------------------
    // Calculs
    // ------------------------------------------------------------------

    @Test
    void exempleDuContratEconomieDe1Euro10() {
        EconomieResponse r = service.calculer(stationA, List.of(stationA, stationB, stationC), Carburant.GAZOLE, 50);

        assertEquals(new BigDecimal("1.749"), r.prixStation());
        assertEquals(new BigDecimal("1.771"), r.prixMoyenLocal());
        assertEquals(50, r.litres());
        assertEquals(new BigDecimal("1.10"), r.montant());
    }

    @Test
    void stationPlusChereQueLaMoyenneDonneUnMontantNegatif() {
        Station chere = station("1", prix(Carburant.GAZOLE, 1.799));
        Station autre1 = station("2", prix(Carburant.GAZOLE, 1.749));
        Station autre2 = station("3", prix(Carburant.GAZOLE, 1.749));

        // moyenne = 5.297 / 3 = 1.76566… → 1.766 ; (1.766 − 1.799) × 40 = −1.32
        EconomieResponse r = service.calculer(chere, List.of(chere, autre1, autre2), Carburant.GAZOLE, 40);

        assertEquals(new BigDecimal("1.766"), r.prixMoyenLocal());
        assertEquals(new BigDecimal("-1.32"), r.montant());
    }

    @Test
    void seuleStationDeLaVilleDonneUneEconomieNulle() {
        EconomieResponse r = service.calculer(stationA, List.of(stationA), Carburant.GAZOLE, 50);

        assertEquals(new BigDecimal("1.749"), r.prixMoyenLocal());
        assertEquals(new BigDecimal("0.00"), r.montant());
    }

    @Test
    void listeNullOuVideSeComporteCommeUneVilleAvecUneSeuleStation() {
        assertEquals(new BigDecimal("0.00"), service.calculer(stationA, null, Carburant.GAZOLE, 50).montant());
        assertEquals(new BigDecimal("0.00"), service.calculer(stationA, List.of(), Carburant.GAZOLE, 50).montant());
    }

    @Test
    void stationAbsenteDeLaListeEstQuandMemeComptee() {
        EconomieResponse r = service.calculer(stationA, List.of(stationB, stationC), Carburant.GAZOLE, 50);

        assertEquals(new BigDecimal("1.771"), r.prixMoyenLocal());
        assertEquals(new BigDecimal("1.10"), r.montant());
    }

    @Test
    void stationEnDoubleDansLaListeNEstCompteeQuUneFois() {
        EconomieResponse r = service.calculer(stationA,
                List.of(stationA, stationB, stationA, stationC, stationB), Carburant.GAZOLE, 50);

        assertEquals(new BigDecimal("1.771"), r.prixMoyenLocal());
    }

    @Test
    void stationsEnRuptureSansPrixOuSansCeCarburantSontIgnorees() {
        Station enRupture = station("4", rupture(Carburant.GAZOLE, 1.500));
        Station sansPrix = station("5", PrixCarburant.builder().carburant(Carburant.GAZOLE).build());
        Station autreCarburant = station("6", prix(Carburant.SP95, 1.900));
        Station sansCarburant = station("7");

        EconomieResponse r = service.calculer(stationA, Arrays.asList(stationA, stationB, stationC,
                enRupture, sansPrix, autreCarburant, sansCarburant, null), Carburant.GAZOLE, 50);

        assertEquals(new BigDecimal("1.771"), r.prixMoyenLocal());
        assertEquals(new BigDecimal("1.10"), r.montant());
    }

    @Test
    void seulLePrixDuCarburantDemandeEstUtilise() {
        Station x = station("1", prix(Carburant.GAZOLE, 1.749), prix(Carburant.E10, 1.700));
        Station y = station("2", prix(Carburant.GAZOLE, 1.600), prix(Carburant.E10, 1.800));

        // E10 : moyenne (1.700 + 1.800) / 2 = 1.750 ; (1.750 − 1.700) × 20 = 1.00
        EconomieResponse r = service.calculer(x, List.of(x, y), Carburant.E10, 20);

        assertEquals(new BigDecimal("1.700"), r.prixStation());
        assertEquals(new BigDecimal("1.750"), r.prixMoyenLocal());
        assertEquals(new BigDecimal("1.00"), r.montant());
    }

    @Test
    void moyenneArrondieATroisDecimalesEtMontantAuCentime() {
        Station x = station("1", prix(Carburant.SP98, 1.7));
        Station y = station("2", prix(Carburant.SP98, 1.8));
        Station z = station("3", prix(Carburant.SP98, 1.8));

        // moyenne = 5.3 / 3 = 1.76666… → 1.767 ; (1.767 − 1.700) × 1 = 0.067 → 0.07
        EconomieResponse r = service.calculer(x, List.of(x, y, z), Carburant.SP98, 1);

        assertEquals(new BigDecimal("1.700"), r.prixStation());
        assertEquals(new BigDecimal("1.767"), r.prixMoyenLocal());
        assertEquals(new BigDecimal("0.07"), r.montant());
    }

    @Test
    void volumeDecimalAccepte() {
        // (1.771 − 1.749) × 42.5 = 0.935 → 0.94
        EconomieResponse r = service.calculer(stationA, List.of(stationA, stationB, stationC), Carburant.GAZOLE, 42.5);

        assertEquals(42.5, r.litres());
        assertEquals(new BigDecimal("0.94"), r.montant());
    }

    // ------------------------------------------------------------------
    // Erreurs (400 pour le frontend)
    // ------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(doubles = {0, -10, Double.NaN, Double.POSITIVE_INFINITY})
    void volumeNonStrictementPositifRefuse(double litres) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.calculer(stationA, List.of(stationA), Carburant.GAZOLE, litres));
        assertTrue(e.getMessage().contains("volume"));
    }

    @Test
    void stationQuiNeVendPasCeCarburantRefusee() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.calculer(stationA, List.of(stationA), Carburant.E85, 50));
        assertTrue(e.getMessage().contains("ne vend pas"));
    }

    @Test
    void carburantEnRuptureDansLaStationRefuse() {
        Station enRupture = station("4", rupture(Carburant.GAZOLE, 1.500));

        assertThrows(IllegalArgumentException.class,
                () -> service.calculer(enRupture, List.of(stationB, stationC), Carburant.GAZOLE, 50));
    }

    @Test
    void stationOuCarburantManquantRefuse() {
        assertThrows(IllegalArgumentException.class,
                () -> service.calculer(null, List.of(stationA), Carburant.GAZOLE, 50));
        assertThrows(IllegalArgumentException.class,
                () -> service.calculer(stationA, List.of(stationA), null, 50));
    }

    // ------------------------------------------------------------------
    // Outils de construction
    // ------------------------------------------------------------------

    private static Station station(String id, PrixCarburant... carburants) {
        return Station.builder().id(id).carburants(List.of(carburants)).build();
    }

    private static PrixCarburant prix(Carburant carburant, double prix) {
        return PrixCarburant.builder().carburant(carburant).prix(prix).build();
    }

    private static PrixCarburant rupture(Carburant carburant, double prix) {
        return PrixCarburant.builder()
                .carburant(carburant)
                .prix(prix)
                .disponible(false)
                .typeRupture(TypeRupture.TEMPORAIRE)
                .build();
    }
}

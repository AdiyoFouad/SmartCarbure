package fr.miage.smartcarbure.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fr.miage.smartcarbure.client.CarburantApiClient;
import fr.miage.smartcarbure.dto.EconomieResponse;
import fr.miage.smartcarbure.enums.Carburant;
import fr.miage.smartcarbure.enums.TypeRupture;
import fr.miage.smartcarbure.exception.StationIntrouvableException;
import fr.miage.smartcarbure.model.PrixCarburant;
import fr.miage.smartcarbure.model.Station;
import fr.miage.smartcarbure.model.Ville;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests du calcul d'économie sur un plein (F15). Aucun appel réseau : les stations sont construites à la main. */
class EconomieServiceTest {

    /** Faux client de l'API : aucun appel réseau, ses réponses sont fixées dans chaque test. */
    private final CarburantApiClient client = mock(CarburantApiClient.class);
    private final EconomieService service = new EconomieService(client);

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
    // Calcul à partir de l'identifiant (API simulée par Mockito)
    // ------------------------------------------------------------------

    @Test
    void calculerPourStationVaChercherLaStationPuisLesStationsDeSaVille() {
        Station choisie = stationDeRennes("1", 1.749);
        when(client.trouverParId("1")).thenReturn(Optional.of(choisie));
        when(client.rechercherParVille("Rennes"))
                .thenReturn(List.of(choisie, stationDeRennes("2", 1.779), stationDeRennes("3", 1.785)));

        EconomieResponse r = service.calculerPourStation("1", Carburant.GAZOLE, 50);

        assertEquals(new BigDecimal("1.749"), r.prixStation());
        assertEquals(new BigDecimal("1.771"), r.prixMoyenLocal());
        assertEquals(new BigDecimal("1.10"), r.montant());
    }

    @Test
    void stationInconnueLeveStationIntrouvableSansChercherLaVille() {
        when(client.trouverParId("99999999")).thenReturn(Optional.empty());

        StationIntrouvableException e = assertThrows(StationIntrouvableException.class,
                () -> service.calculerPourStation("99999999", Carburant.GAZOLE, 50));

        assertEquals("99999999", e.getIdStation());
        verify(client, never()).rechercherParVille(anyString());
    }

    @Test
    void stationSansVilleEstCompareeAElleSeule() {
        // stationA est construite sans ville
        when(client.trouverParId("1")).thenReturn(Optional.of(stationA));

        EconomieResponse r = service.calculerPourStation("1", Carburant.GAZOLE, 50);

        assertEquals(new BigDecimal("0.00"), r.montant());
        verify(client, never()).rechercherParVille(anyString());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -10})
    void volumeInvalideRefuseSansAppelerLApi(double litres) {
        assertThrows(IllegalArgumentException.class,
                () -> service.calculerPourStation("1", Carburant.GAZOLE, litres));

        verifyNoInteractions(client);
    }

    @Test
    void identifiantOuCarburantManquantRefuseSansAppelerLApi() {
        assertThrows(IllegalArgumentException.class,
                () -> service.calculerPourStation(null, Carburant.GAZOLE, 50));
        assertThrows(IllegalArgumentException.class,
                () -> service.calculerPourStation("  ", Carburant.GAZOLE, 50));
        assertThrows(IllegalArgumentException.class,
                () -> service.calculerPourStation("1", null, 50));

        verifyNoInteractions(client);
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

    private static Station stationDeRennes(String id, double prixGazole) {
        return Station.builder()
                .id(id)
                .ville(new Ville("Rennes", "35000", null))
                .carburants(List.of(prix(Carburant.GAZOLE, prixGazole)))
                .build();
    }
}

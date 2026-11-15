package fr.miage.smartcarbure.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.miage.smartcarbure.client.CarburantApiClient;
import fr.miage.smartcarbure.enums.Carburant;
import fr.miage.smartcarbure.exception.ApiIndisponibleException;
import fr.miage.smartcarbure.model.Position;
import fr.miage.smartcarbure.model.PrixCarburant;
import fr.miage.smartcarbure.model.Station;
import fr.miage.smartcarbure.model.StationSurTrajet;
import fr.miage.smartcarbure.model.Trajet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests de TrajetService. Le client de l'API des carburants est simulé avec Mockito :
 * aucun appel réseau n'est fait.
 *
 * <p>Le trajet de test va tout droit du nord au sud, le long de la longitude -1.6,
 * de la latitude 48.0 à 47.5 (environ 55,6 km).</p>
 */
class TrajetServiceTest {

    private static final double LONGITUDE_ROUTE = -1.6;
    /** Kilomètres par degré de longitude à la latitude 47.8. */
    private static final double KM_PAR_DEGRE_LONGITUDE = 111.195 * Math.cos(Math.toRadians(47.8));

    private CarburantApiClient client;
    private TrajetService service;
    private Trajet trajet;

    @BeforeEach
    void setUp() {
        client = mock(CarburantApiClient.class);
        service = new TrajetService(client, new CalculDistance());
        Position depart = new Position(48.0, LONGITUDE_ROUTE, null);
        Position arrivee = new Position(47.5, LONGITUDE_ROUTE, null);
        trajet = new Trajet(depart, arrivee, List.of(depart, arrivee), 55.6);
    }

    // ------------------------------------------------------------------
    // Sélection des stations
    // ------------------------------------------------------------------

    @Test
    void gardeLesStationsProchesDeLaRouteEtEcarteLesLointaines() {
        Station proche = station("1", 2.0, gazole(1.70, true));
        Station lointaine = station("2", 8.0, gazole(1.60, true));
        simulerApi(proche, lointaine);

        List<StationSurTrajet> resultats = service.rechercherLeLongDuTrajet(trajet, null, Carburant.GAZOLE);

        assertEquals(List.of("1"), ids(resultats));
    }

    @Test
    void calculeLaDistanceDeLaStationALaRoute() {
        simulerApi(station("1", 2.0, gazole(1.70, true)));

        List<StationSurTrajet> resultats = service.rechercherLeLongDuTrajet(trajet, null, Carburant.GAZOLE);

        assertEquals(2.0, resultats.get(0).distanceKm(), 0.05);
    }

    @Test
    void ecarteLesStationsQuiNeVendentPasLeCarburantDemande() {
        Station sansGazole = station("1", 1.0, prix(Carburant.SP95, 1.80, true));
        Station avecGazole = station("2", 1.0, gazole(1.70, true));
        simulerApi(sansGazole, avecGazole);

        List<StationSurTrajet> resultats = service.rechercherLeLongDuTrajet(trajet, null, Carburant.GAZOLE);

        assertEquals(List.of("2"), ids(resultats));
    }

    @Test
    void ignoreLesStationsSansCoordonnees() {
        Station sansCoordonnees = Station.builder().id("1")
                .position(new Position(null, null, "1 rue inconnue"))
                .carburants(List.of(gazole(1.70, true))).build();
        Station sansPosition = Station.builder().id("2").carburants(List.of(gazole(1.70, true))).build();
        simulerApi(sansCoordonnees, sansPosition);

        assertTrue(service.rechercherLeLongDuTrajet(trajet, null, Carburant.GAZOLE).isEmpty());
    }

    @Test
    void renvoieUneListeVideSiAucuneStation() {
        simulerApi();

        assertTrue(service.rechercherLeLongDuTrajet(trajet, null, Carburant.GAZOLE).isEmpty());
    }

    // ------------------------------------------------------------------
    // Tri
    // ------------------------------------------------------------------

    @Test
    void trieParPrixCroissantEtPlaceLesRupturesALaFin() {
        Station chere = station("chere", 1.0, gazole(1.90, true));
        Station rupture = station("rupture", 0.5, gazole(1.50, false));
        Station moinsChere = station("moins-chere", 3.0, gazole(1.65, true));
        Station moyenne = station("moyenne", 2.0, gazole(1.75, true));
        simulerApi(chere, rupture, moinsChere, moyenne);

        List<StationSurTrajet> resultats = service.rechercherLeLongDuTrajet(trajet, null, Carburant.GAZOLE);

        assertEquals(List.of("moins-chere", "moyenne", "chere", "rupture"), ids(resultats));
    }

    @Test
    void aPrixEgalLaStationLaPlusProcheDeLaRoutePasseDevant() {
        Station loin = station("loin", 4.0, gazole(1.70, true));
        Station pres = station("pres", 1.0, gazole(1.70, true));
        simulerApi(loin, pres);

        List<StationSurTrajet> resultats = service.rechercherLeLongDuTrajet(trajet, null, Carburant.GAZOLE);

        assertEquals(List.of("pres", "loin"), ids(resultats));
    }

    @Test
    void uneStationSansPrixConnuEstTraiteeCommeUneRupture() {
        Station sansPrix = station("sans-prix", 1.0, gazole(null, true));
        Station avecPrix = station("avec-prix", 2.0, gazole(1.80, true));
        simulerApi(sansPrix, avecPrix);

        List<StationSurTrajet> resultats = service.rechercherLeLongDuTrajet(trajet, null, Carburant.GAZOLE);

        assertEquals(List.of("avec-prix", "sans-prix"), ids(resultats));
    }

    // ------------------------------------------------------------------
    // Rayon (distance maximale à la route)
    // ------------------------------------------------------------------

    @Test
    void utiliseCinqKilometresParDefaut() {
        simulerApi();

        service.rechercherLeLongDuTrajet(trajet, null, Carburant.GAZOLE);

        verify(client).rechercherAutourDePoints(anyList(), eq(TrajetService.rayonRecherche(5.0)));
    }

    @Test
    void unRayonPlusGrandGardeDesStationsPlusEloignees() {
        simulerApi(station("1", 8.0, gazole(1.70, true)));

        assertEquals(1, service.rechercherLeLongDuTrajet(trajet, 10.0, Carburant.GAZOLE).size());
    }

    @Test
    void refuseUnRayonHorsLimitesSansAppelerLApi() {
        assertThrows(IllegalArgumentException.class,
                () -> service.rechercherLeLongDuTrajet(trajet, 0.5, Carburant.GAZOLE));
        assertThrows(IllegalArgumentException.class,
                () -> service.rechercherLeLongDuTrajet(trajet, 11.0, Carburant.GAZOLE));
        verify(client, never()).rechercherAutourDePoints(anyList(), anyDouble());
    }

    @Test
    void refuseUnTrajetDePlusDe500Kilometres() {
        Trajet tropLong = new Trajet(trajet.getDepart(), trajet.getArrivee(), trajet.getPoints(), 501);

        assertThrows(IllegalArgumentException.class,
                () -> service.rechercherLeLongDuTrajet(tropLong, null, Carburant.GAZOLE));
        verify(client, never()).rechercherAutourDePoints(anyList(), anyDouble());
    }

    @Test
    void refuseUnTrajetOuUnCarburantNull() {
        assertThrows(NullPointerException.class,
                () -> service.rechercherLeLongDuTrajet(null, null, Carburant.GAZOLE));
        assertThrows(NullPointerException.class,
                () -> service.rechercherLeLongDuTrajet(trajet, null, null));
    }

    // ------------------------------------------------------------------
    // Points le long de la route et virages
    // ------------------------------------------------------------------

    @Test
    void placeDesPointsRegulierementEspacesDuDepartALArrivee() {
        List<Position> points = service.echantillonner(trajet.getPoints(), TrajetService.ESPACEMENT_POINTS_KM);
        CalculDistance calcul = new CalculDistance();

        assertEquals(trajet.getDepart(), points.get(0));
        assertEquals(trajet.getArrivee(), points.get(points.size() - 1));
        for (int i = 1; i < points.size(); i++) {
            assertTrue(calcul.calculerDistance(points.get(i - 1), points.get(i)) <= TrajetService.ESPACEMENT_POINTS_KM + 0.01);
        }
    }

    @Test
    void placeDesPointsSurUneRouteFaiteDeNombreuxPetitsSegments() {
        List<Position> route = new java.util.ArrayList<>();
        for (int i = 0; i <= 100; i++) {
            route.add(new Position(48.0 - i * 0.005, LONGITUDE_ROUTE, null)); // segments d'environ 0,56 km
        }
        List<Position> points = service.echantillonner(route, TrajetService.ESPACEMENT_POINTS_KM);

        // 55,6 km / 3 km : le départ, 18 points intermédiaires, puis l'arrivée
        assertEquals(20, points.size());
    }

    @Test
    void trouveUneStationProcheDUnVirage() {
        // Route en angle droit : vers le sud puis vers l'est.
        Position depart = new Position(48.0, LONGITUDE_ROUTE, null);
        Position virage = new Position(47.9, LONGITUDE_ROUTE, null);
        Position arrivee = new Position(47.9, LONGITUDE_ROUTE + 0.2, null);
        Trajet enAngle = new Trajet(depart, arrivee, List.of(depart, virage, arrivee), 26);
        // Station à l'extérieur du virage, à 3 km au sud-ouest de l'angle.
        Station station = Station.builder().id("virage")
                .position(new Position(47.9 - 2.12 / 111.195, LONGITUDE_ROUTE - 2.12 / KM_PAR_DEGRE_LONGITUDE, null))
                .carburants(List.of(gazole(1.70, true))).build();
        simulerApi(station);

        List<StationSurTrajet> resultats = service.rechercherLeLongDuTrajet(enAngle, null, Carburant.GAZOLE);

        assertEquals(1, resultats.size());
        assertEquals(3.0, resultats.get(0).distanceKm(), 0.1);
    }

    @Test
    void lesCerclesDeRechercheCouvrentToutLeCouloir() {
        // Le pire cas est une station à mi-chemin entre deux points, au bord du couloir.
        double rayon = 5.0;
        double pire = Math.hypot(TrajetService.ESPACEMENT_POINTS_KM / 2, rayon);
        assertTrue(TrajetService.rayonRecherche(rayon) >= pire);
    }

    @Test
    void distanceAuSegmentPrendLExtremiteLaPlusProcheQuandLeSegmentEstLoin() {
        Position a = new Position(48.0, LONGITUDE_ROUTE, null);
        Position b = new Position(47.99, LONGITUDE_ROUTE, null);
        Position auNord = new Position(48.0 + 1 / 111.195, LONGITUDE_ROUTE, null);

        assertEquals(1.0, TrajetService.distanceAuSegment(auNord, a, b), 0.01);
    }

    // ------------------------------------------------------------------
    // Tronçons (plafond de 500 stations par recherche)
    // ------------------------------------------------------------------

    @Test
    void unTrajetCourtNeDemandeQuUneRecherche() {
        simulerApi();

        service.rechercherLeLongDuTrajet(trajet, null, Carburant.GAZOLE);

        verify(client, times(1)).rechercherAutourDePoints(anyList(), anyDouble());
    }

    @Test
    void unTrajetLongEstCherchePartronconsDAuPlus20Points() {
        Trajet long222km = trajetVersLeSud(2.0, 222.4);
        simulerApi();

        service.rechercherLeLongDuTrajet(long222km, null, Carburant.GAZOLE);

        // 222 km tous les 3 km : 76 points, donc 4 tronçons de 20 points au plus
        verify(client, times(4)).rechercherAutourDePoints(anyList(), anyDouble());
    }

    @Test
    void uneStationTrouveeDansPlusieursTronconsNApparaitQuUneFois() {
        Trajet long222km = trajetVersLeSud(2.0, 222.4);
        // Station à 1 km de la route, au milieu du trajet, renvoyée par chaque recherche.
        Station station = Station.builder().id("partout")
                .position(new Position(47.0, LONGITUDE_ROUTE + 1.0 / (111.195 * Math.cos(Math.toRadians(47.0))), null))
                .carburants(List.of(gazole(1.70, true))).build();
        simulerApi(station);

        List<StationSurTrajet> resultats = service.rechercherLeLongDuTrajet(long222km, null, Carburant.GAZOLE);

        assertEquals(List.of("partout"), ids(resultats));
    }

    @Test
    void decouperEnTronconsGardeTousLesPointsDansLOrdre() {
        List<Position> points = new java.util.ArrayList<>();
        for (int i = 0; i < 45; i++) {
            points.add(new Position(48.0 - i * 0.01, LONGITUDE_ROUTE, null));
        }

        List<List<Position>> troncons = TrajetService.decouperEnTroncons(points, 20);

        assertEquals(3, troncons.size());
        assertEquals(20, troncons.get(0).size());
        assertEquals(5, troncons.get(2).size());
        assertEquals(points.get(20), troncons.get(1).get(0));
    }

    // ------------------------------------------------------------------
    // Panne de l'API
    // ------------------------------------------------------------------

    @Test
    void laPanneDeLApiDesCarburantsEstTransmise() {
        when(client.rechercherAutourDePoints(anyList(), anyDouble()))
                .thenThrow(new ApiIndisponibleException("API indisponible"));

        assertThrows(ApiIndisponibleException.class,
                () -> service.rechercherLeLongDuTrajet(trajet, null, Carburant.GAZOLE));
    }

    // ------------------------------------------------------------------
    // Outils
    // ------------------------------------------------------------------

    /** Trajet tout droit vers le sud, de la latitude 48.0 à 48.0 - degres. */
    private static Trajet trajetVersLeSud(double degres, double distanceKm) {
        Position depart = new Position(48.0, LONGITUDE_ROUTE, null);
        Position arrivee = new Position(48.0 - degres, LONGITUDE_ROUTE, null);
        return new Trajet(depart, arrivee, List.of(depart, arrivee), distanceKm);
    }

    private void simulerApi(Station... stations) {
        when(client.rechercherAutourDePoints(anyList(), anyDouble())).thenReturn(List.of(stations));
    }

    /** Station située à mi-parcours, à distanceKm à l'est de la route. */
    private static Station station(String id, double distanceKm, PrixCarburant... carburants) {
        double longitude = LONGITUDE_ROUTE + distanceKm / KM_PAR_DEGRE_LONGITUDE;
        return Station.builder().id(id)
                .position(new Position(47.8, longitude, "Adresse " + id))
                .carburants(List.of(carburants))
                .build();
    }

    private static PrixCarburant gazole(Double prix, boolean disponible) {
        return prix(Carburant.GAZOLE, prix, disponible);
    }

    private static PrixCarburant prix(Carburant carburant, Double prix, boolean disponible) {
        return PrixCarburant.builder().carburant(carburant).prix(prix).disponible(disponible).build();
    }

    private static List<String> ids(List<StationSurTrajet> resultats) {
        return resultats.stream().map(s -> s.station().getId()).toList();
    }

    @Test
    void lOutilStationPlaceBienLaStationALaDistanceVoulue() {
        assertEquals(2.0, TrajetService.distanceALaRoute(station("x", 2).getPosition(), trajet.getPoints()), 0.05);
    }
}

package fr.miage.smartcarbure.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.miage.smartcarbure.model.Carburant;
import fr.miage.smartcarbure.model.Horaire;
import fr.miage.smartcarbure.model.PrixCarburant;
import fr.miage.smartcarbure.model.Service;
import fr.miage.smartcarbure.model.Station;
import fr.miage.smartcarbure.model.TypeEmplacement;
import fr.miage.smartcarbure.model.TypeRupture;
import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Tests de la conversion du JSON de l'API Prix des carburants (fichiers réels dans src/test/resources). */
class StationJsonConverterTest {

    private static final double DELTA = 1e-9;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static List<Station> stationsRennes;
    private static List<Station> stationsAutoroute;

    private StationJsonConverter converter;

    @BeforeAll
    static void chargerFichiers() throws IOException {
        StationJsonConverter converter = new StationJsonConverter();
        stationsRennes = converter.convertirListe(lireFichier("/api-carburants-exemple.json"));
        stationsAutoroute = converter.convertirListe(lireFichier("/api-carburants-autoroute.json"));
    }

    @BeforeEach
    void creerConverter() {
        converter = new StationJsonConverter();
    }

    private static JsonNode lireFichier(String chemin) throws IOException {
        try (InputStream flux = StationJsonConverterTest.class.getResourceAsStream(chemin)) {
            assertNotNull(flux, "Fichier introuvable : " + chemin);
            return JSON.readTree(flux);
        }
    }

    private static JsonNode json(String texte) {
        return JSON.readTree(texte);
    }

    private static Station station(String id) {
        return stationsRennes.stream()
                .filter(s -> id.equals(s.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Station absente : " + id));
    }

    private static PrixCarburant carburant(Station station, Carburant type) {
        return station.getCarburants().stream()
                .filter(c -> c.getCarburant() == type)
                .findFirst()
                .orElseThrow(() -> new AssertionError(type + " absent de la station " + station.getId()));
    }

    private static Horaire horaire(Station station, DayOfWeek jour) {
        return station.getHoraires().stream()
                .filter(h -> h.getJour() == jour)
                .findFirst()
                .orElseThrow(() -> new AssertionError(jour + " absent de la station " + station.getId()));
    }

    // 1
    @Test
    void leFichierExempleDonneCinqStations() {
        assertEquals(5, stationsRennes.size());
    }

    // 2
    @Test
    void identiteEtRattachementGeographique() {
        Station s = station("35200008");

        assertNull(s.getNom());
        assertEquals("7 boulevard de l'yser", s.getPosition().getAdresse());
        assertEquals("Rennes", s.getVille().getNom());
        assertEquals("35200", s.getVille().getCodePostal());
        assertEquals("35", s.getVille().getDepartement().getCode());
        assertEquals("Ille-et-Vilaine", s.getVille().getDepartement().getNom());
        assertEquals("53", s.getVille().getDepartement().getRegion().getCode());
        assertEquals("Bretagne", s.getVille().getDepartement().getRegion().getNom());
        // Raccourcis qui passent par la ville
        assertEquals("35", s.getDepartement().getCode());
        assertEquals("Bretagne", s.getRegion().getNom());
    }

    @Test
    void raccourcisGeographiquesNullSiUnMaillonManque() {
        Station sansVille = converter.convertir(json("{\"id\": 1}"));
        assertNull(sansVille.getVille());
        assertNull(sansVille.getDepartement());
        assertNull(sansVille.getRegion());

        Station sansRegion = converter.convertir(json("{\"ville\": \"Rennes\", \"code_departement\": \"35\"}"));
        assertEquals("35", sansRegion.getDepartement().getCode());
        assertNull(sansRegion.getRegion());
    }

    // 3
    @Test
    void positionLueDansGeom() {
        Station s = station("35200008");
        assertEquals(48.092, s.getPosition().getLatitude(), DELTA);
        assertEquals(-1.68, s.getPosition().getLongitude(), DELTA);
    }

    // 4
    @Test
    void positionCalculeeDepuisLatitudeLongitudeSiGeomAbsent() {
        Station s = converter.convertir(json(
                "{\"latitude\": \"4809890.2183757\", \"longitude\": \"-165400.71882227\", \"adresse\": \"19, BD LEON BOURGEOIS\"}"));

        assertEquals(48.098902183757, s.getPosition().getLatitude(), 1e-6);
        assertEquals(-1.6540071882227, s.getPosition().getLongitude(), 1e-6);
        assertEquals("19, BD LEON BOURGEOIS", s.getPosition().getAdresse());
    }

    @Test
    void positionAvecAdresseSansCoordonnees() {
        Station s = converter.convertir(json("{\"adresse\": \"Rue X\", \"geom\": null}"));

        assertNotNull(s.getPosition());
        assertEquals("Rue X", s.getPosition().getAdresse());
        assertNull(s.getPosition().getLatitude());
        assertNull(s.getPosition().getLongitude());
    }

    @Test
    void positionAvecCoordonneesSansAdresse() {
        Station s = converter.convertir(json("{\"geom\": {\"lat\": 48.1, \"lon\": -1.6}}"));

        assertEquals(48.1, s.getPosition().getLatitude(), DELTA);
        assertEquals(-1.6, s.getPosition().getLongitude(), DELTA);
        assertNull(s.getPosition().getAdresse());
    }

    @Test
    void positionNullSansAdresseNiCoordonnees() {
        assertNull(converter.convertir(json("{\"ville\": \"Rennes\", \"geom\": {\"lat\": 48.1}}")).getPosition());
    }

    // 5
    @Test
    void emplacementRouteAutorouteOuNull() {
        assertEquals(TypeEmplacement.ROUTE, station("35200008").getEmplacement());

        assertEquals(2, stationsAutoroute.size());
        stationsAutoroute.forEach(s -> assertEquals(TypeEmplacement.AUTOROUTE, s.getEmplacement()));

        assertNull(converter.convertir(json("{\"pop\": \"X\"}")).getEmplacement());
    }

    // 6
    @Test
    void automateOuiNonOuAbsent() {
        assertTrue(station("35000013").isAutomate24h());
        assertFalse(station("35200008").isAutomate24h());
        assertFalse(converter.convertir(json("{}")).isAutomate24h());
    }

    // 7
    @Test
    void carburantsDisponiblesEtEnRupture() {
        Station s = station("35200008");
        assertEquals(6, s.getCarburants().size());

        PrixCarburant gazole = carburant(s, Carburant.GAZOLE);
        assertEquals(2.159, gazole.getPrix(), DELTA);
        assertEquals(LocalDateTime.of(2026, 11, 4, 8, 56, 1), gazole.getDateMaj());
        assertTrue(gazole.isDisponible());
        assertNull(gazole.getTypeRupture());
        assertNull(gazole.getDebutRupture());

        PrixCarburant sp95 = carburant(s, Carburant.SP95);
        assertFalse(sp95.isDisponible());
        assertEquals(TypeRupture.DEFINITIVE, sp95.getTypeRupture());
        assertEquals(LocalDateTime.of(2026, 8, 17, 7, 33, 47), sp95.getDebutRupture());
        assertNull(sp95.getPrix());
    }

    // 8
    @Test
    void ruptureTemporaireSansPrix() {
        PrixCarburant gazole = carburant(station("35000013"), Carburant.GAZOLE);

        assertFalse(gazole.isDisponible());
        assertEquals(TypeRupture.TEMPORAIRE, gazole.getTypeRupture());
        assertEquals(LocalDateTime.of(2026, 10, 26, 14, 30, 47), gazole.getDebutRupture());
        assertNull(gazole.getPrix());
        assertNull(gazole.getDateMaj());
    }

    // 9
    @Test
    void gplcConvertiEnGplc() {
        PrixCarburant gplc = carburant(station("35000012"), Carburant.GPLC);
        assertEquals(0.932, gplc.getPrix(), DELTA);
        assertTrue(gplc.isDisponible());
    }

    @Test
    void enumCarburantDepuisNomApi() {
        assertEquals(Carburant.GAZOLE, Carburant.depuisNomApi("Gazole"));
        assertEquals(Carburant.GPLC, Carburant.depuisNomApi("GPLc"));
        assertEquals("GPLc", Carburant.GPLC.getLibelle());
        assertNull(Carburant.depuisNomApi("Kérosène"));
        assertNull(Carburant.depuisNomApi(null));
    }

    // 10
    @Test
    void carburantSansPrixNiRuptureAbsent() {
        Station s = converter.convertir(json("{\"gazole_prix\": 1.5, \"sp98_prix\": null, \"e85_maj\": null}"));

        assertEquals(1, s.getCarburants().size());
        assertEquals(Carburant.GAZOLE, s.getCarburants().get(0).getCarburant());
    }

    // 11
    @Test
    void servicesPlusieursOuUnSeul() {
        List<String> plusieurs = station("35000013").getServices().stream().map(Service::getNom).toList();
        assertEquals(List.of("Lavage manuel", "Station de gonflage", "Automate CB 24/24"), plusieurs);

        List<Service> unSeul = station("35000012").getServices();
        assertEquals(1, unSeul.size());
        assertEquals("Automate CB 24/24", unSeul.get(0).getNom());

        assertTrue(converter.convertir(json("{\"services_service\": null}")).getServices().isEmpty());
    }

    // 12
    @Test
    void horairesSeptJours() {
        Station s = station("35000025");
        assertEquals(7, s.getHoraires().size());

        Horaire lundi = horaire(s, DayOfWeek.MONDAY);
        assertFalse(lundi.isFerme());
        assertEquals(LocalTime.of(7, 0), lundi.getOuverture());
        assertEquals(LocalTime.of(22, 0), lundi.getFermeture());
        assertEquals(LocalTime.of(22, 0), horaire(s, DayOfWeek.SUNDAY).getFermeture());
    }

    @Test
    void horairesJourFerme() {
        // Station 35700001 : @ferme = "1" tous les jours (plage 01.00-01.00 ignorée)
        Station s = station("35700001");
        assertEquals(7, s.getHoraires().size());

        Horaire lundi = horaire(s, DayOfWeek.MONDAY);
        assertTrue(lundi.isFerme());
        assertNull(lundi.getOuverture());
        assertNull(lundi.getFermeture());
    }

    @Test
    void horairesJourInconnu() {
        // Station 35200008 : jours non marqués fermés mais sans aucune plage horaire
        Station s = station("35200008");
        assertEquals(7, s.getHoraires().size());

        Horaire lundi = horaire(s, DayOfWeek.MONDAY);
        assertFalse(lundi.isFerme());
        assertNull(lundi.getOuverture());
        assertNull(lundi.getFermeture());
    }

    @Test
    void horairesPlusieursPlagesPremiereOuvertureDerniereFermeture() {
        String horaires = "{\\\"jour\\\": {\\\"@id\\\": \\\"3\\\", \\\"@nom\\\": \\\"Mercredi\\\", \\\"@ferme\\\": \\\"\\\", "
                + "\\\"horaire\\\": [{\\\"@ouverture\\\": \\\"08.00\\\", \\\"@fermeture\\\": \\\"12.00\\\"}, "
                + "{\\\"@ouverture\\\": \\\"14.00\\\", \\\"@fermeture\\\": \\\"19.30\\\"}]}}";
        Station s = converter.convertir(json("{\"horaires\": \"" + horaires + "\"}"));

        assertEquals(1, s.getHoraires().size());
        Horaire mercredi = s.getHoraires().get(0);
        assertEquals(DayOfWeek.WEDNESDAY, mercredi.getJour());
        assertFalse(mercredi.isFerme());
        assertEquals(LocalTime.of(8, 0), mercredi.getOuverture());
        assertEquals(LocalTime.of(19, 30), mercredi.getFermeture());
    }

    @Test
    void horairesNullDonneListeVide() {
        assertTrue(converter.convertir(json("{\"horaires\": null}")).getHoraires().isEmpty());
    }

    // 13
    @Test
    void objetVideDonneStationVide() {
        Station s = converter.convertir(json("{}"));

        assertNotNull(s);
        assertNull(s.getId());
        assertNull(s.getNom());
        assertNull(s.getPosition());
        assertNull(s.getVille());
        assertNull(s.getEmplacement());
        assertTrue(s.getHoraires().isEmpty());
        assertTrue(s.getServices().isEmpty());
        assertTrue(s.getCarburants().isEmpty());
    }

    @Test
    void convertirNullDonneStationVide() {
        assertNotNull(converter.convertir(null));
    }

    // 14
    @Test
    void valeursMalFormeesSansException() {
        Station s = converter.convertir(json("{"
                + "\"id\": 42,"
                + "\"geom\": {\"lat\": \"abc\", \"lon\": null},"
                + "\"latitude\": \"pas un nombre\", \"longitude\": \"-165400\","
                + "\"pop\": 12,"
                + "\"horaires_automate_24_24\": [\"Oui\"],"
                + "\"horaires\": \"{pas du json\","
                + "\"services_service\": {\"a\": 1},"
                + "\"gazole_prix\": \"abc\", \"gazole_maj\": \"pas une date\","
                + "\"sp95_prix\": \"1.899\", \"sp95_maj\": \"2026-11-04 08:56:01\","
                + "\"e10_rupture_type\": \"inconnu\""
                + "}"));

        assertEquals("42", s.getId());
        assertNull(s.getPosition());
        assertNull(s.getEmplacement());
        assertFalse(s.isAutomate24h());
        assertTrue(s.getHoraires().isEmpty());
        assertTrue(s.getServices().isEmpty());

        // Gazole : ni prix lisible ni rupture → absent
        assertTrue(s.getCarburants().stream().noneMatch(c -> c.getCarburant() == Carburant.GAZOLE));
        // SP95 : prix en chaîne et date au format local acceptés
        PrixCarburant sp95 = carburant(s, Carburant.SP95);
        assertEquals(1.899, sp95.getPrix(), DELTA);
        assertEquals(LocalDateTime.of(2026, 11, 4, 8, 56, 1), sp95.getDateMaj());
        // E10 : type de rupture inconnu → en rupture, type null
        PrixCarburant e10 = carburant(s, Carburant.E10);
        assertFalse(e10.isDisponible());
        assertNull(e10.getTypeRupture());
    }

    // 15
    @Test
    void reponseSansResultatsDonneListeVide() {
        assertTrue(converter.convertirListe(null).isEmpty());
        assertTrue(converter.convertirListe(json("{\"total_count\": 0}")).isEmpty());
        assertTrue(converter.convertirListe(json("{\"results\": \"pas un tableau\"}")).isEmpty());
        assertTrue(converter.convertirListe(json("{\"results\": []}")).isEmpty());
    }

    @Test
    void elementsNonObjetsIgnores() {
        List<Station> stations = converter.convertirListe(json("{\"results\": [null, 3, {\"id\": 7}]}"));
        assertEquals(1, stations.size());
        assertEquals("7", stations.get(0).getId());
    }
}

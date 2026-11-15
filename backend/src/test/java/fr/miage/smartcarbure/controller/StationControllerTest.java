package fr.miage.smartcarbure.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.miage.smartcarbure.client.CarburantApiClient;
import fr.miage.smartcarbure.client.StationJsonConverter;
import fr.miage.smartcarbure.exception.ApiIndisponibleException;
import fr.miage.smartcarbure.exception.GlobalExceptionHandler;
import fr.miage.smartcarbure.mapper.DepartementMapper;
import fr.miage.smartcarbure.mapper.HoraireMapper;
import fr.miage.smartcarbure.mapper.PositionMapper;
import fr.miage.smartcarbure.mapper.PrixCarburantMapper;
import fr.miage.smartcarbure.mapper.RegionMapper;
import fr.miage.smartcarbure.mapper.StationMapper;
import fr.miage.smartcarbure.mapper.VilleMapper;
import fr.miage.smartcarbure.model.Station;
import fr.miage.smartcarbure.service.StationService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tests de GET /api/stations/{id} sans réseau ni base de données : MockMvc en standaloneSetup avec
 * GlobalExceptionHandler, le vrai StationService et le vrai StationMapper ; seul CarburantApiClient est simulé.
 * La station renvoyée est la station 35000025 du fichier réel api-carburants-exemple.json.
 */
class StationControllerTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String ID = "35000025";

    private CarburantApiClient client;
    private MockMvc mockMvc;

    @BeforeEach
    void creerMockMvc() {
        client = mock(CarburantApiClient.class);
        StationMapper mapper = new StationMapper(new PositionMapper(),
                new VilleMapper(new DepartementMapper(new RegionMapper())),
                new HoraireMapper(), new PrixCarburantMapper());
        mockMvc = MockMvcBuilders
                .standaloneSetup(new StationController(new StationService(client, mapper)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /** Station 35000025 lue dans le fichier d'exemple avec le vrai StationJsonConverter. */
    private static Station stationExemple() throws IOException {
        try (InputStream flux = StationControllerTest.class.getResourceAsStream("/api-carburants-exemple.json")) {
            List<Station> stations = new StationJsonConverter().convertirListe(JSON.readTree(flux));
            return stations.stream().filter(s -> ID.equals(s.getId())).findFirst().orElseThrow();
        }
    }

    /** Vérifie le code HTTP et compare le corps à l'arbre JSON attendu (l'ordre des champs est ignoré). */
    private void verifier(RequestBuilder requete, int statut, String jsonAttendu) throws Exception {
        String corps = mockMvc.perform(requete).andExpect(status().is(statut)).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode attendu = JSON.readTree(jsonAttendu);
        assertEquals(attendu, JSON.readTree(corps), "Corps de la réponse : " + corps);
    }

    /** Sept jours de 07:00 à 22:00, au format donné. */
    private static String horaires(String ouverture, String fermeture, boolean avecChampFerme) {
        return List.of("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche").stream()
                .map(jour -> "{ \"jour\": \"" + jour + "\", "
                        + (avecChampFerme ? "\"ferme\": false, " : "")
                        + "\"ouverture\": \"" + ouverture + "\", \"fermeture\": \"" + fermeture + "\" }")
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private static final String CARBURANTS = """
            [
              { "type": "GAZOLE", "prix": 2.25, "dateMaj": "2026-10-28T20:40:57Z", "disponible": true,
                "typeRupture": null, "debutRupture": null },
              { "type": "SP95", "prix": null, "dateMaj": null, "disponible": false,
                "typeRupture": "DEFINITIVE", "debutRupture": "2026-03-27T17:43:44Z" },
              { "type": "SP98", "prix": null, "dateMaj": null, "disponible": false,
                "typeRupture": "TEMPORAIRE", "debutRupture": "2026-11-01T18:12:13Z" },
              { "type": "E10", "prix": null, "dateMaj": null, "disponible": false,
                "typeRupture": "TEMPORAIRE", "debutRupture": "2026-11-01T18:12:13Z" },
              { "type": "E85", "prix": null, "dateMaj": null, "disponible": false,
                "typeRupture": "DEFINITIVE", "debutRupture": "2026-03-27T17:43:44Z" },
              { "type": "GPLC", "prix": null, "dateMaj": null, "disponible": false,
                "typeRupture": "DEFINITIVE", "debutRupture": "2026-03-27T17:43:44Z" }
            ]""";

    /**
     * Format du CONTRAT (docs/contrat-api.md) : "nom": null et "distanceKm": null présents,
     * heures "HH:mm", champ "ferme" dans chaque horaire.
     */
    @Test
    void ficheAuFormatDuContrat() throws Exception {
        when(client.trouverParId(ID)).thenReturn(Optional.of(stationExemple()));

        verifier(get("/api/stations/" + ID), 200, """
                {
                  "id": "35000025",
                  "nom": null,
                  "adresse": "19, BD LEON BOURGEOIS",
                  "ville": "Rennes",
                  "codePostal": "35000",
                  "departement": { "code": "35", "nom": "Ille-et-Vilaine" },
                  "region": { "code": "53", "nom": "Bretagne" },
                  "position": { "latitude": 48.098902183757, "longitude": -1.6540071882226999 },
                  "emplacement": "ROUTE",
                  "automate24h": true,
                  "horaires": %s,
                  "services": ["Automate CB 24/24", "Piste poids lourds", "Relais colis"],
                  "carburants": %s,
                  "distanceKm": null
                }""".formatted(horaires("07:00", "22:00", true), CARBURANTS));
    }

    @Test
    void stationInconnueDonne404() throws Exception {
        when(client.trouverParId("99999999")).thenReturn(Optional.empty());

        verifier(get("/api/stations/99999999"), 404,
                "{ \"statut\": 404, \"message\": \"Station introuvable : 99999999.\" }");
    }

    @Test
    void idNonNumeriqueDonne404() throws Exception {
        when(client.trouverParId("abc")).thenReturn(Optional.empty());

        verifier(get("/api/stations/abc"), 404,
                "{ \"statut\": 404, \"message\": \"Station introuvable : abc.\" }");
    }

    @Test
    void apiEnPanneDonne503() throws Exception {
        when(client.trouverParId(ID)).thenThrow(new ApiIndisponibleException("L'API Prix des carburants ne répond pas."));

        verifier(get("/api/stations/" + ID), 503,
                "{ \"statut\": 503, \"message\": \"Le service Prix des carburants est temporairement indisponible. "
                        + "Réessayez dans quelques instants.\" }");
    }

    @Test
    void deleteDonne405() throws Exception {
        verifier(delete("/api/stations/" + ID), 405,
                "{ \"statut\": 405, \"message\": \"Méthode DELETE non autorisée pour cette route.\" }");
    }
}

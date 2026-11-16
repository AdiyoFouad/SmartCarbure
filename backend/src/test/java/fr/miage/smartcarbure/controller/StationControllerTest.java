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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import fr.miage.smartcarbure.dto.StationDTO;
import fr.miage.smartcarbure.enums.Carburant;
import fr.miage.smartcarbure.enums.TypeTri;
import fr.miage.smartcarbure.exception.GlobalExceptionHandler;
import fr.miage.smartcarbure.service.RechercheService;

/**
 * Tests de GET /api/stations/{id} sans réseau ni base de données : MockMvc en standaloneSetup avec
 * GlobalExceptionHandler, le vrai StationService et le vrai StationMapper ; seul CarburantApiClient est simulé.
 * La station renvoyée est la station 35000025 du fichier réel api-carburants-exemple.json.
 */
class StationControllerTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String ID = "35000025";

    private CarburantApiClient client;
	private final RechercheService service = mock(RechercheService.class);
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



	@Test
	void rechercheAutourDunePositionTransmetLesCriteresEtLeTriParDefaut() throws Exception {
		StationDTO station = new StationDTO();
		station.setDistanceKm(3.6);
		when(service.rechercher(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(station));

		mockMvc.perform(get("/api/stations")
						.param("lat", "48.1173")
						.param("lon", "-1.6778")
						.param("rayon", "10")
						.param("carburant", "GAZOLE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].distanceKm").value(3.6));

		verify(service).rechercher(argThat(criteres ->
				criteres.getLat() == 48.1173
						&& criteres.getLon() == -1.6778
						&& criteres.getRayon() == 10
						&& criteres.getCarburant() == Carburant.GAZOLE
						&& criteres.getTri() == TypeTri.PRIX));
	}

	@Test
	void rechercheParVilleTransmetLesParametresF2() throws Exception {
		when(service.rechercher(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(new StationDTO()));

		mockMvc.perform(get("/api/stations")
						.param("ville", "Rennes")
						.param("carburant", "E10")
						.param("tri", "DISTANCE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].distanceKm").value(nullValue()));

		verify(service).rechercher(argThat(criteres ->
				"Rennes".equals(criteres.getVille())
						&& criteres.getCarburant() == Carburant.E10
						&& criteres.getTri() == TypeTri.DISTANCE));
	}

	@Test
	void parametresObligatoiresEtBornesSontValidesParLeService() throws Exception {
		when(service.rechercher(org.mockito.ArgumentMatchers.any()))
				.thenThrow(new IllegalArgumentException("Le rayon doit être compris entre 1 et 50 km."));

		mockMvc.perform(get("/api/stations")
						.param("lat", "48.1173")
						.param("lon", "-1.6778")
						.param("carburant", "GAZOLE"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.statut").value(400))
				.andExpect(jsonPath("$.message").value(containsString("rayon")));
	}

	@Test
	void carburantInconnuDonne400SansAppelerLeService() throws Exception {
		mockMvc.perform(get("/api/stations")
						.param("ville", "Rennes")
						.param("carburant", "KEROSENE"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.statut").value(400))
				.andExpect(jsonPath("$.message").value(containsString("GAZOLE")));

		verifyNoInteractions(service);
	}

	@Test
	void parametreNumeriqueMalFormeDonne400SansAppelerLeService() throws Exception {
		mockMvc.perform(get("/api/stations")
						.param("lat", "nord")
						.param("lon", "-1.6778")
						.param("rayon", "10")
						.param("carburant", "GAZOLE"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.statut").value(400));

		verifyNoInteractions(service);
	}

	@Test
	void absenceDeVilleEtCoordonneesDonne400() throws Exception {
		when(service.rechercher(org.mockito.ArgumentMatchers.any()))
				.thenThrow(new IllegalArgumentException("La latitude et la longitude sont obligatoires hors recherche par ville."));

		mockMvc.perform(get("/api/stations").param("carburant", "GAZOLE"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.statut").value(400));
	}


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

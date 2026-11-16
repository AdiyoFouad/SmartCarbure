package fr.miage.smartcarbure.controller;

import java.util.List;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import fr.miage.smartcarbure.dto.StationDTO;
import fr.miage.smartcarbure.enums.Carburant;
import fr.miage.smartcarbure.enums.TypeTri;
import fr.miage.smartcarbure.exception.GlobalExceptionHandler;
import fr.miage.smartcarbure.service.RechercheService;

class StationControllerTest {

	private final RechercheService service = mock(RechercheService.class);
	private MockMvc mockMvc;

	@BeforeEach
	void creerMockMvc() {
		mockMvc = MockMvcBuilders.standaloneSetup(new StationController(service))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

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
}
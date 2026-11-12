package fr.miage.smartcarbure.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.miage.smartcarbure.dto.EconomieResponse;
import fr.miage.smartcarbure.enums.Carburant;
import fr.miage.smartcarbure.exception.ApiIndisponibleException;
import fr.miage.smartcarbure.exception.GlobalExceptionHandler;
import fr.miage.smartcarbure.exception.StationIntrouvableException;
import fr.miage.smartcarbure.service.EconomieService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests de GET /api/economie sans démarrer Spring Boot ni la base de données :
 * le service est simulé par Mockito, MockMvc vérifie le code HTTP et le JSON renvoyé.
 */
class EconomieControllerTest {

    private final EconomieService service = mock(EconomieService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void creerMockMvc() {
        mockMvc = MockMvcBuilders.standaloneSetup(new EconomieController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void renvoieLeCalculAuFormatDuContrat() throws Exception {
        when(service.calculerPourStation("35000001", Carburant.GAZOLE, 50))
                .thenReturn(new EconomieResponse(new BigDecimal("1.749"), new BigDecimal("1.771"),
                        50, new BigDecimal("1.10")));

        mockMvc.perform(get("/api/economie")
                        .param("idStation", "35000001")
                        .param("carburant", "GAZOLE")
                        .param("litres", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prixStation").value(1.749))
                .andExpect(jsonPath("$.prixMoyenLocal").value(1.771))
                .andExpect(jsonPath("$.litres").value(50.0))
                .andExpect(jsonPath("$.montant").value(1.10));
    }

    @Test
    void stationInconnueDonne404() throws Exception {
        when(service.calculerPourStation("99999999", Carburant.GAZOLE, 50))
                .thenThrow(new StationIntrouvableException("99999999"));

        mockMvc.perform(get("/api/economie")
                        .param("idStation", "99999999")
                        .param("carburant", "GAZOLE")
                        .param("litres", "50"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statut").value(404))
                .andExpect(jsonPath("$.message").value("Station introuvable : 99999999."));
    }

    @Test
    void volumeRefuseParLeServiceDonne400() throws Exception {
        when(service.calculerPourStation("35000001", Carburant.GAZOLE, 0))
                .thenThrow(new IllegalArgumentException("Le volume du plein doit être un nombre strictement positif : 0.0"));

        mockMvc.perform(get("/api/economie")
                        .param("idStation", "35000001")
                        .param("carburant", "GAZOLE")
                        .param("litres", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statut").value(400));
    }

    @Test
    void parametreManquantDonne400SansAppelerLeService() throws Exception {
        mockMvc.perform(get("/api/economie")
                        .param("idStation", "35000001")
                        .param("carburant", "GAZOLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statut").value(400))
                .andExpect(jsonPath("$.message").value(containsString("litres")));

        verifyNoInteractions(service);
    }

    @Test
    void carburantInconnuDonne400SansAppelerLeService() throws Exception {
        mockMvc.perform(get("/api/economie")
                        .param("idStation", "35000001")
                        .param("carburant", "KEROSENE")
                        .param("litres", "50"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("GAZOLE")));

        verifyNoInteractions(service);
    }

    @Test
    void apiIndisponibleDonne503() throws Exception {
        when(service.calculerPourStation("35000001", Carburant.GAZOLE, 50))
                .thenThrow(new ApiIndisponibleException("L'API Prix des carburants ne répond pas."));

        mockMvc.perform(get("/api/economie")
                        .param("idStation", "35000001")
                        .param("carburant", "GAZOLE")
                        .param("litres", "50"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.statut").value(503));
    }
}

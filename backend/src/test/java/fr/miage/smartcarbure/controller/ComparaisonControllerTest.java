package fr.miage.smartcarbure.controller;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import fr.miage.smartcarbure.dto.ComparaisonResponse;
import fr.miage.smartcarbure.dto.ZoneComparaisonDTO;
import fr.miage.smartcarbure.enums.Carburant;
import fr.miage.smartcarbure.exception.GlobalExceptionHandler;
import fr.miage.smartcarbure.service.ComparaisonService;

class ComparaisonControllerTest {

    private final ComparaisonService service = mock(ComparaisonService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void creerMockMvc() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ComparaisonController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void exposeLaComparaisonSurLaRouteDemandee() throws Exception {
        when(service.comparer("VILLE", "Rennes", "Nantes", Carburant.GAZOLE))
                .thenReturn(new ComparaisonResponse("VILLE", Carburant.GAZOLE,
                        new ZoneComparaisonDTO("Rennes", new BigDecimal("1.742"), 4),
                        new ZoneComparaisonDTO("Nantes", new BigDecimal("1.746"), 3),
                        new BigDecimal("-0.004")));

        mockMvc.perform(get("/api/comparer").param("type", "VILLE").param("zone1", "Rennes")
                        .param("zone2", "Nantes").param("carburant", "GAZOLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("VILLE"))
                .andExpect(jsonPath("$.carburant").value("GAZOLE"))
                .andExpect(jsonPath("$.zone1.nom").value("Rennes"))
                .andExpect(jsonPath("$.zone1.prixMoyen").value(1.742))
                .andExpect(jsonPath("$.zone1.nombreStations").value(4))
                .andExpect(jsonPath("$.ecart").value(-0.004));
    }

    @Test
    void parametreManquantDonne400SansAppelerLeService() throws Exception {
        mockMvc.perform(get("/api/comparer").param("type", "VILLE").param("zone1", "Rennes")
                        .param("carburant", "GAZOLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("zone2")));

        verifyNoInteractions(service);
    }
}
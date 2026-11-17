package fr.miage.smartcarbure.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.miage.smartcarbure.client.ItineraireClient;
import fr.miage.smartcarbure.enums.Carburant;
import fr.miage.smartcarbure.exception.ApiIndisponibleException;
import fr.miage.smartcarbure.exception.GlobalExceptionHandler;
import fr.miage.smartcarbure.mapper.DepartementMapper;
import fr.miage.smartcarbure.mapper.HoraireMapper;
import fr.miage.smartcarbure.mapper.PositionMapper;
import fr.miage.smartcarbure.mapper.PrixCarburantMapper;
import fr.miage.smartcarbure.mapper.RegionMapper;
import fr.miage.smartcarbure.mapper.StationMapper;
import fr.miage.smartcarbure.mapper.VilleMapper;
import fr.miage.smartcarbure.model.Position;
import fr.miage.smartcarbure.model.PrixCarburant;
import fr.miage.smartcarbure.model.Station;
import fr.miage.smartcarbure.model.StationSurTrajet;
import fr.miage.smartcarbure.model.Trajet;
import fr.miage.smartcarbure.service.TrajetService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests de GET /api/trajet sans réseau : MockMvc en standaloneSetup avec GlobalExceptionHandler
 * et les vrais mappers ; ItineraireClient et TrajetService sont simulés.
 */
class TrajetControllerTest {

    private static final Position RENNES = new Position(48.1173, -1.6778, null);
    private static final Position NANTES = new Position(47.2184, -1.5536, null);

    private ItineraireClient itineraireClient;
    private TrajetService trajetService;
    private MockMvc mockMvc;

    @BeforeEach
    void creerMockMvc() {
        itineraireClient = mock(ItineraireClient.class);
        trajetService = mock(TrajetService.class);
        PositionMapper positionMapper = new PositionMapper();
        StationMapper stationMapper = new StationMapper(positionMapper,
                new VilleMapper(new DepartementMapper(new RegionMapper())),
                new HoraireMapper(), new PrixCarburantMapper());
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TrajetController(itineraireClient, trajetService, stationMapper, positionMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /** Trajet Rennes → Nantes de 107,4123 km, avec un point intermédiaire. */
    private static Trajet trajetRennesNantes() {
        Position milieu = new Position(47.8431, -1.6819, null);
        return new Trajet(RENNES, NANTES, List.of(RENNES, milieu, NANTES), 107.4123);
    }

    /** Station en SP95 à 1,809 €, située sur la route. */
    private static Station stationBainDeBretagne() {
        return Station.builder()
                .id("35470001")
                .position(new Position(47.8431, -1.6819, "5 route de Rennes"))
                .carburants(List.of(PrixCarburant.builder().carburant(Carburant.SP95).prix(1.809).build()))
                .build();
    }

    // ------------------------------------------------------------------
    // Réponse 200
    // ------------------------------------------------------------------

    @Test
    void renvoieLeTrajetEtLesStationsAuFormatDuContrat() throws Exception {
        when(itineraireClient.calculerTrajet(any(), any())).thenReturn(trajetRennesNantes());
        when(trajetService.rechercherLeLongDuTrajet(any(), any(), eq(Carburant.SP95)))
                .thenReturn(List.of(new StationSurTrajet(stationBainDeBretagne(), 0.234)));

        mockMvc.perform(get("/api/trajet")
                        .param("depart", "48.1173,-1.6778")
                        .param("arrivee", "47.2184,-1.5536")
                        .param("carburant", "SP95"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trajet.depart.latitude").value(48.1173))
                .andExpect(jsonPath("$.trajet.depart.longitude").value(-1.6778))
                .andExpect(jsonPath("$.trajet.arrivee.latitude").value(47.2184))
                .andExpect(jsonPath("$.trajet.points", hasSize(3)))
                .andExpect(jsonPath("$.trajet.points[1].latitude").value(47.8431))
                .andExpect(jsonPath("$.trajet.distanceKm").value(107.4))
                .andExpect(jsonPath("$.stations", hasSize(1)))
                .andExpect(jsonPath("$.stations[0].id").value("35470001"))
                .andExpect(jsonPath("$.stations[0].nom").value(nullValue()))
                .andExpect(jsonPath("$.stations[0].adresse").value("5 route de Rennes"))
                .andExpect(jsonPath("$.stations[0].carburants[0].type").value("SP95"))
                .andExpect(jsonPath("$.stations[0].carburants[0].prix").value(1.809))
                .andExpect(jsonPath("$.stations[0].distanceKm").value(0.2));
    }

    @Test
    void transmetLesPositionsLeCarburantEtUnRayonAbsent() throws Exception {
        Trajet trajet = trajetRennesNantes();
        when(itineraireClient.calculerTrajet(any(), any())).thenReturn(trajet);
        when(trajetService.rechercherLeLongDuTrajet(any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/trajet")
                        .param("depart", " 48.1173 , -1.6778 ")
                        .param("arrivee", "47.2184,-1.5536")
                        .param("carburant", "GAZOLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stations", hasSize(0)));

        verify(itineraireClient).calculerTrajet(RENNES, NANTES);
        verify(trajetService).rechercherLeLongDuTrajet(eq(trajet), isNull(), eq(Carburant.GAZOLE));
    }

    @Test
    void transmetLeRayonDemande() throws Exception {
        Trajet trajet = trajetRennesNantes();
        when(itineraireClient.calculerTrajet(any(), any())).thenReturn(trajet);
        when(trajetService.rechercherLeLongDuTrajet(any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/trajet")
                        .param("depart", "48.1173,-1.6778")
                        .param("arrivee", "47.2184,-1.5536")
                        .param("carburant", "GAZOLE")
                        .param("rayon", "8"))
                .andExpect(status().isOk());

        verify(trajetService).rechercherLeLongDuTrajet(trajet, 8.0, Carburant.GAZOLE);
    }

    // ------------------------------------------------------------------
    // Erreurs 400 détectées avant tout appel externe
    // ------------------------------------------------------------------

    @Test
    void carburantAbsentDonne400() throws Exception {
        mockMvc.perform(get("/api/trajet")
                        .param("depart", "48.1173,-1.6778")
                        .param("arrivee", "47.2184,-1.5536"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statut").value(400))
                .andExpect(jsonPath("$.message").value(containsString("carburant")));

        verifyNoInteractions(itineraireClient, trajetService);
    }

    @Test
    void carburantInconnuDonne400() throws Exception {
        mockMvc.perform(get("/api/trajet")
                        .param("depart", "48.1173,-1.6778")
                        .param("arrivee", "47.2184,-1.5536")
                        .param("carburant", "KEROSENE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statut").value(400));

        verifyNoInteractions(itineraireClient, trajetService);
    }

    @Test
    void departAbsentDonne400() throws Exception {
        mockMvc.perform(get("/api/trajet")
                        .param("arrivee", "47.2184,-1.5536")
                        .param("carburant", "GAZOLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("depart")));

        verifyNoInteractions(itineraireClient, trajetService);
    }

    @Test
    void coordonneesMalFormeesDonnent400() throws Exception {
        for (String depart : List.of("rennes", "48.1173", "48.1;-1.6", "nord,-1.6778", "1,2,3")) {
            mockMvc.perform(get("/api/trajet")
                            .param("depart", depart)
                            .param("arrivee", "47.2184,-1.5536")
                            .param("carburant", "GAZOLE"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.statut").value(400));
        }
        verifyNoInteractions(itineraireClient, trajetService);
    }

    @Test
    void coordonneesHorsLimitesDonnent400() throws Exception {
        mockMvc.perform(get("/api/trajet")
                        .param("depart", "95,-1.6778")
                        .param("arrivee", "47.2184,-1.5536")
                        .param("carburant", "GAZOLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("hors limites")));

        verifyNoInteractions(itineraireClient, trajetService);
    }

    @Test
    void rayonHorsLimitesDonne400SansAppelerLeServiceDItineraire() throws Exception {
        for (String rayon : List.of("0.5", "11")) {
            mockMvc.perform(get("/api/trajet")
                            .param("depart", "48.1173,-1.6778")
                            .param("arrivee", "47.2184,-1.5536")
                            .param("carburant", "GAZOLE")
                            .param("rayon", rayon))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("rayon")));
        }
        verifyNoInteractions(itineraireClient, trajetService);
    }

    // ------------------------------------------------------------------
    // Erreurs venant des services
    // ------------------------------------------------------------------

    @Test
    void aucunItineraireRoutierDonne400() throws Exception {
        when(itineraireClient.calculerTrajet(any(), any()))
                .thenThrow(new IllegalArgumentException("Aucun itinéraire routier trouvé entre ces deux points."));

        mockMvc.perform(get("/api/trajet")
                        .param("depart", "48.1173,-1.6778")
                        .param("arrivee", "47.0,-3.5")
                        .param("carburant", "GAZOLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Aucun itinéraire routier trouvé entre ces deux points."));
    }

    @Test
    void trajetTropLongDonne400() throws Exception {
        when(itineraireClient.calculerTrajet(any(), any())).thenReturn(trajetRennesNantes());
        when(trajetService.rechercherLeLongDuTrajet(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Le trajet dépasse 500 km."));

        mockMvc.perform(get("/api/trajet")
                        .param("depart", "48.1173,-1.6778")
                        .param("arrivee", "43.2965,5.3698")
                        .param("carburant", "GAZOLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le trajet dépasse 500 km."));
    }

    @Test
    void serviceIndisponibleDonne503() throws Exception {
        when(itineraireClient.calculerTrajet(any(), any()))
                .thenThrow(new ApiIndisponibleException("Le service d'itinéraire ne répond pas."));

        mockMvc.perform(get("/api/trajet")
                        .param("depart", "48.1173,-1.6778")
                        .param("arrivee", "47.2184,-1.5536")
                        .param("carburant", "GAZOLE"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.statut").value(503));
    }

    // ------------------------------------------------------------------
    // Outils
    // ------------------------------------------------------------------

    @Test
    void arrondirGardeUneDecimale() {
        assertEquals(3.2, TrajetController.arrondir(3.24));
        assertEquals(3.3, TrajetController.arrondir(3.25));
        assertEquals(0.0, TrajetController.arrondir(0.04));
        assertEquals(107.4, TrajetController.arrondir(107.4123));
    }
}

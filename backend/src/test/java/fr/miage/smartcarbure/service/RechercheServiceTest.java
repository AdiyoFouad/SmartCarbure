package fr.miage.smartcarbure.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.miage.smartcarbure.client.CarburantApiClient;
import fr.miage.smartcarbure.dto.CritereRechercheDTO;
import fr.miage.smartcarbure.dto.StationDTO;
import fr.miage.smartcarbure.enums.Carburant;
import fr.miage.smartcarbure.enums.TypeTri;
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

class RechercheServiceTest {

    private final CarburantApiClient carburantApiClient = mock(CarburantApiClient.class);
    private final StationMapper stationMapper = new StationMapper(
            new PositionMapper(),
            new VilleMapper(new DepartementMapper(new RegionMapper())),
            new HoraireMapper(),
            new PrixCarburantMapper());
    private final RechercheService service = new RechercheService(
            carburantApiClient, new CalculDistance(), stationMapper);

    @BeforeEach
    void verifierMocks() {
        org.mockito.Mockito.reset(carburantApiClient);
    }

    @Test
    void filtreParCarburantEtRayonEtTrieParDistance() {
        Station proche = station("proche", 0.02, 0.0, Carburant.GAZOLE, 1.80, true);
        Station lointaine = station("lointaine", 0.07, 0.0, Carburant.GAZOLE, 1.60, true);
        Station horsRayon = station("hors-rayon", 0.2, 0.0, Carburant.GAZOLE, 1.50, true);
        Station autreCarburant = station("autre-carburant", 0.01, 0.0, Carburant.E10, 1.70, true);
        when(carburantApiClient.rechercherAutour(0.0, 0.0, 10.0))
                .thenReturn(List.of(lointaine, autreCarburant, horsRayon, proche));

        CritereRechercheDTO critere = new CritereRechercheDTO();
        critere.setLat(0.0);
        critere.setLon(0.0);
        critere.setRayon(10);
        critere.setCarburant("GAZOLE");
        critere.setTri(TypeTri.DISTANCE);

        List<StationDTO> resultats = service.rechercher(critere);

        assertEquals(List.of("proche", "lointaine"),
                resultats.stream().map(StationDTO::getId).toList());
        assertEquals(2.2, resultats.getFirst().getDistanceKm(), 0.1);
        assertEquals(7.8, resultats.getLast().getDistanceKm(), 0.1);
    }

    @Test
    void accepteE10EtPlaceLesRupturesApresLesPrixDisponibles() {
        Station enRupture = station("rupture", null, null, Carburant.E10, 1.20, false);
        Station moinsChere = station("moins-chere", null, null, Carburant.E10, 1.50, true);
        Station moinsEconomique = station("plus-chere", null, null, Carburant.E10, 1.80, true);
        Station sansE10 = station("sans-e10", null, null, Carburant.GAZOLE, 1.30, true);
        when(carburantApiClient.rechercherParVille("Rennes"))
                .thenReturn(List.of(enRupture, moinsEconomique, sansE10, moinsChere));

        CritereRechercheDTO critere = new CritereRechercheDTO();
        critere.setVille("Rennes");
        critere.setCarburant("E10");
        critere.setTri(TypeTri.PRIX);

        List<StationDTO> resultats = service.rechercher(critere);

        assertEquals(List.of("moins-chere", "plus-chere", "rupture"),
                resultats.stream().map(StationDTO::getId).toList());
        assertNull(resultats.getFirst().getDistanceKm());
    }

    @Test
    void rechercheUnCodePostalAvecLaMethodeDediee() {
            when(carburantApiClient.rechercherParCodePostal("35000")).thenReturn(List.of());

            CritereRechercheDTO critere = new CritereRechercheDTO();
            critere.setVille(" 35000 ");
            critere.setCarburant("GAZOLE");

            assertEquals(List.of(), service.rechercher(critere));
            verify(carburantApiClient).rechercherParCodePostal("35000");
    }

    @Test
    void refuseLesCriteresDePositionInvalidesEtUnCarburantInconnu() {
        CritereRechercheDTO critere = new CritereRechercheDTO();
        critere.setCarburant("GAZOLE");
        critere.setLat(48.1);
        critere.setRayon(10);
        assertThrows(IllegalArgumentException.class, () -> service.rechercher(critere));

        critere.setLon(-1.6);
        critere.setRayon(51);
        assertThrows(IllegalArgumentException.class, () -> service.rechercher(critere));

        critere.setRayon(10);
        critere.setCarburant("KEROZENE");
        assertThrows(IllegalArgumentException.class, () -> service.rechercher(critere));

        critere.setCarburant("SP95-E10");
        assertThrows(IllegalArgumentException.class, () -> service.rechercher(critere));
    }

    private Station station(String id, Double latitude, Double longitude,
                            Carburant carburant, Double prix, boolean disponible) {
        Position position = latitude == null ? null : new Position(latitude, longitude, null);
        return Station.builder()
                .id(id)
                .position(position)
                .carburants(List.of(PrixCarburant.builder()
                        .carburant(carburant)
                        .prix(prix)
                        .disponible(disponible)
                        .build()))
                .build();
    }
}

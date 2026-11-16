package fr.miage.smartcarbure.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fr.miage.smartcarbure.client.CarburantApiClient;
import fr.miage.smartcarbure.dto.PrixCarburantDTO;
import fr.miage.smartcarbure.dto.StationDTO;
import fr.miage.smartcarbure.enums.Carburant;
import fr.miage.smartcarbure.enums.TypeRupture;
import fr.miage.smartcarbure.exception.ApiIndisponibleException;
import fr.miage.smartcarbure.exception.StationIntrouvableException;
import fr.miage.smartcarbure.mapper.DepartementMapper;
import fr.miage.smartcarbure.mapper.HoraireMapper;
import fr.miage.smartcarbure.mapper.PositionMapper;
import fr.miage.smartcarbure.mapper.PrixCarburantMapper;
import fr.miage.smartcarbure.mapper.RegionMapper;
import fr.miage.smartcarbure.mapper.StationMapper;
import fr.miage.smartcarbure.mapper.VilleMapper;
import fr.miage.smartcarbure.model.PrixCarburant;
import fr.miage.smartcarbure.model.Station;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests de StationService : CarburantApiClient est simulé avec Mockito, le vrai StationMapper est utilisé. */
class StationServiceTest {

    private CarburantApiClient client;
    private StationService service;

    @BeforeEach
    void creerService() {
        client = mock(CarburantApiClient.class);
        StationMapper mapper = new StationMapper(new PositionMapper(),
                new VilleMapper(new DepartementMapper(new RegionMapper())),
                new HoraireMapper(), new PrixCarburantMapper());
        service = new StationService(client, mapper);
    }

    @Test
    void stationTrouveeDonneSaFicheAvecTousSesCarburants() {
        Station station = Station.builder()
                .id("35000013")
                .carburants(List.of(
                        PrixCarburant.builder().carburant(Carburant.GAZOLE).prix(1.789)
                                .dateMaj(LocalDateTime.of(2026, 1, 15, 14, 30)).build(),
                        PrixCarburant.builder().carburant(Carburant.E85).disponible(false)
                                .typeRupture(TypeRupture.TEMPORAIRE)
                                .debutRupture(LocalDateTime.of(2026, 7, 15, 14, 30)).build()))
                .build();
        when(client.trouverParId("35000013")).thenReturn(Optional.of(station));

        StationDTO fiche = service.getFiche("35000013");

        assertEquals("35000013", fiche.getId());
        assertNull(fiche.getNom());
        assertNull(fiche.getDistanceKm());
        assertEquals(2, fiche.getCarburants().size());

        PrixCarburantDTO gazole = fiche.getCarburants().get(0);
        assertEquals(Carburant.GAZOLE, gazole.getType());
        // Heure d'hiver : 14:30 à Paris = 13:30 UTC
        assertEquals(Instant.parse("2026-01-15T13:30:00Z"), gazole.getDateMaj());

        PrixCarburantDTO e85 = fiche.getCarburants().get(1);
        assertEquals(false, e85.isDisponible());
        assertNull(e85.getPrix());
        assertEquals(TypeRupture.TEMPORAIRE, e85.getTypeRupture());
        // Heure d'été : 14:30 à Paris = 12:30 UTC
        assertEquals(Instant.parse("2026-07-15T12:30:00Z"), e85.getDebutRupture());
    }

    @Test
    void stationAbsenteLeveStationIntrouvable() {
        when(client.trouverParId("99999999")).thenReturn(Optional.empty());

        StationIntrouvableException e = assertThrows(StationIntrouvableException.class,
                () -> service.getFiche("99999999"));
        assertEquals("99999999", e.getIdStation());
        assertEquals("Station introuvable : 99999999.", e.getMessage());
    }

    @Test
    void idNonNumeriqueLeveStationIntrouvable() {
        // CarburantApiClient.trouverParId renvoie Optional.empty() sans appel réseau pour un id non numérique
        when(client.trouverParId("abc")).thenReturn(Optional.empty());

        assertThrows(StationIntrouvableException.class, () -> service.getFiche("abc"));
    }

    @Test
    void apiEnPannePropageApiIndisponible() {
        when(client.trouverParId("35000013"))
                .thenThrow(new ApiIndisponibleException("L'API Prix des carburants ne répond pas."));

        assertThrows(ApiIndisponibleException.class, () -> service.getFiche("35000013"));
    }
}

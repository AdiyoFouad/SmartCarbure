package fr.miage.smartcarbure.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import fr.miage.smartcarbure.dto.StationDTO;
import fr.miage.smartcarbure.enums.Carburant;
import fr.miage.smartcarbure.enums.TypeEmplacement;
import fr.miage.smartcarbure.model.Departement;
import fr.miage.smartcarbure.model.Horaire;
import fr.miage.smartcarbure.model.Position;
import fr.miage.smartcarbure.model.PrixCarburant;
import fr.miage.smartcarbure.model.Region;
import fr.miage.smartcarbure.model.ServiceStation;
import fr.miage.smartcarbure.model.Station;
import fr.miage.smartcarbure.model.Ville;

class StationMapperTest {

    private final PositionMapper positionMapper = new PositionMapper();
    private final VilleMapper villeMapper = new VilleMapper(
            new DepartementMapper(new RegionMapper()));
    private final HoraireMapper horaireMapper = new HoraireMapper();
    private final ServiceStationMapper serviceMapper = new ServiceStationMapper();
    private final PrixCarburantMapper prixCarburantMapper = new PrixCarburantMapper();
    private final StationMapper mapper = new StationMapper(positionMapper, villeMapper,
            horaireMapper, serviceMapper, prixCarburantMapper);

    @Test
    void convertitTouteLaStationEtSesSousObjetsEnDto() {
        Station station = Station.builder()
                .id("35000001")
                .nom("Station test")
                .position(new Position(48.1, -1.68, "1 rue X"))
                .ville(new Ville("Rennes", "35000",
                        new Departement("35", "Ille-et-Vilaine", new Region("53", "Bretagne"))))
                .emplacement(TypeEmplacement.AUTOROUTE)
                .automate24h(true)
                .horaires(List.of(new Horaire(DayOfWeek.MONDAY, false,
                        LocalTime.of(8, 0), LocalTime.of(20, 0))))
                .services(List.of(new ServiceStation("Lavage")))
                .carburants(List.of(PrixCarburant.builder()
                        .carburant(Carburant.GAZOLE)
                        .prix(1.789)
                        .dateMaj(LocalDateTime.of(2026, 11, 4, 8, 56, 1))
                        .build()))
                .build();

        StationDTO dto = mapper.toDTO(station);

        assertEquals(station.getId(), dto.getId());
        assertEquals(station.getNom(), dto.getNom());
        assertEquals(station.getPosition().getLatitude(), dto.getPosition().getLatitude());
        assertEquals("Rennes", dto.getVille().getNom());
        assertEquals("53", dto.getVille().getDepartement().getRegion().getCode());
        assertEquals(TypeEmplacement.AUTOROUTE, dto.getEmplacement());
        assertEquals(DayOfWeek.MONDAY, dto.getHoraires().getFirst().getJour());
        assertEquals(LocalTime.of(8, 0), dto.getHoraires().getFirst().getOuverture());
        assertEquals("Lavage", dto.getServices().getFirst().getNom());
        assertEquals(Carburant.GAZOLE, dto.getCarburants().getFirst().getCarburant());
        assertEquals(Instant.parse("2026-11-04T07:56:01Z"), dto.getCarburants().getFirst().getDateMaj());
        assertNull(dto.getCarburants().getFirst().getDebutRupture());
        assertNull(dto.getDistanceKm());
    }

    @Test
    void convertitDtoEnStationAvecSesSousObjets() {
        StationDTO dto = new StationDTO();
        dto.setId("35000001");
        dto.setNom("Station test");
        dto.setPosition(positionMapper.toDTO(new Position(48.1, -1.68, "1 rue X")));
        dto.setVille(villeMapper.toDTO(new Ville("Rennes", "35000",
                new Departement("35", "Ille-et-Vilaine", new Region("53", "Bretagne")))));
        dto.setEmplacement(TypeEmplacement.AUTOROUTE);
        dto.setAutomate24h(true);
        dto.setHoraires(List.of(horaireMapper.toDTO(
                new Horaire(DayOfWeek.MONDAY, false, LocalTime.of(8, 0), LocalTime.of(20, 0)))));
        dto.setServices(List.of(serviceMapper.toDTO(new ServiceStation("Lavage"))));
        dto.setCarburants(List.of(prixCarburantMapper.toDTO(PrixCarburant.builder()
                .carburant(Carburant.GAZOLE)
                .prix(1.789)
                .dateMaj(LocalDateTime.of(2026, 11, 4, 8, 56, 1))
                .build())));
        dto.setDistanceKm(3.2);

        Station station = mapper.fromDTO(dto);

        assertEquals("35000001", station.getId());
        assertEquals("Station test", station.getNom());
        assertEquals(48.1, station.getPosition().getLatitude());
        assertEquals("Rennes", station.getVille().getNom());
        assertEquals("53", station.getRegion().getCode());
        assertEquals(TypeEmplacement.AUTOROUTE, station.getEmplacement());
        assertTrue(station.isAutomate24h());
        assertEquals(DayOfWeek.MONDAY, station.getHoraires().getFirst().getJour());
        assertEquals("Lavage", station.getServices().getFirst().getNom());
        assertEquals(Carburant.GAZOLE, station.getCarburants().getFirst().getCarburant());
        assertEquals(LocalDateTime.of(2026, 11, 4, 8, 56, 1),
                station.getCarburants().getFirst().getDateMaj());
    }

}

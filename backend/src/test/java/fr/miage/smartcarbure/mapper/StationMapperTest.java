package fr.miage.smartcarbure.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
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
    private final PrixCarburantMapper prixCarburantMapper = new PrixCarburantMapper();
    private final StationMapper mapper = new StationMapper(positionMapper, villeMapper,
            horaireMapper, prixCarburantMapper);

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
        assertEquals("1 rue X", dto.getAdresse());
        assertEquals(station.getPosition().getLatitude(), dto.getPosition().getLatitude());
        assertEquals("Rennes", dto.getVille());
        assertEquals("35000", dto.getCodePostal());
        assertEquals("35", dto.getDepartement().getCode());
        assertEquals("53", dto.getRegion().getCode());
        assertEquals(TypeEmplacement.AUTOROUTE, dto.getEmplacement());
        assertEquals("lundi", dto.getHoraires().getFirst().getJour());
        assertEquals(LocalTime.of(8, 0), dto.getHoraires().getFirst().getOuverture());
        assertEquals("Lavage", dto.getServices().getFirst());
        assertEquals(Carburant.GAZOLE, dto.getCarburants().getFirst().getType());
        assertEquals(Instant.parse("2026-11-04T07:56:01Z"), dto.getCarburants().getFirst().getDateMaj());
        assertNull(dto.getCarburants().getFirst().getDebutRupture());
        assertNull(dto.getDistanceKm());

        JsonNode json = JsonMapper.builder().build().valueToTree(dto);
        assertEquals("1 rue X", json.get("adresse").asString());
        assertEquals("Rennes", json.get("ville").asString());
        assertEquals("35000", json.get("codePostal").asString());
        assertEquals("Ille-et-Vilaine", json.get("departement").get("nom").asString());
        assertEquals("Bretagne", json.get("region").get("nom").asString());
        assertFalse(json.get("position").has("adresse"));
        assertFalse(json.get("departement").has("region"));
        assertEquals("lundi", json.get("horaires").get(0).get("jour").asString());
        assertFalse(json.get("horaires").get(0).has("ferme"));
        assertEquals("Lavage", json.get("services").get(0).asString());
        assertEquals("GAZOLE", json.get("carburants").get(0).get("type").asString());
        assertFalse(json.get("carburants").get(0).has("carburant"));
        assertFalse(json.has("distanceKm"));
    }

}

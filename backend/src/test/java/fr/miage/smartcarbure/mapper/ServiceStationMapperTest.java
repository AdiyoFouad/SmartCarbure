package fr.miage.smartcarbure.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import fr.miage.smartcarbure.dto.ServiceStationDTO;
import fr.miage.smartcarbure.model.ServiceStation;

class ServiceStationMapperTest {

    private final ServiceStationMapper mapper = new ServiceStationMapper();

    @Test
    void convertitService() {
        ServiceStationDTO dto = mapper.toDTO(new ServiceStation("Lavage"));

        assertEquals("Lavage", dto.getNom());
    }

    @Test
    void convertitServiceDepuisDto() {
        ServiceStationDTO dto = new ServiceStationDTO();
        dto.setNom("Lavage");

        ServiceStation service = mapper.fromDTO(dto);

        assertEquals("Lavage", service.getNom());
    }
}

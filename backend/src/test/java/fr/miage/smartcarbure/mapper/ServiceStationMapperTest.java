package fr.miage.smartcarbure.mapper;

import org.junit.jupiter.api.Test;

import fr.miage.smartcarbure.dto.ServiceStationDTO;
import fr.miage.smartcarbure.model.ServiceStation;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceStationMapperTest {

    private final ServiceStationMapper mapper = new ServiceStationMapper();

    @Test
    void convertitService() {
        ServiceStationDTO dto = mapper.toDTO(new ServiceStation("Lavage"));

        assertEquals("Lavage", dto.getNom());
    }

}

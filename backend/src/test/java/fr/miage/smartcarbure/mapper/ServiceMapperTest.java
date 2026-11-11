package fr.miage.smartcarbure.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import fr.miage.smartcarbure.dto.ServiceDTO;
import fr.miage.smartcarbure.model.Service;

class ServiceMapperTest {

    private final ServiceMapper mapper = new ServiceMapper();

    @Test
    void convertitService() {
        ServiceDTO dto = mapper.toDTO(new Service("Lavage"));

        assertEquals("Lavage", dto.getNom());
    }

    @Test
    void convertitServiceDepuisDto() {
        ServiceDTO dto = new ServiceDTO();
        dto.setNom("Lavage");

        Service service = mapper.fromDTO(dto);

        assertEquals("Lavage", service.getNom());
    }
}

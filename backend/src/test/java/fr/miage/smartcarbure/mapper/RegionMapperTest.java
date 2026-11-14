package fr.miage.smartcarbure.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import fr.miage.smartcarbure.dto.RegionDTO;
import fr.miage.smartcarbure.model.Region;

class RegionMapperTest {

    private final RegionMapper mapper = new RegionMapper();

    @Test
    void convertitRegion() {
        RegionDTO dto = mapper.toDTO(new Region("53", "Bretagne"));

        assertEquals("53", dto.getCode());
        assertEquals("Bretagne", dto.getNom());
    }

    @Test
    void renvoieNullPourUneRegionNulle() {
        assertNull(mapper.toDTO(null));
    }
}

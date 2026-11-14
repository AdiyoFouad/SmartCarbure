package fr.miage.smartcarbure.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import fr.miage.smartcarbure.dto.DepartementDTO;
import fr.miage.smartcarbure.dto.RegionDTO;
import fr.miage.smartcarbure.model.Departement;
import fr.miage.smartcarbure.model.Region;

class DepartementMapperTest {

    private final DepartementMapper mapper = new DepartementMapper(new RegionMapper());

    @Test
    void convertitDepartementEtSaRegion() {
        DepartementDTO dto = mapper.toDTO(
                new Departement("35", "Ille-et-Vilaine", new Region("53", "Bretagne")));

        assertEquals("35", dto.getCode());
        assertEquals("Ille-et-Vilaine", dto.getNom());
        assertEquals("53", dto.getRegion().getCode());
        assertEquals("Bretagne", dto.getRegion().getNom());
    }

    @Test
    void accepteUnDepartementSansRegion() {
        DepartementDTO dto = mapper.toDTO(new Departement("35", "Ille-et-Vilaine", null));

        assertEquals("35", dto.getCode());
        assertNull(dto.getRegion());
    }

    @Test
    void renvoieNullPourUnDepartementNul() {
        assertNull(mapper.toDTO(null));
    }
}

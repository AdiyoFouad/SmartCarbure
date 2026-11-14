package fr.miage.smartcarbure.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import fr.miage.smartcarbure.dto.VilleDTO;
import fr.miage.smartcarbure.model.Departement;
import fr.miage.smartcarbure.model.Region;
import fr.miage.smartcarbure.model.Ville;

class VilleMapperTest {

    private final VilleMapper mapper = new VilleMapper(
            new DepartementMapper(new RegionMapper()));

    @Test
    void convertitVilleEtSaHierarchieGeographique() {
        VilleDTO dto = mapper.toDTO(new Ville("Rennes", "35000",
                new Departement("35", "Ille-et-Vilaine", new Region("53", "Bretagne"))));

        assertEquals("Rennes", dto.getNom());
        assertEquals("35000", dto.getCodePostal());
        assertEquals("35", dto.getDepartement().getCode());
        assertEquals("53", dto.getDepartement().getRegion().getCode());
    }

    @Test
    void accepteUneVilleSansDepartement() {
        VilleDTO dto = mapper.toDTO(new Ville("Rennes", "35000", null));

        assertEquals("Rennes", dto.getNom());
        assertNull(dto.getDepartement());
    }

    @Test
    void renvoieNullPourUneVilleNulle() {
        assertNull(mapper.toDTO(null));
    }
}

package fr.miage.smartcarbure.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import fr.miage.smartcarbure.dto.VilleDTO;
import fr.miage.smartcarbure.dto.DepartementDTO;
import fr.miage.smartcarbure.dto.RegionDTO;
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
        assertNull(mapper.fromDTO(null));
    }

    @Test
    void convertitVilleDepuisDtoEtSaHierarchieGeographique() {
        RegionDTO regionDTO = new RegionDTO();
        regionDTO.setCode("53");
        regionDTO.setNom("Bretagne");
        DepartementDTO departementDTO = new DepartementDTO();
        departementDTO.setCode("35");
        departementDTO.setNom("Ille-et-Vilaine");
        departementDTO.setRegion(regionDTO);
        VilleDTO dto = new VilleDTO();
        dto.setNom("Rennes");
        dto.setCodePostal("35000");
        dto.setDepartement(departementDTO);

        Ville ville = mapper.fromDTO(dto);

        assertEquals("Rennes", ville.getNom());
        assertEquals("35000", ville.getCodePostal());
        assertEquals("35", ville.getDepartement().getCode());
        assertEquals("53", ville.getDepartement().getRegion().getCode());
    }
}

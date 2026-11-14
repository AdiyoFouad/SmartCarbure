package fr.miage.smartcarbure.mapper;

import org.springframework.stereotype.Component;

import fr.miage.smartcarbure.dto.DepartementDTO;
import fr.miage.smartcarbure.model.Departement;

@Component
public class DepartementMapper {

    private final RegionMapper regionMapper;

    public DepartementMapper(RegionMapper regionMapper) {
        this.regionMapper = regionMapper;
    }

    public DepartementDTO toDTO(Departement departement) {
        if (departement == null) {
            return null;
        }
        DepartementDTO dto = new DepartementDTO();
        dto.setCode(departement.getCode());
        dto.setNom(departement.getNom());
        dto.setRegion(regionMapper.toDTO(departement.getRegion()));
        return dto;
    }
}

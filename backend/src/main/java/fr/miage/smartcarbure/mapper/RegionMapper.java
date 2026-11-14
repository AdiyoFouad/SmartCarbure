package fr.miage.smartcarbure.mapper;

import org.springframework.stereotype.Component;

import fr.miage.smartcarbure.dto.RegionDTO;
import fr.miage.smartcarbure.model.Region;

@Component
public class RegionMapper {

    public RegionDTO toDTO(Region region) {
        if (region == null) {
            return null;
        }
        RegionDTO dto = new RegionDTO();
        dto.setCode(region.getCode());
        dto.setNom(region.getNom());
        return dto;
    }
}

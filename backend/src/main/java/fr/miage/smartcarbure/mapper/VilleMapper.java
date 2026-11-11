package fr.miage.smartcarbure.mapper;

import org.springframework.stereotype.Component;

import fr.miage.smartcarbure.dto.VilleDTO;
import fr.miage.smartcarbure.model.Ville;

@Component
public class VilleMapper {

    private final DepartementMapper departementMapper;

    public VilleMapper(DepartementMapper departementMapper) {
        this.departementMapper = departementMapper;
    }

    public VilleDTO toDTO(Ville ville) {
        if (ville == null) {
            return null;
        }
        VilleDTO dto = new VilleDTO();
        dto.setNom(ville.getNom());
        dto.setCodePostal(ville.getCodePostal());
        dto.setDepartement(departementMapper.toDTO(ville.getDepartement()));
        return dto;
    }

    public Ville fromDTO(VilleDTO dto) {
        if (dto == null) {
            return null;
        }
        return new Ville(dto.getNom(), dto.getCodePostal(),
                departementMapper.fromDTO(dto.getDepartement()));
    }
}

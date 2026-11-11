package fr.miage.smartcarbure.mapper;

import org.springframework.stereotype.Component;

import fr.miage.smartcarbure.dto.PositionDTO;
import fr.miage.smartcarbure.model.Position;

@Component
public class PositionMapper {

    public PositionDTO toDTO(Position position) {
        if (position == null) {
            return null;
        }
        PositionDTO dto = new PositionDTO();
        dto.setLatitude(position.getLatitude());
        dto.setLongitude(position.getLongitude());
        dto.setAdresse(position.getAdresse());
        return dto;
    }

    public Position fromDTO(PositionDTO dto) {
        if (dto == null) {
            return null;
        }
        return new Position(dto.getLatitude(), dto.getLongitude(), dto.getAdresse());
    }
}

package fr.miage.smartcarbure.mapper;

import org.springframework.stereotype.Component;

import fr.miage.smartcarbure.dto.ServiceStationDTO;
import fr.miage.smartcarbure.model.ServiceStation;

@Component
public class ServiceStationMapper {

    public ServiceStationDTO toDTO(ServiceStation service) {
        ServiceStationDTO dto = new ServiceStationDTO();
        dto.setNom(service.getNom());
        return dto;
    }

    public ServiceStation fromDTO(ServiceStationDTO dto) {
        return new ServiceStation(dto.getNom());
    }
}

package fr.miage.smartcarbure.mapper;

import org.springframework.stereotype.Component;

import fr.miage.smartcarbure.dto.ServiceDTO;
import fr.miage.smartcarbure.model.Service;

@Component
public class ServiceMapper {

    public ServiceDTO toDTO(Service service) {
        ServiceDTO dto = new ServiceDTO();
        dto.setNom(service.getNom());
        return dto;
    }

    public Service fromDTO(ServiceDTO dto) {
        return new Service(dto.getNom());
    }
}

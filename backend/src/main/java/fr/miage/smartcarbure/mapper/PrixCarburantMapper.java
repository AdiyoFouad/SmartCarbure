package fr.miage.smartcarbure.mapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import fr.miage.smartcarbure.dto.PrixCarburantDTO;
import fr.miage.smartcarbure.model.PrixCarburant;

@Component
public class PrixCarburantMapper {

    private static final ZoneId ZONE_PARIS = ZoneId.of("Europe/Paris");

    public PrixCarburantDTO toDTO(PrixCarburant prixCarburant) {
        PrixCarburantDTO dto = new PrixCarburantDTO();
        dto.setType(prixCarburant.getCarburant());
        dto.setPrix(prixCarburant.getPrix());
        dto.setDateMaj(toInstant(prixCarburant.getDateMaj()));
        dto.setDisponible(prixCarburant.isDisponible());
        dto.setTypeRupture(prixCarburant.getTypeRupture());
        dto.setDebutRupture(toInstant(prixCarburant.getDebutRupture()));
        return dto;
    }
    private Instant toInstant(LocalDateTime date) {
        return date == null ? null : date.atZone(ZONE_PARIS).toInstant();
    }
}

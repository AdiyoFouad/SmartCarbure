package fr.miage.smartcarbure.mapper;

import java.util.Objects;

import org.springframework.stereotype.Component;

import fr.miage.smartcarbure.dto.StationDTO;
import fr.miage.smartcarbure.dto.VilleDTO;
import fr.miage.smartcarbure.model.ServiceStation;
import fr.miage.smartcarbure.model.Station;

@Component
public class StationMapper {

    private final PositionMapper positionMapper;
    private final VilleMapper villeMapper;
    private final HoraireMapper horaireMapper;
    private final PrixCarburantMapper prixCarburantMapper;

    public StationMapper(PositionMapper positionMapper, VilleMapper villeMapper,
                         HoraireMapper horaireMapper, PrixCarburantMapper prixCarburantMapper) {
        this.positionMapper = positionMapper;
        this.villeMapper = villeMapper;
        this.horaireMapper = horaireMapper;
        this.prixCarburantMapper = prixCarburantMapper;
    }

    public StationDTO toDTO(Station station) {
        Objects.requireNonNull(station, "La station à convertir est obligatoire.");

        StationDTO dto = new StationDTO();
        dto.setId(station.getId());
        dto.setNom(station.getNom());
        dto.setAdresse(station.getPosition() == null ? null : station.getPosition().getAdresse());
        dto.setPosition(positionMapper.toDTO(station.getPosition()));
        VilleDTO ville = villeMapper.toDTO(station.getVille());
        if (ville != null) {
            dto.setVille(ville.getNom());
            dto.setCodePostal(ville.getCodePostal());
            dto.setDepartement(ville.getDepartement());
            dto.setRegion(ville.getDepartement() == null ? null : ville.getDepartement().getRegion());
        }
        dto.setEmplacement(station.getEmplacement());
        dto.setAutomate24h(station.isAutomate24h());
        dto.setHoraires(station.getHoraires().stream().map(horaireMapper::toDTO).toList());
        dto.setServices(station.getServices().stream().map(ServiceStation::getNom).toList());
        dto.setCarburants(station.getCarburants().stream().map(prixCarburantMapper::toDTO).toList());
        return dto;
    }

}

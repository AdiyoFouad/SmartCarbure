package fr.miage.smartcarbure.mapper;

import java.util.Objects;

import org.springframework.stereotype.Component;

import fr.miage.smartcarbure.dto.StationDTO;
import fr.miage.smartcarbure.model.Station;

@Component
public class StationMapper {

    private final PositionMapper positionMapper;
    private final VilleMapper villeMapper;
    private final HoraireMapper horaireMapper;
    private final ServiceMapper serviceMapper;
    private final PrixCarburantMapper prixCarburantMapper;

    public StationMapper(PositionMapper positionMapper, VilleMapper villeMapper,
                         HoraireMapper horaireMapper, ServiceMapper serviceMapper,
                         PrixCarburantMapper prixCarburantMapper) {
        this.positionMapper = positionMapper;
        this.villeMapper = villeMapper;
        this.horaireMapper = horaireMapper;
        this.serviceMapper = serviceMapper;
        this.prixCarburantMapper = prixCarburantMapper;
    }

    public StationDTO toDTO(Station station) {
        Objects.requireNonNull(station, "La station à convertir est obligatoire.");

        StationDTO dto = new StationDTO();
        dto.setId(station.getId());
        dto.setNom(station.getNom());
        dto.setPosition(positionMapper.toDTO(station.getPosition()));
        dto.setVille(villeMapper.toDTO(station.getVille()));
        dto.setEmplacement(station.getEmplacement());
        dto.setAutomate24h(station.isAutomate24h());
        dto.setHoraires(station.getHoraires().stream().map(horaireMapper::toDTO).toList());
        dto.setServices(station.getServices().stream().map(serviceMapper::toDTO).toList());
        dto.setCarburants(station.getCarburants().stream().map(prixCarburantMapper::toDTO).toList());
        return dto;
    }

    public Station fromDTO(StationDTO dto) {
        Objects.requireNonNull(dto, "Le DTO de station à convertir est obligatoire.");

        return Station.builder()
                .id(dto.getId())
                .nom(dto.getNom())
                .position(positionMapper.fromDTO(dto.getPosition()))
                .ville(villeMapper.fromDTO(dto.getVille()))
                .emplacement(dto.getEmplacement())
                .automate24h(dto.isAutomate24h())
                .horaires(dto.getHoraires() == null ? null
                        : dto.getHoraires().stream().map(horaireMapper::fromDTO).toList())
                .services(dto.getServices() == null ? null
                        : dto.getServices().stream().map(serviceMapper::fromDTO).toList())
                .carburants(dto.getCarburants() == null ? null
                        : dto.getCarburants().stream().map(prixCarburantMapper::fromDTO).toList())
                .build();
    }
}

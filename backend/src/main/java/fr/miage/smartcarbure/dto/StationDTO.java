package fr.miage.smartcarbure.dto;

import java.util.List;

import fr.miage.smartcarbure.enums.TypeEmplacement;
import lombok.Data;

@Data
public class StationDTO {
	private String id;
	private String nom;
	private PositionDTO position;
	private VilleDTO ville;
	private TypeEmplacement emplacement;
    private boolean automate24h;
	private List<HoraireDTO> horaires;
	private List<ServiceDTO> services;
	private List<PrixCarburantDTO> carburants;
	private Double distanceKm;
}

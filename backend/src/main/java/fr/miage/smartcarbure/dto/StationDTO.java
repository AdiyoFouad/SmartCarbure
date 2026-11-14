package fr.miage.smartcarbure.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import fr.miage.smartcarbure.enums.TypeEmplacement;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StationDTO {
	private String id;
	private String nom;
	private String adresse;
	private String ville;
	private String codePostal;
	private DepartementDTO departement;
	private RegionDTO region;
	private PositionDTO position;
	private TypeEmplacement emplacement;
    private boolean automate24h;
	private List<HoraireDTO> horaires;
	private List<String> services;
	private List<PrixCarburantDTO> carburants;
	private Double distanceKm;
}

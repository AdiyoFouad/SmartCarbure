package fr.miage.smartcarbure.dto;

import lombok.Data;

/** Critères d'une recherche de stations : lieu, carburant, rayon et tri. */
@Data
public class CritereRechercheDTO {
	private Double lat;
	private Double lon;
	private Integer rayon;
	private String ville;
	private String carburant;
	private TypeTri tri = TypeTri.PRIX;
}

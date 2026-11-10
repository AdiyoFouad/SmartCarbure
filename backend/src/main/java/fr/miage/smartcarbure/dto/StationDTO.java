package fr.miage.smartcarbure.dto;

import java.util.List;

import fr.miage.smartcarbure.model.Position;
import fr.miage.smartcarbure.model.PrixCarburant;
import fr.miage.smartcarbure.model.Service;
import fr.miage.smartcarbure.model.TypeEmplacement;
import fr.miage.smartcarbure.model.Ville;
import lombok.Data;

/** Station renvoyée au frontend au format JSON du contrat d'API. */
@Data
public class StationDTO {
	private String id;
	private String nom;
	private Position position;
	private Ville ville;
	private TypeEmplacement emplacement;
    private boolean automate24h;
    ///TODO (Fouad): remplacer List<?> par List<Horaires> quand la classe Horaire sera créée
    private List<?> horaires;
	private List<Service> services;
	private List<PrixCarburant> carburants;
	private Double distanceKm;
}

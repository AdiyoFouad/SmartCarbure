package fr.miage.smartcarbure.dto;

import java.util.List;
import lombok.Data;

/**
 * Objet « trajet » de la réponse de GET /api/trajet : départ, arrivée, points de l'itinéraire
 * (dans l'ordre, du départ à l'arrivée) et longueur en km arrondie à une décimale.
 */
@Data
public class TrajetDTO {
    private PositionDTO depart;
    private PositionDTO arrivee;
    private List<PositionDTO> points;
    private Double distanceKm;
}

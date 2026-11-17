package fr.miage.smartcarbure.dto;

import java.util.List;
import lombok.Data;

/**
 * Réponse de GET /api/trajet (F6) : l'itinéraire à tracer sur la carte et les stations trouvées
 * le long de la route, triées par prix, avec pour chacune sa distance à la route (distanceKm).
 */
@Data
public class TrajetResponse {
    private TrajetDTO trajet;
    private List<StationDTO> stations;
}

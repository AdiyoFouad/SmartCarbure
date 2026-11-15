package fr.miage.smartcarbure.model;

import java.util.Objects;

/**
 * Station trouvée le long d'un trajet, avec sa distance à la route en kilomètres
 * (champ distanceKm de GET /api/trajet dans le contrat d'API).
 *
 * @param station    la station (obligatoire)
 * @param distanceKm distance entre la station et le point le plus proche de la route, en km
 */
public record StationSurTrajet(Station station, double distanceKm) {

    public StationSurTrajet {
        Objects.requireNonNull(station, "La station est obligatoire.");
    }
}

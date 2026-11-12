package fr.miage.smartcarbure.service;

import java.util.Objects;

import org.springframework.stereotype.Component;

import fr.miage.smartcarbure.model.Position;

/** Calcule la distance à vol d'oiseau entre deux positions GPS, en kilomètres. */
@Component
public class CalculDistance {

    private static final double RAYON_TERRESTRE_KM = 6_371.0088;

    /**
     * Calcule la distance orthodromique entre deux positions avec la formule de Haversine.
     *
     * @throws NullPointerException si une position est null
     * @throws IllegalArgumentException si une position ne contient pas de coordonnées GPS
     */
    public double calculerDistance(Position origine, Position destination) {
        Objects.requireNonNull(origine, "La position d'origine est obligatoire.");
        Objects.requireNonNull(destination, "La position de destination est obligatoire.");

        double haversine = getHaversine(origine, destination);
        double angle = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
        return RAYON_TERRESTRE_KM * angle;
    }

    private static double getHaversine(Position origine, Position destination) {
        double latitudeOrigine = Math.toRadians(origine.getLatitude());
        double latitudeDestination = Math.toRadians(destination.getLatitude());
        double differenceLatitude = latitudeDestination - latitudeOrigine;
        double differenceLongitude = Math.toRadians(destination.getLongitude() - origine.getLongitude());

        double haversine = Math.pow(Math.sin(differenceLatitude / 2), 2)
                + Math.cos(latitudeOrigine) * Math.cos(latitudeDestination)
                * Math.pow(Math.sin(differenceLongitude / 2), 2);
        return haversine;
    }

}

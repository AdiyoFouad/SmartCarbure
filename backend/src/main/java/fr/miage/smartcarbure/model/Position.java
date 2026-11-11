package fr.miage.smartcarbure.model;

import lombok.Getter;

import java.util.Objects;

/**
 * Coordonnées GPS (latitude, longitude en degrés décimaux) et adresse d'une station ou d'un point de trajet.
 * Il faut au moins une adresse ou les deux coordonnées. Une coordonnée seule est refusée.
 */
@Getter
public class Position {

    private final Double latitude;
    private final Double longitude;
    private final String adresse;

    public Position(Double latitude, Double longitude, String adresse) {
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException("La latitude et la longitude doivent être fournies ensemble.");
        }
        if (latitude == null && adresse == null) {
            throw new IllegalArgumentException("Une position doit avoir au moins une adresse ou des coordonnées.");
        }
        this.latitude = latitude;
        this.longitude = longitude;
        this.adresse = adresse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Position autre)) {
            return false;
        }
        return Objects.equals(latitude, autre.latitude)
                && Objects.equals(longitude, autre.longitude)
                && Objects.equals(adresse, autre.adresse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude, adresse);
    }

    @Override
    public String toString() {
        return "Position{latitude=" + latitude + ", longitude=" + longitude + ", adresse='" + adresse + "'}";
    }
}

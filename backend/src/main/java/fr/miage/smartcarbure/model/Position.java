package fr.miage.smartcarbure.model;

/**
 * Coordonnées GPS (latitude, longitude en degrés décimaux) et adresse d'une station ou d'un point de trajet.
 * Latitude et longitude peuvent être null quand seule l'adresse est connue.
 */
public class Position {

    private Double latitude;
    private Double longitude;
    private String adresse;

    public Position() {
    }

    public Position(Double latitude, Double longitude, String adresse) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.adresse = adresse;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    @Override
    public String toString() {
        return "Position{latitude=" + latitude + ", longitude=" + longitude + ", adresse='" + adresse + "'}";
    }
}

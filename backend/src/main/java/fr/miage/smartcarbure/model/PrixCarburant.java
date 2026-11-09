package fr.miage.smartcarbure.model;

import java.time.LocalDateTime;

/**
 * Classe d'association entre une station et un carburant : prix, date de mise à jour,
 * disponibilité, type et début de rupture.
 * Le prix peut être null (carburant en rupture sans prix connu).
 * Les dates sont à l'heure de Paris (voir StationJsonConverter).
 */
public class PrixCarburant {

    private Carburant carburant;
    private Double prix;
    private LocalDateTime dateMaj;
    private boolean disponible;
    private TypeRupture typeRupture;
    private LocalDateTime debutRupture;

    public PrixCarburant() {
    }

    public PrixCarburant(Carburant carburant, Double prix, LocalDateTime dateMaj,
                         boolean disponible, TypeRupture typeRupture, LocalDateTime debutRupture) {
        this.carburant = carburant;
        this.prix = prix;
        this.dateMaj = dateMaj;
        this.disponible = disponible;
        this.typeRupture = typeRupture;
        this.debutRupture = debutRupture;
    }

    public Carburant getCarburant() {
        return carburant;
    }

    public void setCarburant(Carburant carburant) {
        this.carburant = carburant;
    }

    public Double getPrix() {
        return prix;
    }

    public void setPrix(Double prix) {
        this.prix = prix;
    }

    public LocalDateTime getDateMaj() {
        return dateMaj;
    }

    public void setDateMaj(LocalDateTime dateMaj) {
        this.dateMaj = dateMaj;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public TypeRupture getTypeRupture() {
        return typeRupture;
    }

    public void setTypeRupture(TypeRupture typeRupture) {
        this.typeRupture = typeRupture;
    }

    public LocalDateTime getDebutRupture() {
        return debutRupture;
    }

    public void setDebutRupture(LocalDateTime debutRupture) {
        this.debutRupture = debutRupture;
    }

    @Override
    public String toString() {
        return "PrixCarburant{carburant=" + carburant + ", prix=" + prix + ", dateMaj=" + dateMaj
                + ", disponible=" + disponible + ", typeRupture=" + typeRupture
                + ", debutRupture=" + debutRupture + "}";
    }
}

package fr.miage.smartcarbure.model;

import fr.miage.smartcarbure.model.enums.Carburant;
import fr.miage.smartcarbure.model.enums.TypeRupture;
import java.time.LocalDateTime;

/**
 * Classe d'association entre une station et un carburant : prix, date de mise à jour,
 * disponibilité, type et début de rupture.
 * Le carburant est obligatoire. Le prix peut être null (carburant en rupture sans prix connu).
 * Les dates sont à l'heure de Paris (voir StationJsonConverter).
 *
 * <p>Création : {@code PrixCarburant.builder().carburant(Carburant.GAZOLE).prix(1.789).build()}</p>
 */
public class PrixCarburant {

    private final Carburant carburant;
    private final Double prix;
    private final LocalDateTime dateMaj;
    private final boolean disponible;
    private final TypeRupture typeRupture;
    private final LocalDateTime debutRupture;

    private PrixCarburant(Builder builder) {
        this.carburant = builder.carburant;
        this.prix = builder.prix;
        this.dateMaj = builder.dateMaj;
        this.disponible = builder.disponible;
        this.typeRupture = builder.typeRupture;
        this.debutRupture = builder.debutRupture;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Carburant getCarburant() {
        return carburant;
    }

    public Double getPrix() {
        return prix;
    }

    public LocalDateTime getDateMaj() {
        return dateMaj;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public TypeRupture getTypeRupture() {
        return typeRupture;
    }

    public LocalDateTime getDebutRupture() {
        return debutRupture;
    }

    @Override
    public String toString() {
        return "PrixCarburant{carburant=" + carburant + ", prix=" + prix + ", dateMaj=" + dateMaj
                + ", disponible=" + disponible + ", typeRupture=" + typeRupture
                + ", debutRupture=" + debutRupture + "}";
    }

    /** Construit un PrixCarburant. Le carburant est obligatoire ; disponible vaut true par défaut. */
    public static class Builder {

        private Carburant carburant;
        private Double prix;
        private LocalDateTime dateMaj;
        private boolean disponible = true;
        private TypeRupture typeRupture;
        private LocalDateTime debutRupture;

        private Builder() {
        }

        public Builder carburant(Carburant carburant) {
            this.carburant = carburant;
            return this;
        }

        public Builder prix(Double prix) {
            this.prix = prix;
            return this;
        }

        public Builder dateMaj(LocalDateTime dateMaj) {
            this.dateMaj = dateMaj;
            return this;
        }

        public Builder disponible(boolean disponible) {
            this.disponible = disponible;
            return this;
        }

        public Builder typeRupture(TypeRupture typeRupture) {
            this.typeRupture = typeRupture;
            return this;
        }

        public Builder debutRupture(LocalDateTime debutRupture) {
            this.debutRupture = debutRupture;
            return this;
        }

        public PrixCarburant build() {
            if (carburant == null) {
                throw new IllegalStateException("Le carburant est obligatoire pour créer un PrixCarburant.");
            }
            return new PrixCarburant(this);
        }
    }
}

package fr.miage.smartcarbure.model;

import lombok.Getter;

import java.util.Objects;

/** Service proposé par une station (lavage, boutique, gonflage...), avec le libellé fourni par l'API. */
@Getter
public class ServiceStation {

    private final String nom;

    public ServiceStation(String nom) {
        this.nom = Objects.requireNonNull(nom, "Le nom du service est obligatoire.");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceStation autre)) {
            return false;
        }
        return nom.equals(autre.nom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nom);
    }

    @Override
    public String toString() {
        return "Service{nom='" + nom + "'}";
    }
}

package fr.miage.smartcarbure.model;

import java.util.Objects;

/** Service proposé par une station (lavage, boutique, gonflage...), avec le libellé fourni par l'API. */
public class Service {

    private final String nom;

    public Service(String nom) {
        this.nom = Objects.requireNonNull(nom, "Le nom du service est obligatoire.");
    }

    public String getNom() {
        return nom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Service autre)) {
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

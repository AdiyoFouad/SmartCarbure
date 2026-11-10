package fr.miage.smartcarbure.model;

import java.util.Objects;

/**
 * Ville et code postal auxquels appartient une station ; la ville appartient à un département.
 * Il faut au moins un nom ou un code postal ; le département peut être null.
 */
public class Ville {

    private final String nom;
    private final String codePostal;
    private final Departement departement;

    public Ville(String nom, String codePostal, Departement departement) {
        if (nom == null && codePostal == null) {
            throw new IllegalArgumentException("Une ville doit avoir au moins un nom ou un code postal.");
        }
        this.nom = nom;
        this.codePostal = codePostal;
        this.departement = departement;
    }

    public String getNom() {
        return nom;
    }

    public String getCodePostal() {
        return codePostal;
    }

    public Departement getDepartement() {
        return departement;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ville autre)) {
            return false;
        }
        return Objects.equals(nom, autre.nom)
                && Objects.equals(codePostal, autre.codePostal)
                && Objects.equals(departement, autre.departement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nom, codePostal, departement);
    }

    @Override
    public String toString() {
        return "Ville{nom='" + nom + "', codePostal='" + codePostal + "', departement=" + departement + "}";
    }
}

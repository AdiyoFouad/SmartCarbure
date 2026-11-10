package fr.miage.smartcarbure.model;

import java.util.Objects;

/**
 * Département (code et nom) qui regroupe des villes ; il appartient à une région.
 * Le code et le nom sont obligatoires ; la région peut être null.
 */
public class Departement {

    private final String code;
    private final String nom;
    private final Region region;

    public Departement(String code, String nom, Region region) {
        this.code = Objects.requireNonNull(code, "Le code du département est obligatoire.");
        this.nom = Objects.requireNonNull(nom, "Le nom du département est obligatoire.");
        this.region = region;
    }

    public String getCode() {
        return code;
    }

    public String getNom() {
        return nom;
    }

    public Region getRegion() {
        return region;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Departement autre)) {
            return false;
        }
        return code.equals(autre.code) && nom.equals(autre.nom) && Objects.equals(region, autre.region);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, nom, region);
    }

    @Override
    public String toString() {
        return "Departement{code='" + code + "', nom='" + nom + "', region=" + region + "}";
    }
}

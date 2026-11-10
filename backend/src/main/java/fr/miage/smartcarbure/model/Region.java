package fr.miage.smartcarbure.model;

import java.util.Objects;

/** Région (code et nom) qui regroupe des départements. Le code et le nom sont obligatoires. */
public class Region {

    private final String code;
    private final String nom;

    public Region(String code, String nom) {
        this.code = Objects.requireNonNull(code, "Le code de la région est obligatoire.");
        this.nom = Objects.requireNonNull(nom, "Le nom de la région est obligatoire.");
    }

    public String getCode() {
        return code;
    }

    public String getNom() {
        return nom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Region autre)) {
            return false;
        }
        return code.equals(autre.code) && nom.equals(autre.nom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, nom);
    }

    @Override
    public String toString() {
        return "Region{code='" + code + "', nom='" + nom + "'}";
    }
}

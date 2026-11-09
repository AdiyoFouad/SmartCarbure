package fr.miage.smartcarbure.model;

/** Département (code et nom) qui regroupe des villes ; il appartient à une région. */
public class Departement {

    private String code;
    private String nom;
    private Region region;

    public Departement() {
    }

    public Departement(String code, String nom, Region region) {
        this.code = code;
        this.nom = nom;
        this.region = region;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    @Override
    public String toString() {
        return "Departement{code='" + code + "', nom='" + nom + "', region=" + region + "}";
    }
}

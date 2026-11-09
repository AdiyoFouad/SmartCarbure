package fr.miage.smartcarbure.model;

/** Ville et code postal auxquels appartient une station ; la ville appartient à un département. */
public class Ville {

    private String nom;
    private String codePostal;
    private Departement departement;

    public Ville() {
    }

    public Ville(String nom, String codePostal, Departement departement) {
        this.nom = nom;
        this.codePostal = codePostal;
        this.departement = departement;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getCodePostal() {
        return codePostal;
    }

    public void setCodePostal(String codePostal) {
        this.codePostal = codePostal;
    }

    public Departement getDepartement() {
        return departement;
    }

    public void setDepartement(Departement departement) {
        this.departement = departement;
    }

    @Override
    public String toString() {
        return "Ville{nom='" + nom + "', codePostal='" + codePostal + "', departement=" + departement + "}";
    }
}

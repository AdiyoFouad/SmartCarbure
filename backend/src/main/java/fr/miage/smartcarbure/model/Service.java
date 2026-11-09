package fr.miage.smartcarbure.model;

/** Service proposé par une station (lavage, boutique, gonflage...), avec le libellé fourni par l'API. */
public class Service {

    private String nom;

    public Service() {
    }

    public Service(String nom) {
        this.nom = nom;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    @Override
    public String toString() {
        return "Service{nom='" + nom + "'}";
    }
}

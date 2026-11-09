package fr.miage.smartcarbure.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Point de vente de carburant lu depuis l'API Prix des carburants.
 * Une station est rattachée à une ville, elle-même rattachée à un département puis à une région.
 * Le nom vaut toujours null pour l'instant : l'API ne fournit pas de nom de station.
 */
public class Station {

    private String id;
    private String nom;
    private Position position;
    private Ville ville;
    private TypeEmplacement emplacement;
    private boolean automate24h;
    private List<Horaire> horaires = new ArrayList<>();
    private List<Service> services = new ArrayList<>();
    private List<PrixCarburant> carburants = new ArrayList<>();

    public Station() {
    }

    public Station(String id, Position position, Ville ville, TypeEmplacement emplacement, boolean automate24h) {
        this.id = id;
        this.position = position;
        this.ville = ville;
        this.emplacement = emplacement;
        this.automate24h = automate24h;
    }

    /** Département de la station, en passant par sa ville ; null si un maillon manque. */
    public Departement getDepartement() {
        return ville == null ? null : ville.getDepartement();
    }

    /** Région de la station, en passant par sa ville et son département ; null si un maillon manque. */
    public Region getRegion() {
        Departement departement = getDepartement();
        return departement == null ? null : departement.getRegion();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Ville getVille() {
        return ville;
    }

    public void setVille(Ville ville) {
        this.ville = ville;
    }

    public TypeEmplacement getEmplacement() {
        return emplacement;
    }

    public void setEmplacement(TypeEmplacement emplacement) {
        this.emplacement = emplacement;
    }

    public boolean isAutomate24h() {
        return automate24h;
    }

    public void setAutomate24h(boolean automate24h) {
        this.automate24h = automate24h;
    }

    public List<Horaire> getHoraires() {
        return horaires;
    }

    public void setHoraires(List<Horaire> horaires) {
        this.horaires = horaires == null ? new ArrayList<>() : horaires;
    }

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services == null ? new ArrayList<>() : services;
    }

    public List<PrixCarburant> getCarburants() {
        return carburants;
    }

    public void setCarburants(List<PrixCarburant> carburants) {
        this.carburants = carburants == null ? new ArrayList<>() : carburants;
    }

    @Override
    public String toString() {
        return "Station{id='" + id + "', nom='" + nom + "', position=" + position + ", ville=" + ville
                + ", emplacement=" + emplacement + ", automate24h=" + automate24h
                + ", horaires=" + horaires + ", services=" + services + ", carburants=" + carburants + "}";
    }
}

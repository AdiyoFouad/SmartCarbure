package fr.miage.smartcarbure.model;

import fr.miage.smartcarbure.model.enums.TypeEmplacement;
import lombok.Getter;

import java.util.List;

/**
 * Point de vente de carburant lu depuis l'API Prix des carburants.
 * Une station est rattachée à une ville, elle-même rattachée à un département puis à une région.
 * L'identifiant est obligatoire. Le nom vaut toujours null pour l'instant : l'API ne fournit pas de nom de station.
 * Les listes (horaires, services, carburants) ne sont jamais null et ne sont pas modifiables.
 *
 * <p>Création : {@code Station.builder().id("35200008").position(p).ville(v).build()}</p>
 */
@Getter
public class Station {

    private final String id;
    private final String nom;
    private final Position position;
    private final Ville ville;
    private final TypeEmplacement emplacement;
    private final boolean automate24h;
    private final List<Horaire> horaires;
    private final List<Service> services;
    private final List<PrixCarburant> carburants;

    private Station(Builder builder) {
        this.id = builder.id;
        this.nom = builder.nom;
        this.position = builder.position;
        this.ville = builder.ville;
        this.emplacement = builder.emplacement;
        this.automate24h = builder.automate24h;
        this.horaires = List.copyOf(builder.horaires);
        this.services = List.copyOf(builder.services);
        this.carburants = List.copyOf(builder.carburants);
    }

    public static Builder builder() {
        return new Builder();
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

    @Override
    public String toString() {
        return "Station{id='" + id + "', nom='" + nom + "', position=" + position + ", ville=" + ville
                + ", emplacement=" + emplacement + ", automate24h=" + automate24h
                + ", horaires=" + horaires + ", services=" + services + ", carburants=" + carburants + "}";
    }

    /** Construit une Station. L'identifiant est obligatoire ; les listes sont vides par défaut. */
    public static class Builder {

        private String id;
        private String nom;
        private Position position;
        private Ville ville;
        private TypeEmplacement emplacement;
        private boolean automate24h;
        private List<Horaire> horaires = List.of();
        private List<Service> services = List.of();
        private List<PrixCarburant> carburants = List.of();

        private Builder() {
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder nom(String nom) {
            this.nom = nom;
            return this;
        }

        public Builder position(Position position) {
            this.position = position;
            return this;
        }

        public Builder ville(Ville ville) {
            this.ville = ville;
            return this;
        }

        public Builder emplacement(TypeEmplacement emplacement) {
            this.emplacement = emplacement;
            return this;
        }

        public Builder automate24h(boolean automate24h) {
            this.automate24h = automate24h;
            return this;
        }

        /** Liste des horaires ; null est traité comme une liste vide. */
        public Builder horaires(List<Horaire> horaires) {
            this.horaires = horaires == null ? List.of() : horaires;
            return this;
        }

        /** Liste des services ; null est traité comme une liste vide. */
        public Builder services(List<Service> services) {
            this.services = services == null ? List.of() : services;
            return this;
        }

        /** Liste des carburants ; null est traité comme une liste vide. */
        public Builder carburants(List<PrixCarburant> carburants) {
            this.carburants = carburants == null ? List.of() : carburants;
            return this;
        }

        public Station build() {
            if (id == null) {
                throw new IllegalStateException("L'identifiant est obligatoire pour créer une Station.");
            }
            return new Station(this);
        }
    }
}

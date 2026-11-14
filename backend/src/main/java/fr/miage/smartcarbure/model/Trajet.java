package fr.miage.smartcarbure.model;

import lombok.Getter;

import java.util.List;
import java.util.Objects;

/**
 * Itinéraire entre une position de départ et une position d'arrivée, calculé par le service
 * d'itinéraire à chaque recherche (jamais enregistré en base).
 *
 * <p>Les points décrivent la route dans l'ordre, du départ à l'arrivée. Il en faut au moins deux,
 * et chacun doit avoir une latitude et une longitude. La liste n'est pas modifiable.</p>
 */
@Getter
public class Trajet {

    private final Position depart;
    private final Position arrivee;
    private final List<Position> points;
    private final double distanceKm;

    /**
     * @param depart     position de départ (avec coordonnées)
     * @param arrivee    position d'arrivée (avec coordonnées)
     * @param points     points de la route, du départ à l'arrivée (au moins deux, tous avec coordonnées)
     * @param distanceKm longueur de la route en kilomètres (positive ou nulle)
     * @throws NullPointerException     si le départ, l'arrivée ou la liste des points est null
     * @throws IllegalArgumentException si une position n'a pas de coordonnées, s'il y a moins de deux points
     *                                  ou si la distance est négative ou invalide
     */
    public Trajet(Position depart, Position arrivee, List<Position> points, double distanceKm) {
        this.depart = verifierCoordonnees(Objects.requireNonNull(depart, "Le départ est obligatoire."), "Le départ");
        this.arrivee = verifierCoordonnees(Objects.requireNonNull(arrivee, "L'arrivée est obligatoire."), "L'arrivée");
        Objects.requireNonNull(points, "Les points du trajet sont obligatoires.");
        if (points.size() < 2) {
            throw new IllegalArgumentException("Un trajet doit avoir au moins deux points.");
        }
        for (Position point : points) {
            verifierCoordonnees(Objects.requireNonNull(point, "Un point du trajet est null."), "Chaque point du trajet");
        }
        if (!Double.isFinite(distanceKm) || distanceKm < 0) {
            throw new IllegalArgumentException("La distance du trajet doit être positive : " + distanceKm);
        }
        this.points = List.copyOf(points);
        this.distanceKm = distanceKm;
    }

    private static Position verifierCoordonnees(Position position, String libelle) {
        if (position.getLatitude() == null || position.getLongitude() == null) {
            throw new IllegalArgumentException(libelle + " doit avoir une latitude et une longitude.");
        }
        return position;
    }

    @Override
    public String toString() {
        return "Trajet{depart=" + depart + ", arrivee=" + arrivee + ", points=" + points.size()
                + ", distanceKm=" + distanceKm + "}";
    }
}

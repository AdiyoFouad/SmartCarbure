package fr.miage.smartcarbure.service;

import fr.miage.smartcarbure.client.CarburantApiClient;
import fr.miage.smartcarbure.enums.Carburant;
import fr.miage.smartcarbure.model.Position;
import fr.miage.smartcarbure.model.PrixCarburant;
import fr.miage.smartcarbure.model.Station;
import fr.miage.smartcarbure.model.StationSurTrajet;
import fr.miage.smartcarbure.model.Trajet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Recherche des stations le long d'un trajet (F6). Méthode décrite dans docs/etude-trajet.md :
 * <ol>
 *   <li>on place des points tous les {@value #ESPACEMENT_POINTS_KM} km le long de la route ;</li>
 *   <li>on récupère les stations dans un cercle autour de chaque point, tronçon par tronçon
 *       (environ 60 km par recherche, voir {@link CarburantApiClient#rechercherAutourDePoints}),
 *       puis on supprime les doublons ;</li>
 *   <li>on ne garde que les stations à moins de {@code rayonKm} de la route, qui vendent le carburant demandé ;</li>
 *   <li>on trie par prix ; les stations en rupture sur ce carburant sont placées en fin de liste.</li>
 * </ol>
 *
 * <p>Le trajet lui-même (les points de la route) est fourni par le service d'itinéraire : cette classe
 * ne dépend pas du service choisi (OpenRouteService, OSRM…).</p>
 */
@Service
public class TrajetService {

    /** Distance maximale à la route utilisée quand l'utilisateur n'en donne pas. */
    public static final double RAYON_PAR_DEFAUT_KM = 5.0;
    /** Distance maximale à la route la plus petite acceptée. */
    public static final double RAYON_MIN_KM = 1.0;
    /** Distance maximale à la route la plus grande acceptée. */
    public static final double RAYON_MAX_KM = 10.0;
    /** Longueur maximale d'un trajet accepté. */
    public static final double LONGUEUR_MAX_KM = 500.0;
    /** Espacement des points placés le long de la route. */
    public static final double ESPACEMENT_POINTS_KM = 3.0;
    /**
     * Nombre de points par tronçon (20 points espacés de 3 km : environ 60 km de route).
     * CarburantApiClient renvoie au plus max-resultats (500) stations par recherche, rangées par id :
     * une recherche par tronçon garantit qu'aucune station n'est perdue au hasard, même en ville
     * avec un rayon de 10 km.
     */
    static final int POINTS_PAR_TRONCON = 20;

    /** Kilomètres par degré de latitude (rayon terrestre moyen de CalculDistance). */
    private static final double KM_PAR_DEGRE = 6_371.0088 * Math.PI / 180;

    private final CarburantApiClient carburantApiClient;
    private final CalculDistance calculDistance;

    public TrajetService(CarburantApiClient carburantApiClient, CalculDistance calculDistance) {
        this.carburantApiClient = carburantApiClient;
        this.calculDistance = calculDistance;
    }

    /**
     * Stations situées à moins de rayonKm de la route et qui vendent le carburant demandé.
     * Les stations disponibles sont triées du prix le plus bas au plus élevé (à prix égal, la plus
     * proche de la route d'abord) ; les stations en rupture suivent, de la plus proche à la plus éloignée.
     *
     * @param trajet    le trajet calculé par le service d'itinéraire
     * @param rayonKm   distance maximale à la route en km, de 1 à 10 ; null pour 5 km
     * @param carburant carburant recherché
     * @return les stations le long du trajet, sans doublon ; liste vide si aucune
     * @throws NullPointerException     si le trajet ou le carburant est null
     * @throws IllegalArgumentException si le rayon est hors limites ou si le trajet dépasse 500 km
     * @throws fr.miage.smartcarbure.exception.ApiIndisponibleException si l'API des carburants ne répond pas
     */
    public List<StationSurTrajet> rechercherLeLongDuTrajet(Trajet trajet, Double rayonKm, Carburant carburant) {
        Objects.requireNonNull(trajet, "Le trajet est obligatoire.");
        Objects.requireNonNull(carburant, "Le carburant est obligatoire.");
        double rayon = verifierRayon(rayonKm);
        if (trajet.getDistanceKm() > LONGUEUR_MAX_KM) {
            throw new IllegalArgumentException("Le trajet dépasse " + (int) LONGUEUR_MAX_KM + " km.");
        }

        List<Position> echantillons = echantillonner(trajet.getPoints(), ESPACEMENT_POINTS_KM);
        Map<String, Station> candidates = new LinkedHashMap<>();
        for (List<Position> troncon : decouperEnTroncons(echantillons, POINTS_PAR_TRONCON)) {
            for (Station station : carburantApiClient.rechercherAutourDePoints(troncon, rayonRecherche(rayon))) {
                candidates.putIfAbsent(station.getId(), station);
            }
        }

        List<StationSurTrajet> resultats = new ArrayList<>();
        for (Station station : candidates.values()) {
            Position position = station.getPosition();
            if (position == null || position.getLatitude() == null || prixDe(station, carburant).isEmpty()) {
                continue;
            }
            double distance = distanceALaRoute(position, trajet.getPoints());
            if (distance <= rayon) {
                resultats.add(new StationSurTrajet(station, distance));
            }
        }
        resultats.sort(ordre(carburant));
        return resultats;
    }

    /**
     * Taille des cercles de recherche autour des points : la distance à la route plus la moitié
     * de l'espacement. Une station proche de la route est ainsi toujours dans au moins un cercle,
     * même dans les virages.
     */
    static double rayonRecherche(double rayonKm) {
        return rayonKm + ESPACEMENT_POINTS_KM / 2;
    }

    /**
     * Points placés tous les espacementKm le long de la route, en commençant par le départ
     * et en finissant par l'arrivée.
     */
    List<Position> echantillonner(List<Position> route, double espacementKm) {
        List<Position> echantillons = new ArrayList<>();
        echantillons.add(route.get(0));
        double parcouruDepuisDernier = 0;
        for (int i = 1; i < route.size(); i++) {
            Position a = route.get(i - 1);
            Position b = route.get(i);
            double longueur = calculDistance.calculerDistance(a, b);
            double position = espacementKm - parcouruDepuisDernier;
            while (position <= longueur) {
                echantillons.add(interpoler(a, b, position / longueur));
                position += espacementKm;
            }
            parcouruDepuisDernier = longueur - (position - espacementKm);
        }
        Position arrivee = route.get(route.size() - 1);
        if (!echantillons.get(echantillons.size() - 1).equals(arrivee)) {
            echantillons.add(arrivee);
        }
        return echantillons;
    }

    /** Découpe la liste de points en tronçons d'au plus taille points, dans l'ordre de la route. */
    static List<List<Position>> decouperEnTroncons(List<Position> points, int taille) {
        List<List<Position>> troncons = new ArrayList<>();
        for (int debut = 0; debut < points.size(); debut += taille) {
            troncons.add(points.subList(debut, Math.min(debut + taille, points.size())));
        }
        return troncons;
    }

    /** Distance en km entre une position et le point le plus proche de la route. */
    static double distanceALaRoute(Position position, List<Position> route) {
        double minimum = Double.MAX_VALUE;
        for (int i = 1; i < route.size(); i++) {
            minimum = Math.min(minimum, distanceAuSegment(position, route.get(i - 1), route.get(i)));
        }
        return minimum;
    }

    /**
     * Distance en km entre un point p et le segment [a, b]. Les positions sont projetées sur un plan
     * centré sur p (précis à quelques mètres près pour des distances de quelques kilomètres).
     */
    static double distanceAuSegment(Position p, Position a, Position b) {
        double cosLatitude = Math.cos(Math.toRadians(p.getLatitude()));
        double ax = (a.getLongitude() - p.getLongitude()) * cosLatitude * KM_PAR_DEGRE;
        double ay = (a.getLatitude() - p.getLatitude()) * KM_PAR_DEGRE;
        double bx = (b.getLongitude() - p.getLongitude()) * cosLatitude * KM_PAR_DEGRE;
        double by = (b.getLatitude() - p.getLatitude()) * KM_PAR_DEGRE;
        double dx = bx - ax;
        double dy = by - ay;
        double longueurCarree = dx * dx + dy * dy;
        double t = longueurCarree == 0 ? 0 : Math.max(0, Math.min(1, -(ax * dx + ay * dy) / longueurCarree));
        double x = ax + t * dx;
        double y = ay + t * dy;
        return Math.sqrt(x * x + y * y);
    }

    private static Position interpoler(Position a, Position b, double fraction) {
        double latitude = a.getLatitude() + (b.getLatitude() - a.getLatitude()) * fraction;
        double longitude = a.getLongitude() + (b.getLongitude() - a.getLongitude()) * fraction;
        return new Position(latitude, longitude, null);
    }

    private static double verifierRayon(Double rayonKm) {
        if (rayonKm == null) {
            return RAYON_PAR_DEFAUT_KM;
        }
        if (!Double.isFinite(rayonKm) || rayonKm < RAYON_MIN_KM || rayonKm > RAYON_MAX_KM) {
            throw new IllegalArgumentException("La distance à la route doit être comprise entre "
                    + (int) RAYON_MIN_KM + " et " + (int) RAYON_MAX_KM + " km : " + rayonKm);
        }
        return rayonKm;
    }

    private static Optional<PrixCarburant> prixDe(Station station, Carburant carburant) {
        return station.getCarburants().stream().filter(p -> p.getCarburant() == carburant).findFirst();
    }

    /** Disponibles d'abord (prix croissant, puis distance), ruptures ensuite (distance croissante). */
    private static Comparator<StationSurTrajet> ordre(Carburant carburant) {
        return Comparator
                .comparing((StationSurTrajet s) -> !estDisponible(s, carburant))
                .thenComparing(s -> estDisponible(s, carburant) ? prixDe(s.station(), carburant).get().getPrix() : 0.0)
                .thenComparingDouble(StationSurTrajet::distanceKm);
    }

    /** Disponible = vendu, pas en rupture et avec un prix connu. */
    private static boolean estDisponible(StationSurTrajet s, Carburant carburant) {
        return prixDe(s.station(), carburant)
                .map(p -> p.isDisponible() && p.getPrix() != null)
                .orElse(false);
    }
}

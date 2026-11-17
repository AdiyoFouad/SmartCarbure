package fr.miage.smartcarbure.controller;

import fr.miage.smartcarbure.client.ItineraireClient;
import fr.miage.smartcarbure.dto.PositionDTO;
import fr.miage.smartcarbure.dto.StationDTO;
import fr.miage.smartcarbure.dto.TrajetDTO;
import fr.miage.smartcarbure.dto.TrajetResponse;
import fr.miage.smartcarbure.enums.Carburant;
import fr.miage.smartcarbure.mapper.PositionMapper;
import fr.miage.smartcarbure.mapper.StationMapper;
import fr.miage.smartcarbure.model.Position;
import fr.miage.smartcarbure.model.StationSurTrajet;
import fr.miage.smartcarbure.model.Trajet;
import fr.miage.smartcarbure.service.TrajetService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expose la recherche des stations le long d'un trajet (F6) : GET /api/trajet.
 *
 * <p>Exemple : {@code /api/trajet?depart=48.1173,-1.6778&arrivee=47.2184,-1.5536&carburant=SP95&rayon=5}</p>
 *
 * <p>Erreurs (transformées en { statut, message } par GlobalExceptionHandler) :</p>
 * <ul>
 *   <li>400 : paramètre absent (dont carburant), coordonnées mal formées ou hors limites, carburant inconnu,
 *       rayon hors de 1 à 10 km, trajet de plus de 500 km, aucun itinéraire routier entre les deux points ;</li>
 *   <li>503 : service d'itinéraire ou API Prix des carburants indisponible (clé absente, quota, panne).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/trajet")
@RequiredArgsConstructor
public class TrajetController {

    private static final Logger LOG = LoggerFactory.getLogger(TrajetController.class);

    private final ItineraireClient itineraireClient;
    private final TrajetService trajetService;
    private final StationMapper stationMapper;
    private final PositionMapper positionMapper;

    @GetMapping
    public TrajetResponse rechercherLeLongDuTrajet(
            @RequestParam("depart") String depart,
            @RequestParam("arrivee") String arrivee,
            @RequestParam("carburant") Carburant carburant,
            @RequestParam(name = "rayon", required = false) Double rayon) {
        Position positionDepart = lirePosition(depart, "depart");
        Position positionArrivee = lirePosition(arrivee, "arrivee");
        // Vérifié avant d'appeler le service d'itinéraire, pour ne pas consommer de quota pour rien.
        verifierRayon(rayon);

        long debut = System.currentTimeMillis();
        Trajet trajet = itineraireClient.calculerTrajet(positionDepart, positionArrivee);
        List<StationSurTrajet> stations = trajetService.rechercherLeLongDuTrajet(trajet, rayon, carburant);
        LOG.info("Trajet de {} km : {} station(s) trouvée(s) en {} ms",
                arrondir(trajet.getDistanceKm()), stations.size(), System.currentTimeMillis() - debut);

        TrajetResponse reponse = new TrajetResponse();
        reponse.setTrajet(versDTO(trajet));
        reponse.setStations(stations.stream().map(this::versDTO).toList());
        return reponse;
    }

    /** Lit « latitude,longitude » (par exemple « 48.1173,-1.6778 »). */
    static Position lirePosition(String valeur, String parametre) {
        String[] parties = valeur.split(",");
        if (parties.length != 2) {
            throw new IllegalArgumentException(
                    "Le paramètre " + parametre + " doit être de la forme latitude,longitude : " + valeur + ".");
        }
        double latitude;
        double longitude;
        try {
            latitude = Double.parseDouble(parties[0].trim());
            longitude = Double.parseDouble(parties[1].trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Le paramètre " + parametre + " doit contenir deux nombres : " + valeur + ".", e);
        }
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90
                || !Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Coordonnées hors limites pour " + parametre + " : " + valeur
                    + " (latitude de -90 à 90, longitude de -180 à 180).");
        }
        return new Position(latitude, longitude, null);
    }

    private static void verifierRayon(Double rayon) {
        if (rayon != null && (!Double.isFinite(rayon)
                || rayon < TrajetService.RAYON_MIN_KM || rayon > TrajetService.RAYON_MAX_KM)) {
            throw new IllegalArgumentException("Le rayon doit être compris entre "
                    + (int) TrajetService.RAYON_MIN_KM + " et " + (int) TrajetService.RAYON_MAX_KM + " km.");
        }
    }

    private TrajetDTO versDTO(Trajet trajet) {
        TrajetDTO dto = new TrajetDTO();
        dto.setDepart(positionMapper.toDTO(trajet.getDepart()));
        dto.setArrivee(positionMapper.toDTO(trajet.getArrivee()));
        List<PositionDTO> points = trajet.getPoints().stream().map(positionMapper::toDTO).toList();
        dto.setPoints(points);
        dto.setDistanceKm(arrondir(trajet.getDistanceKm()));
        return dto;
    }

    private StationDTO versDTO(StationSurTrajet stationSurTrajet) {
        StationDTO dto = stationMapper.toDTO(stationSurTrajet.station());
        dto.setDistanceKm(arrondir(stationSurTrajet.distanceKm()));
        return dto;
    }

    /** Distance arrondie à une décimale, comme le demande le contrat (3.24 → 3.2). */
    static double arrondir(double distanceKm) {
        return Math.round(distanceKm * 10) / 10.0;
    }
}

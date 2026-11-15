package fr.miage.smartcarbure.service;

import fr.miage.smartcarbure.client.CarburantApiClient;
import fr.miage.smartcarbure.dto.CritereRechercheDTO;
import fr.miage.smartcarbure.dto.StationDTO;
import fr.miage.smartcarbure.enums.Carburant;
import fr.miage.smartcarbure.enums.TypeTri;
import fr.miage.smartcarbure.mapper.StationMapper;
import fr.miage.smartcarbure.model.Position;
import fr.miage.smartcarbure.model.PrixCarburant;
import fr.miage.smartcarbure.model.Station;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/** Filtre les stations par carburant et rayon, calcule leur distance et trie les résultats. */
@Service
public class RechercheService {

    private static final int RAYON_MIN_KM = 1;
    private static final int RAYON_MAX_KM = 50;
    private static final Pattern CODE_POSTAL_PATTERN = Pattern.compile("\\d{5}");

    private final CarburantApiClient carburantApiClient;
    private final CalculDistance calculDistance;
    private final StationMapper stationMapper;

    public RechercheService(CarburantApiClient carburantApiClient, CalculDistance calculDistance,
                            StationMapper stationMapper) {
        this.carburantApiClient = carburantApiClient;
        this.calculDistance = calculDistance;
        this.stationMapper = stationMapper;
    }

    public List<StationDTO> rechercher(CritereRechercheDTO critere) {
        Objects.requireNonNull(critere, "Les critères de recherche sont obligatoires.");
        Carburant carburant = critere.getCarburant();
        if (carburant == null) {
            throw new IllegalArgumentException("Le type de carburant est obligatoire.");
        }
        TypeTri tri = critere.getTri() == null ? TypeTri.PRIX : critere.getTri();

        boolean rechercheParVille = critere.getVille() != null && !critere.getVille().isBlank();
        Position origine = null;
        double rayonKm = 0;
        List<Station> stations;
        if (rechercheParVille) {
            String ville = critere.getVille().trim();
            // si c'est le code postal qui est fourni la recherche portera sur ce dernier
            stations = CODE_POSTAL_PATTERN.matcher(ville).matches()
                    ? carburantApiClient.rechercherParCodePostal(ville)
                    : carburantApiClient.rechercherParVille(ville);
        } else {
            origine = lireOrigine(critere);
            rayonKm = lireRayon(critere.getRayon());
            stations = carburantApiClient.rechercherAutour(
                    origine.getLatitude(), origine.getLongitude(), rayonKm);
        }

        Position origineFinale = origine;
        double rayonFinal = rayonKm;
        boolean calculerDistance = !rechercheParVille;

        return stations.stream()
                .filter(station -> prixPour(station, carburant) != null)
                .filter(station -> !calculerDistance
                        || distanceDansRayon(station, origineFinale, rayonFinal))
                .sorted(comparateur(tri, calculerDistance, origineFinale, carburant))
                .map(station -> {
                    StationDTO dto = stationMapper.toDTO(station);
                    if (calculerDistance) {
                        dto.setDistanceKm(calculerDistance(station, origineFinale));
                    }
                    return dto;
                })
                .toList();
    }

    private boolean distanceDansRayon(Station station, Position origine, double rayonKm) {
        Double distance = distanceKm(station, origine);
        return distance != null && distance <= rayonKm;
    }

    private Comparator<Station> comparateur(TypeTri tri, boolean calculerDistance,
                                           Position origine, Carburant carburant) {
        if (tri == TypeTri.DISTANCE && calculerDistance) {
            return Comparator.comparing(station -> distanceKm(station, origine),
                    Comparator.nullsLast(Double::compareTo));
        }
        return Comparator.comparing(station -> prixPour(station, carburant),
                Comparator.comparing(PrixCarburant::isDisponible).reversed()
                        .thenComparing(PrixCarburant::getPrix, Comparator.nullsLast(Double::compareTo)));
    }

    private Double calculerDistance(Station station, Position origine) {
        Double distance = distanceKm(station, origine);
        if (distance == null) {
            throw new IllegalStateException("Une station dans le rayon ne possède pas de coordonnées GPS.");
        }
        return distance;
    }

    private Double distanceKm(Station station, Position origine) {
        Position position = station.getPosition();
        if (!aDesCoordonnees(position) || (origine != null && !aDesCoordonnees(origine))) {
            return null;
        }
        return calculDistance.calculerDistance(origine, position);
    }

    private PrixCarburant prixPour(Station station, Carburant carburant) {
        return station.getCarburants().stream()
                .filter(prix -> prix.getCarburant() == carburant)
                .findFirst()
                .orElse(null);
    }

    private Position lireOrigine(CritereRechercheDTO critere) {
        if (critere.getLat() == null || critere.getLon() == null) {
            throw new IllegalArgumentException("La latitude et la longitude sont obligatoires hors recherche par ville.");
        }
        double latitude = critere.getLat();
        double longitude = critere.getLon();
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude invalide : " + latitude);
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude invalide : " + longitude);
        }
        return new Position(latitude, longitude, null);
    }

    private int lireRayon(Integer rayon) {
        if (rayon == null || rayon < RAYON_MIN_KM || rayon > RAYON_MAX_KM) {
            throw new IllegalArgumentException(
                    "Le rayon doit être compris entre " + RAYON_MIN_KM + " et " + RAYON_MAX_KM + " km.");
        }
        return rayon;
    }

    private boolean aDesCoordonnees(Position position) {
        return position != null && position.getLatitude() != null && position.getLongitude() != null;
    }
}

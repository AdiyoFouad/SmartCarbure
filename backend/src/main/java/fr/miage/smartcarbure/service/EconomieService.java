package fr.miage.smartcarbure.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import fr.miage.smartcarbure.client.CarburantApiClient;
import fr.miage.smartcarbure.dto.EconomieResponse;
import fr.miage.smartcarbure.enums.Carburant;
import fr.miage.smartcarbure.exception.StationIntrouvableException;
import fr.miage.smartcarbure.model.PrixCarburant;
import fr.miage.smartcarbure.model.Station;
import fr.miage.smartcarbure.model.Ville;

/**
 * Calcule l'économie réalisée sur un plein par rapport au prix moyen local (F15).
 *
 * <p>Règles du contrat d'API (docs/contrat-api.md, « Économie sur un plein ») :</p>
 * <ul>
 *   <li>le prix moyen local est la moyenne des prix des stations de la même ville qui vendent ce carburant,
 *       stations en rupture exclues ; la station choisie en fait partie ;</li>
 *   <li>montant = (prixMoyenLocal − prixStation) × litres, arrondi au centime :
 *       positif pour une économie, négatif pour un surcoût.</li>
 * </ul>
 *
 * <p>{@link #calculerPourStation} va chercher la station et les stations de sa ville avec CarburantApiClient,
 * puis délègue à {@link #calculer}, qui ne fait aucun appel réseau : on peut ainsi tester le calcul sans l'API.
 * Les calculs sont faits en BigDecimal pour éviter les erreurs d'arrondi des double.</p>
 *
 * <p>Erreurs : saisie invalide → IllegalArgumentException (400) ; station inconnue → StationIntrouvableException
 * (404) ; API en panne → ApiIndisponibleException levée par CarburantApiClient (503).</p>
 */
@Service
public class EconomieService {

    /** Les prix sont exprimés en €/L avec 3 décimales (1.749). */
    private static final int DECIMALES_PRIX = 3;
    /** Les montants sont arrondis au centime (1.10). */
    private static final int DECIMALES_MONTANT = 2;

    /** Client de l'API Prix des carburants, utilisé pour récupérer la station et les stations de sa ville. */
    private final CarburantApiClient carburantApiClient;

    /** Injection par constructeur : Spring fournit le CarburantApiClient. */
    public EconomieService(CarburantApiClient carburantApiClient) {
        this.carburantApiClient = carburantApiClient;
    }

    /**
     * Calcule l'économie d'un plein dans la station donnée par son identifiant (GET /api/economie).
     * La saisie est vérifiée avant tout appel à l'API.
     *
     * @param idStation identifiant de la station (par exemple « 35000001 »)
     * @param carburant carburant du plein
     * @param litres    volume du plein, strictement positif
     * @return le prix de la station, le prix moyen local, le volume et le montant économisé
     * @throws IllegalArgumentException    si un paramètre manque, si le volume n'est pas strictement positif,
     *                                     ou si la station ne vend pas ce carburant (ou l'a en rupture)
     * @throws StationIntrouvableException si aucune station n'a cet identifiant
     */
    public EconomieResponse calculerPourStation(String idStation, Carburant carburant, double litres) {
        // 1. On vérifie la saisie avant d'appeler l'API, pour ne pas faire d'appel réseau inutile
        if (idStation == null || idStation.isBlank()) {
            throw new IllegalArgumentException("L'identifiant de la station est obligatoire.");
        }
        verifierCarburantEtLitres(carburant, litres);

        // 2. On récupère la station choisie ; si elle n'existe pas, le handler renvoie un 404
        Station station = carburantApiClient.trouverParId(idStation)
                .orElseThrow(() -> new StationIntrouvableException(idStation));

        // 3. On la compare aux stations de sa ville
        return calculer(station, stationsDeLaVille(station), carburant, litres);
    }

    /**
     * Calcule l'économie (ou le surcoût) d'un plein dans une station.
     *
     * @param station           station choisie par l'utilisateur
     * @param stationsDeLaVille stations de la même ville (la station choisie peut y figurer ou non ;
     *                          elle n'est comptée qu'une fois) ; null est traité comme une liste vide
     * @param carburant         carburant du plein
     * @param litres            volume du plein, strictement positif
     * @return le prix de la station, le prix moyen local, le volume et le montant économisé
     * @throws IllegalArgumentException si un paramètre manque, si le volume n'est pas strictement positif,
     *                                  ou si la station ne vend pas ce carburant (ou l'a en rupture)
     */
    public EconomieResponse calculer(Station station, List<Station> stationsDeLaVille,
                                     Carburant carburant, double litres) {
        if (station == null) {
            throw new IllegalArgumentException("La station est obligatoire.");
        }
        verifierCarburantEtLitres(carburant, litres);

        // Prix dans la station choisie : refus (400) si elle ne vend pas ce carburant ou l'a en rupture
        BigDecimal prixStation = prixDisponible(station, carburant)
                .orElseThrow(() -> new IllegalArgumentException("La station " + station.getId()
                        + " ne vend pas le carburant " + carburant + " ou l'a en rupture."));

        BigDecimal prixMoyenLocal = prixMoyen(station, stationsDeLaVille, carburant);

        // montant = (moyenne − prix de la station) × litres, arrondi au centime
        BigDecimal montant = prixMoyenLocal.subtract(prixStation)
                .multiply(BigDecimal.valueOf(litres))
                .setScale(DECIMALES_MONTANT, RoundingMode.HALF_UP);

        return new EconomieResponse(prixStation, prixMoyenLocal, litres, montant);
    }

    /**
     * Stations de la même ville que la station choisie, lues dans l'API.
     * Si l'API ne donne pas de nom de ville, liste vide : la station est alors comparée à elle seule.
     */
    private List<Station> stationsDeLaVille(Station station) {
        Ville ville = station.getVille();
        if (ville == null || ville.getNom() == null || ville.getNom().isBlank()) {
            return List.of();
        }
        return carburantApiClient.rechercherParVille(ville.getNom());
    }

    /** Vérifications communes aux deux méthodes publiques. */
    private static void verifierCarburantEtLitres(Carburant carburant, double litres) {
        if (carburant == null) {
            throw new IllegalArgumentException("Le carburant est obligatoire.");
        }
        if (!Double.isFinite(litres) || litres <= 0) {
            throw new IllegalArgumentException("Le volume du plein doit être un nombre strictement positif : " + litres);
        }
    }

    /**
     * Moyenne des prix disponibles du carburant dans les stations de la ville, arrondie à 3 décimales.
     * Les stations sont dédoublonnées par identifiant et la station choisie est toujours comptée :
     * il y a donc au moins un prix (celui de la station, vérifié avant l'appel).
     */
    private BigDecimal prixMoyen(Station station, List<Station> stationsDeLaVille, Carburant carburant) {
        // La station choisie d'abord, puis les autres sans doublon (même identifiant = même station)
        Map<String, Station> stationsParId = new LinkedHashMap<>();
        stationsParId.put(station.getId(), station);
        if (stationsDeLaVille != null) {
            for (Station autre : stationsDeLaVille) {
                if (autre != null) {
                    stationsParId.putIfAbsent(autre.getId(), autre);
                }
            }
        }

        // On ne compte que les stations qui ont un prix disponible pour ce carburant
        BigDecimal somme = BigDecimal.ZERO;
        int nombrePrix = 0;
        for (Station s : stationsParId.values()) {
            Optional<BigDecimal> prix = prixDisponible(s, carburant);
            if (prix.isPresent()) {
                somme = somme.add(prix.get());
                nombrePrix++;
            }
        }
        return somme.divide(BigDecimal.valueOf(nombrePrix), DECIMALES_PRIX, RoundingMode.HALF_UP);
    }

    /**
     * Prix du carburant dans la station, s'il est vendu, disponible (pas en rupture) et strictement positif ;
     * Optional.empty() sinon.
     */
    private Optional<BigDecimal> prixDisponible(Station station, Carburant carburant) {
        return station.getCarburants().stream()
                .filter(prixCarburant -> prixCarburant.getCarburant() == carburant)
                .filter(PrixCarburant::isDisponible)
                .map(PrixCarburant::getPrix)
                .filter(prix -> prix != null && prix > 0)
                .findFirst()
                .map(prix -> BigDecimal.valueOf(prix).setScale(DECIMALES_PRIX, RoundingMode.HALF_UP));
    }
}

package fr.miage.smartcarbure.service;

import fr.miage.smartcarbure.dto.EconomieResponse;
import fr.miage.smartcarbure.model.PrixCarburant;
import fr.miage.smartcarbure.model.Station;
import fr.miage.smartcarbure.enums.Carburant;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

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
 * <p>Ce service ne fait aucun appel à l'API Prix des carburants : il reçoit la station et les stations
 * de sa ville, ce qui permet de le tester sans réseau. Les calculs sont faits en BigDecimal pour éviter
 * les erreurs d'arrondi des double.</p>
 *
 * <p>Une saisie invalide lève IllegalArgumentException (400 pour le frontend).</p>
 */
@Service
public class EconomieService {

    /** Les prix sont exprimés en €/L avec 3 décimales (1.749). */
    private static final int DECIMALES_PRIX = 3;
    /** Les montants sont arrondis au centime (1.10). */
    private static final int DECIMALES_MONTANT = 2;

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
        if (carburant == null) {
            throw new IllegalArgumentException("Le carburant est obligatoire.");
        }
        if (!Double.isFinite(litres) || litres <= 0) {
            throw new IllegalArgumentException("Le volume du plein doit être un nombre strictement positif : " + litres);
        }

        BigDecimal prixStation = prixDisponible(station, carburant)
                .orElseThrow(() -> new IllegalArgumentException("La station " + station.getId()
                        + " ne vend pas le carburant " + carburant + " ou l'a en rupture."));

        BigDecimal prixMoyenLocal = prixMoyen(station, stationsDeLaVille, carburant);

        BigDecimal montant = prixMoyenLocal.subtract(prixStation)
                .multiply(BigDecimal.valueOf(litres))
                .setScale(DECIMALES_MONTANT, RoundingMode.HALF_UP);

        return new EconomieResponse(prixStation, prixMoyenLocal, litres, montant);
    }

    /**
     * Moyenne des prix disponibles du carburant dans les stations de la ville, arrondie à 3 décimales.
     * Les stations sont dédoublonnées par identifiant et la station choisie est toujours comptée :
     * il y a donc au moins un prix (celui de la station, vérifié avant l'appel).
     */
    private BigDecimal prixMoyen(Station station, List<Station> stationsDeLaVille, Carburant carburant) {
        Map<String, Station> stationsParId = new LinkedHashMap<>();
        stationsParId.put(station.getId(), station);
        if (stationsDeLaVille != null) {
            for (Station autre : stationsDeLaVille) {
                if (autre != null) {
                    stationsParId.putIfAbsent(autre.getId(), autre);
                }
            }
        }

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

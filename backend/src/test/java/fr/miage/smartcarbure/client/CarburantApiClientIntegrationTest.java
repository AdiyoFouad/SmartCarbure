package fr.miage.smartcarbure.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.miage.smartcarbure.config.RestClientConfig;
import fr.miage.smartcarbure.model.Station;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Test d'intégration qui appelle la VRAIE API Prix des carburants (connexion Internet nécessaire).
 * Désactivé par défaut. Pour le lancer :
 * <pre>.\mvnw.cmd test "-Dtest=CarburantApiClientIntegrationTest" "-Dapi.reelle=true"</pre>
 */
@EnabledIfSystemProperty(named = "api.reelle", matches = "true")
class CarburantApiClientIntegrationTest {

    private static final String URL_API =
            "https://data.economie.gouv.fr/api/explore/v2.1/catalog/datasets/"
                    + "prix-des-carburants-en-france-flux-instantane-v2/records";

    private final CarburantApiClient client = new CarburantApiClient(
            new RestClientConfig().carburantRestClient(URL_API, Duration.ofSeconds(3), Duration.ofSeconds(10)),
            new StationJsonConverter(),
            500);

    @Test
    void rechercherRennesSurLaVraieApi() {
        List<Station> stations = client.rechercherParVille("rennes");

        assertFalse(stations.isEmpty(), "Aucune station trouvée pour Rennes");
        assertTrue(stations.stream().allMatch(s -> "Rennes".equalsIgnoreCase(s.getVille().getNom())));
    }

    @Test
    void rechercherAutourDeRennesSurLaVraieApi() {
        List<Station> stations = client.rechercherAutour(48.1173, -1.6778, 10);

        assertFalse(stations.isEmpty(), "Aucune station à moins de 10 km de Rennes");
    }
}

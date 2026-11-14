package fr.miage.smartcarbure.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import fr.miage.smartcarbure.exception.ApiIndisponibleException;
import fr.miage.smartcarbure.model.Position;
import fr.miage.smartcarbure.model.Trajet;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Tests d'ItineraireClient. OpenRouteService est simulé avec MockRestServiceServer :
 * aucun appel réseau n'est fait et aucune vraie clé n'est nécessaire.
 */
class ItineraireClientTest {

    private static final String URL = "https://ors.test" + ItineraireClient.CHEMIN_DIRECTIONS;

    /** Réponse GeoJSON simplifiée d'OpenRouteService pour Rennes → Nantes (points en [longitude, latitude]). */
    private static final String REPONSE_RENNES_NANTES = """
            {
              "type": "FeatureCollection",
              "features": [{
                "type": "Feature",
                "properties": { "summary": { "distance": 107412.3, "duration": 4380.5 } },
                "geometry": {
                  "type": "LineString",
                  "coordinates": [[-1.6778, 48.1173], [-1.6819, 47.8431], [-1.5536, 47.2184]]
                }
              }]
            }
            """;

    private final Position rennes = new Position(48.1173, -1.6778, null);
    private final Position nantes = new Position(47.2184, -1.5536, null);

    private MockRestServiceServer serveur;
    private ItineraireClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ors.test");
        serveur = MockRestServiceServer.bindTo(builder).build();
        client = new ItineraireClient(builder.build(), "cle-de-test");
    }

    @Test
    void envoieLesCoordonneesEnLongitudeLatitudeAvecLaCle() {
        serveur.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "cle-de-test"))
                .andExpect(content().string("{\"coordinates\":[[-1.6778,48.1173],[-1.5536,47.2184]]}"))
                .andRespond(withSuccess(REPONSE_RENNES_NANTES, MediaType.APPLICATION_JSON));

        client.calculerTrajet(rennes, nantes);

        serveur.verify();
    }

    @Test
    void convertitLaReponseEnTrajet() {
        serveur.expect(requestTo(URL)).andRespond(withSuccess(REPONSE_RENNES_NANTES, MediaType.APPLICATION_JSON));

        Trajet trajet = client.calculerTrajet(rennes, nantes);

        assertEquals(rennes, trajet.getDepart());
        assertEquals(nantes, trajet.getArrivee());
        assertEquals(3, trajet.getPoints().size());
        assertEquals(47.8431, trajet.getPoints().get(1).getLatitude());
        assertEquals(-1.6819, trajet.getPoints().get(1).getLongitude());
        assertEquals(107.4123, trajet.getDistanceKm(), 0.0001);
    }

    @Test
    void aucuneRoutePossibleEstUneSaisieInvalide() {
        serveur.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"code\":2010,\"message\":\"Could not find routable point within a radius of 350.0 meters\"}}"));

        assertThrows(IllegalArgumentException.class, () -> client.calculerTrajet(rennes, nantes));
    }

    @Test
    void uneCleRefuseeOuUnQuotaJournalierAtteintRendLeServiceIndisponible() {
        serveur.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThrows(ApiIndisponibleException.class, () -> client.calculerTrajet(rennes, nantes));
    }

    @Test
    void tropDeDemandesRendLeServiceIndisponible() {
        serveur.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThrows(ApiIndisponibleException.class, () -> client.calculerTrajet(rennes, nantes));
    }

    @Test
    void unePanneDuServiceLeRendIndisponible() {
        serveur.expect(requestTo(URL)).andRespond(withServerError());

        assertThrows(ApiIndisponibleException.class, () -> client.calculerTrajet(rennes, nantes));
    }

    @Test
    void unDelaiDepasseRendLeServiceIndisponible() {
        serveur.expect(requestTo(URL)).andRespond(requete -> {
            throw new SocketTimeoutException("délai dépassé");
        });

        assertThrows(ApiIndisponibleException.class, () -> client.calculerTrajet(rennes, nantes));
    }

    @Test
    void uneReponseInattendueRendLeServiceIndisponible() {
        serveur.expect(requestTo(URL)).andRespond(withSuccess("{\"features\":[]}", MediaType.APPLICATION_JSON));

        assertThrows(ApiIndisponibleException.class, () -> client.calculerTrajet(rennes, nantes));
    }

    @Test
    void sansCleAucunAppelNEstFait() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ors.test");
        MockRestServiceServer serveurSansAppel = MockRestServiceServer.bindTo(builder).build();
        ItineraireClient sansCle = new ItineraireClient(builder.build(), "  ");

        assertThrows(ApiIndisponibleException.class, () -> sansCle.calculerTrajet(rennes, nantes));
        serveurSansAppel.verify();
    }

    @Test
    void refuseUnePositionSansCoordonnees() {
        Position adresseSeule = new Position(null, null, "Rennes");

        assertThrows(IllegalArgumentException.class, () -> client.calculerTrajet(adresseSeule, nantes));
        assertThrows(NullPointerException.class, () -> client.calculerTrajet(null, nantes));
    }
}

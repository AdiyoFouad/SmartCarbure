package fr.miage.smartcarbure.client;

import fr.miage.smartcarbure.exception.ApiIndisponibleException;
import fr.miage.smartcarbure.model.Position;
import fr.miage.smartcarbure.model.Trajet;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Appelle le service d'itinéraire OpenRouteService (API Directions v2, profil voiture, format GeoJSON)
 * pour calculer la route entre deux positions.
 *
 * <p>La clé API est personnelle : elle est lue dans la variable d'environnement ITINERAIRE_API_KEY
 * (fichier .env, jamais sur GitHub). Sans clé, l'application démarre quand même, mais le calcul
 * d'un trajet lève ApiIndisponibleException.</p>
 *
 * <p>Erreurs :</p>
 * <ul>
 *   <li>aucune route praticable entre les deux points (point en mer, trop loin d'une route…) :
 *       IllegalArgumentException (saisie invalide, 400) ;</li>
 *   <li>clé absente ou refusée, quota dépassé, panne, délai dépassé, réponse illisible :
 *       ApiIndisponibleException (503), avec la cause journalisée au niveau WARN.</li>
 * </ul>
 */
@Component
public class ItineraireClient {

    private static final Logger LOG = LoggerFactory.getLogger(ItineraireClient.class);

    /**
     * Endpoint Directions v2 d'OpenRouteService, profil voiture, réponse GeoJSON, sur le serveur
     * api.heigit.org (l'ancienne adresse api.openrouteservice.org est fermée depuis novembre 2026).
     */
    static final String CHEMIN_DIRECTIONS = "/openrouteservice/v2/directions/driving-car/geojson";

    private final RestClient restClient;
    private final String cleApi;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public ItineraireClient(@Qualifier("itineraireRestClient") RestClient restClient,
                            @Value("${smartcarbure.itineraire.cle:}") String cleApi) {
        this.restClient = restClient;
        this.cleApi = cleApi == null ? "" : cleApi.trim();
    }

    /**
     * Calcule la route en voiture entre deux positions.
     *
     * @param depart  position de départ (avec latitude et longitude)
     * @param arrivee position d'arrivée (avec latitude et longitude)
     * @return le trajet : départ, arrivée, points de la route dans l'ordre et longueur en km
     * @throws NullPointerException     si une position est null
     * @throws IllegalArgumentException si une position n'a pas de coordonnées ou si aucune route n'existe
     * @throws ApiIndisponibleException si le service ne répond pas, refuse la clé ou dépasse son quota
     */
    public Trajet calculerTrajet(Position depart, Position arrivee) {
        Objects.requireNonNull(depart, "Le départ est obligatoire.");
        Objects.requireNonNull(arrivee, "L'arrivée est obligatoire.");
        verifierCoordonnees(depart, "Le départ");
        verifierCoordonnees(arrivee, "L'arrivée");
        if (cleApi.isEmpty()) {
            LOG.warn("Service d'itinéraire : aucune clé configurée (variable ITINERAIRE_API_KEY)");
            throw new ApiIndisponibleException("Le service d'itinéraire n'est pas configuré (clé absente).");
        }

        String corps;
        try {
            corps = restClient.post()
                    .uri(CHEMIN_DIRECTIONS)
                    .header("Authorization", cleApi)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpsRequete(depart, arrivee))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            throw traduireErreur(e);
        } catch (ResourceAccessException e) {
            LOG.warn("Service d'itinéraire : délai dépassé ou réseau indisponible : {}", e.getMessage());
            throw new ApiIndisponibleException("Le service d'itinéraire ne répond pas.", e);
        } catch (RestClientException e) {
            LOG.warn("Service d'itinéraire : échec de l'appel : {}", e.getMessage());
            throw new ApiIndisponibleException("Échec de l'appel au service d'itinéraire.", e);
        }
        return lireTrajet(corps, depart, arrivee);
    }

    /** Corps JSON de la requête. Attention : OpenRouteService attend [longitude, latitude]. */
    static String corpsRequete(Position depart, Position arrivee) {
        return "{\"coordinates\":[" + coordonnees(depart) + "," + coordonnees(arrivee) + "]}";
    }

    /**
     * Lit la réponse GeoJSON : les points sont dans features[0].geometry.coordinates
     * ([longitude, latitude]) et la longueur en mètres dans features[0].properties.summary.distance.
     */
    private Trajet lireTrajet(String corps, Position depart, Position arrivee) {
        try {
            JsonNode route = jsonMapper.readTree(corps).get("features").get(0);
            List<Position> points = new ArrayList<>();
            for (JsonNode point : route.get("geometry").get("coordinates")) {
                points.add(new Position(point.get(1).doubleValue(), point.get(0).doubleValue(), null));
            }
            double distanceMetres = route.get("properties").get("summary").get("distance").doubleValue();
            return new Trajet(depart, arrivee, points, distanceMetres / 1000);
        } catch (RuntimeException e) {
            // Réponse vide, champ absent, moins de deux points, coordonnées invalides…
            LOG.warn("Service d'itinéraire : réponse inattendue : {}", e.getMessage());
            throw new ApiIndisponibleException("Le service d'itinéraire a renvoyé une réponse inattendue.", e);
        }
    }

    /** 400 et 404 : pas de route possible (saisie) ; 401, 403, 429 et 5xx : service indisponible. */
    private RuntimeException traduireErreur(RestClientResponseException e) {
        int statut = e.getStatusCode().value();
        String message = messageErreur(e.getResponseBodyAsString());
        LOG.warn("Service d'itinéraire : erreur {} : {}", statut, message);
        if (statut == 400 || statut == 404) {
            return new IllegalArgumentException("Aucun itinéraire routier trouvé entre ces deux points.", e);
        }
        if (statut == 401 || statut == 403) {
            return new ApiIndisponibleException("Le service d'itinéraire refuse la clé ou son quota journalier est atteint.", e);
        }
        if (statut == 429) {
            return new ApiIndisponibleException("Trop de demandes au service d'itinéraire, réessayez dans une minute.", e);
        }
        return new ApiIndisponibleException("Le service d'itinéraire a répondu " + statut + ".", e);
    }

    /** Extrait error.message d'une réponse d'erreur d'OpenRouteService, ou renvoie le corps (tronqué). */
    private String messageErreur(String corps) {
        if (corps == null || corps.isBlank()) {
            return "(corps vide)";
        }
        try {
            JsonNode message = jsonMapper.readTree(corps).get("error").get("message");
            if (message != null && message.isString()) {
                return message.stringValue();
            }
        } catch (RuntimeException e) {
            // corps non JSON ou sans error.message
        }
        return corps.length() > 300 ? corps.substring(0, 300) + "…" : corps;
    }

    private static String coordonnees(Position position) {
        return "[" + nombre(position.getLongitude()) + "," + nombre(position.getLatitude()) + "]";
    }

    private static String nombre(double valeur) {
        return BigDecimal.valueOf(valeur).stripTrailingZeros().toPlainString();
    }

    private static void verifierCoordonnees(Position position, String libelle) {
        if (position.getLatitude() == null || position.getLongitude() == null) {
            throw new IllegalArgumentException(libelle + " doit avoir une latitude et une longitude.");
        }
    }
}

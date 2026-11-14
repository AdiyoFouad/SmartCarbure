package fr.miage.smartcarbure.client;

import fr.miage.smartcarbure.exception.ApiIndisponibleException;
import fr.miage.smartcarbure.model.Departement;
import fr.miage.smartcarbure.model.Position;
import fr.miage.smartcarbure.model.Station;
import fr.miage.smartcarbure.model.Ville;
import java.math.BigDecimal;
import java.net.URI;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Appelle l'API « Prix des carburants – flux instantané v2 » (Opendatasoft Explore v2.1, endpoint records)
 * et convertit sa réponse en objets du modèle grâce à StationJsonConverter.
 *
 * <p>Ce client ne fait aucune logique métier : pas de filtre par carburant, pas de tri par prix,
 * pas de calcul de distance (voir RechercheService). Il se contente de construire la requête,
 * de récupérer toutes les pages (dans la limite de max-resultats), de supprimer les doublons par id
 * et, pour les recherches par nom de ville ou de département, de ne garder que les noms exactement égaux.</p>
 *
 * <p>Toute panne de l'API (délai dépassé, réseau coupé, erreur 4xx ou 5xx, réponse illisible) lève
 * ApiIndisponibleException (503 pour le frontend) ; la cause est journalisée au niveau WARN.</p>
 */
@Component
public class CarburantApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(CarburantApiClient.class);

    /** Nombre maximal d'enregistrements par appel imposé par l'API. */
    static final int TAILLE_PAGE = 100;
    /** L'API refuse offset + limit au-delà de 10 000. */
    private static final int OFFSET_MAXIMUM = 10_000;
    /** Nombre maximal d'ids ou de points dans un même appel (longueur de l'URL). */
    static final int TAILLE_PAQUET = 100;

    private static final Pattern ID_STATION = Pattern.compile("\\d{1,12}");
    private static final Pattern CODE_DEPARTEMENT = Pattern.compile("\\d{2,3}|2[AB]", Pattern.CASE_INSENSITIVE);
    private static final Pattern CODE_POSTAL = Pattern.compile("\\d{5}");

    private final RestClient restClient;
    private final StationJsonConverter converter;
    private final int maxResultats;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public CarburantApiClient(@Qualifier("carburantRestClient") RestClient restClient,
                              StationJsonConverter converter,
                              @Value("${smartcarbure.api-carburants.max-resultats:500}") int maxResultats) {
        if (maxResultats < 1) {
            throw new IllegalArgumentException("max-resultats doit être au moins égal à 1.");
        }
        this.restClient = restClient;
        this.converter = converter;
        this.maxResultats = maxResultats;
    }

    // ------------------------------------------------------------------
    // Méthodes publiques
    // ------------------------------------------------------------------

    /**
     * Stations situées à moins de rayonKm du point donné, de la plus proche à la plus lointaine.
     * Au-delà de max-resultats, seules les plus proches sont renvoyées.
     */
    public List<Station> rechercherAutour(double latitude, double longitude, double rayonKm) {
        verifierCoordonnees(latitude, longitude);
        verifierRayon(rayonKm);
        String point = point(latitude, longitude);
        String where = "within_distance(geom, " + point + ", " + nombre(rayonKm) + "km)";
        String orderBy = "distance(geom, " + point + ")";
        return paginer(where, orderBy);
    }

    /**
     * Stations d'une ville. La casse, les accents, les tirets et les espaces sont ignorés :
     * « saint malo » trouve « Saint-Malo », mais « malo » ne le trouve pas.
     */
    public List<Station> rechercherParVille(String nom) {
        String nomVille = verifierTexte(nom, "Le nom de la ville");
        String attendu = normaliser(nomVille);
        List<Station> stations = paginer("ville like " + chaine(nomVille), "id");
        return stations.stream()
                .filter(station -> attendu.equals(normaliser(nomVille(station))))
                .toList();
    }

    /** Stations d'un code postal (par exemple 35000). */
    public List<Station> rechercherParCodePostal(String codePostal) {
        String cp = verifierTexte(codePostal, "Le code postal");
        if (!CODE_POSTAL.matcher(cp).matches()) {
            throw new IllegalArgumentException("Le code postal doit contenir 5 chiffres : " + cp);
        }
        return paginer("cp=" + chaine(cp), "id");
    }

    /**
     * Stations d'un département, désigné par son code (35, 2A, 974) ou par son nom
     * (« ille-et-vilaine » : casse, accents, tirets et espaces ignorés).
     */
    public List<Station> rechercherParDepartement(String codeOuNom) {
        String valeur = verifierTexte(codeOuNom, "Le code ou le nom du département");
        if (CODE_DEPARTEMENT.matcher(valeur).matches()) {
            return paginer("code_departement=" + chaine(valeur.toUpperCase(Locale.ROOT)), "id");
        }
        String attendu = normaliser(valeur);
        List<Station> stations = paginer("departement like " + chaine(valeur), "id");
        return stations.stream()
                .filter(station -> attendu.equals(normaliser(nomDepartement(station))))
                .toList();
    }

    /** Station dont l'identifiant est donné ; Optional.empty() si elle n'existe pas (ou si l'id n'est pas numérique). */
    public Optional<Station> trouverParId(String id) {
        if (id == null || !ID_STATION.matcher(id.trim()).matches()) {
            return Optional.empty();
        }
        JsonNode reponse = appeler("id=" + id.trim(), null, 1, 0);
        return converter.convertirListe(reponse).stream().findFirst();
    }

    /**
     * Stations dont les identifiants sont donnés (par exemple les favoris d'un utilisateur), en un appel
     * par paquet de 100 ids. Les ids inconnus ou non numériques sont ignorés ; liste vide si aucun id.
     */
    public List<Station> trouverParIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Set<String> idsValides = new LinkedHashSet<>();
        for (String id : ids) {
            if (id != null && ID_STATION.matcher(id.trim()).matches()) {
                idsValides.add(id.trim());
            }
        }
        return parPaquets(new ArrayList<>(idsValides),
                paquet -> "id in (" + String.join(", ", paquet) + ")");
    }

    /**
     * Stations situées à moins de rayonKm d'au moins un des points (par exemple les points d'un trajet),
     * en un appel par paquet de 100 points. Chaque station n'apparaît qu'une fois.
     * Tous les points doivent avoir une latitude et une longitude.
     */
    public List<Station> rechercherAutourDePoints(List<Position> points, double rayonKm) {
        verifierRayon(rayonKm);
        if (points == null || points.isEmpty()) {
            return List.of();
        }
        List<String> filtres = new ArrayList<>();
        for (Position position : points) {
            if (position == null || position.getLatitude() == null || position.getLongitude() == null) {
                throw new IllegalArgumentException("Chaque point doit avoir une latitude et une longitude.");
            }
            verifierCoordonnees(position.getLatitude(), position.getLongitude());
            filtres.add("within_distance(geom, " + point(position.getLatitude(), position.getLongitude())
                    + ", " + nombre(rayonKm) + "km)");
        }
        return parPaquets(filtres, paquet -> String.join(" OR ", paquet));
    }

    // ------------------------------------------------------------------
    // Pagination et appels HTTP
    // ------------------------------------------------------------------

    /** Découpe la liste en paquets, fait une recherche paginée par paquet et supprime les doublons par id. */
    private List<Station> parPaquets(List<String> elements, Function<List<String>, String> where) {
        Map<String, Station> stations = new LinkedHashMap<>();
        for (int debut = 0; debut < elements.size(); debut += TAILLE_PAQUET) {
            List<String> paquet = elements.subList(debut, Math.min(debut + TAILLE_PAQUET, elements.size()));
            for (Station station : paginer(where.apply(paquet), "id")) {
                stations.putIfAbsent(station.getId(), station);
            }
        }
        return new ArrayList<>(stations.values());
    }

    /**
     * Récupère toutes les pages d'une recherche (100 enregistrements par appel), sans dépasser max-resultats.
     * Les doublons par id sont supprimés : on garde la première station rencontrée.
     */
    private List<Station> paginer(String where, String orderBy) {
        Map<String, Station> stations = new LinkedHashMap<>();
        int offset = 0;
        boolean derniere = false;
        while (!derniere && offset < maxResultats && offset < OFFSET_MAXIMUM) {
            int limit = Math.min(TAILLE_PAGE, Math.min(maxResultats, OFFSET_MAXIMUM) - offset);
            JsonNode reponse = appeler(where, orderBy, limit, offset);
            int recus = reponse.get("results").size();
            for (Station station : converter.convertirListe(reponse)) {
                stations.putIfAbsent(station.getId(), station);
            }
            offset += recus;
            JsonNode total = reponse.get("total_count");
            derniere = recus < limit || (total != null && total.isNumber() && offset >= total.longValue());
        }
        if (!derniere) {
            LOG.warn("API Prix des carburants : recherche limitée à {} enregistrements (where={})", offset, where);
        }
        return new ArrayList<>(stations.values());
    }

    /**
     * Fait un appel à l'endpoint records et renvoie la réponse JSON, qui contient toujours un tableau "results".
     * Toute erreur lève ApiIndisponibleException après avoir été journalisée.
     */
    private JsonNode appeler(String where, String orderBy, int limit, int offset) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("where", where);
        variables.put("limit", limit);
        variables.put("offset", offset);
        if (orderBy != null) {
            variables.put("orderBy", orderBy);
        }
        URI[] uriAppelee = new URI[1];
        String corps;
        try {
            corps = restClient.get()
                    .uri(builder -> {
                        builder.queryParam("where", "{where}")
                                .queryParam("limit", "{limit}")
                                .queryParam("offset", "{offset}");
                        if (orderBy != null) {
                            builder.queryParam("order_by", "{orderBy}");
                        }
                        uriAppelee[0] = builder.build(variables);
                        return uriAppelee[0];
                    })
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            int statut = e.getStatusCode().value();
            String message = messageApi(e.getResponseBodyAsString());
            LOG.warn("API Prix des carburants : erreur {} pour {} : {}", statut, uriAppelee[0], message);
            throw new ApiIndisponibleException("L'API Prix des carburants a répondu " + statut + " : " + message, e);
        } catch (ResourceAccessException e) {
            LOG.warn("API Prix des carburants : délai dépassé ou réseau indisponible pour {} : {}",
                    uriAppelee[0], e.getMessage());
            throw new ApiIndisponibleException("L'API Prix des carburants ne répond pas.", e);
        } catch (RestClientException e) {
            LOG.warn("API Prix des carburants : échec de l'appel {} : {}", uriAppelee[0], e.getMessage());
            throw new ApiIndisponibleException("Échec de l'appel à l'API Prix des carburants.", e);
        }
        return lireReponse(corps, uriAppelee[0]);
    }

    /** Décode le corps de la réponse ; il doit contenir un tableau "results". */
    private JsonNode lireReponse(String corps, URI uri) {
        if (corps == null || corps.isBlank()) {
            LOG.warn("API Prix des carburants : réponse vide pour {}", uri);
            throw new ApiIndisponibleException("L'API Prix des carburants a renvoyé une réponse vide.");
        }
        JsonNode reponse;
        try {
            reponse = jsonMapper.readTree(corps);
        } catch (RuntimeException e) {
            LOG.warn("API Prix des carburants : réponse illisible pour {} : {}", uri, e.getMessage());
            throw new ApiIndisponibleException("L'API Prix des carburants a renvoyé une réponse illisible.", e);
        }
        if (reponse == null || !reponse.isObject() || reponse.get("results") == null || !reponse.get("results").isArray()) {
            LOG.warn("API Prix des carburants : réponse sans tableau \"results\" pour {}", uri);
            throw new ApiIndisponibleException("L'API Prix des carburants a renvoyé une réponse inattendue.");
        }
        return reponse;
    }

    /** Extrait le champ "message" d'une réponse d'erreur de l'API, ou renvoie le corps brut (tronqué). */
    private String messageApi(String corps) {
        if (corps == null || corps.isBlank()) {
            return "(corps vide)";
        }
        try {
            JsonNode erreur = jsonMapper.readTree(corps);
            JsonNode message = erreur == null ? null : erreur.get("message");
            if (message != null && message.isString()) {
                return message.stringValue();
            }
        } catch (RuntimeException e) {
            // corps non JSON : on renvoie le texte brut
        }
        return corps.length() > 300 ? corps.substring(0, 300) + "…" : corps;
    }

    // ------------------------------------------------------------------
    // Construction des filtres ODSQL
    // ------------------------------------------------------------------

    /** Point ODSQL : attention, l'ordre est longitude puis latitude. */
    private static String point(double latitude, double longitude) {
        return "geom'POINT(" + nombre(longitude) + " " + nombre(latitude) + ")'";
    }

    /** Nombre écrit avec un point décimal et sans notation scientifique. */
    private static String nombre(double valeur) {
        return BigDecimal.valueOf(valeur).stripTrailingZeros().toPlainString();
    }

    /** Chaîne ODSQL entre guillemets, avec \ et " échappés (le texte vient de l'utilisateur). */
    private static String chaine(String valeur) {
        return "\"" + valeur.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    /** Minuscules, sans accents, sans tirets ni espaces : « Saint-Malo » et « saint malo » donnent « saintmalo ». */
    static String normaliser(String texte) {
        if (texte == null) {
            return "";
        }
        String sansAccents = Normalizer.normalize(texte, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sansAccents.toLowerCase(Locale.ROOT).replaceAll("[\\s\\-]+", "");
    }

    private static String nomVille(Station station) {
        Ville ville = station.getVille();
        return ville == null ? null : ville.getNom();
    }

    private static String nomDepartement(Station station) {
        Departement departement = station.getDepartement();
        return departement == null ? null : departement.getNom();
    }

    // ------------------------------------------------------------------
    // Vérification des arguments
    // ------------------------------------------------------------------

    private static String verifierTexte(String valeur, String libelle) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException(libelle + " est obligatoire.");
        }
        return valeur.trim();
    }

    private static void verifierCoordonnees(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude invalide : " + latitude);
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude invalide : " + longitude);
        }
    }

    private static void verifierRayon(double rayonKm) {
        if (!Double.isFinite(rayonKm) || rayonKm <= 0) {
            throw new IllegalArgumentException("Le rayon doit être strictement positif : " + rayonKm);
        }
    }
}

package fr.miage.smartcarbure.client;

import fr.miage.smartcarbure.model.Carburant;
import fr.miage.smartcarbure.model.Departement;
import fr.miage.smartcarbure.model.Horaire;
import fr.miage.smartcarbure.model.Position;
import fr.miage.smartcarbure.model.PrixCarburant;
import fr.miage.smartcarbure.model.Region;
import fr.miage.smartcarbure.model.Service;
import fr.miage.smartcarbure.model.Station;
import fr.miage.smartcarbure.model.TypeEmplacement;
import fr.miage.smartcarbure.model.TypeRupture;
import fr.miage.smartcarbure.model.Ville;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Convertit le JSON de l'API « Prix des carburants – flux instantané v2 » en objets du modèle.
 *
 * <p>Chaque enregistrement de l'API contient les mêmes informations deux fois : sous forme de
 * chaînes JSON à décoder une seconde fois (prix, rupture, services, horaires), dont la forme
 * varie (objet ou tableau), et sous forme de champs « à plat » déjà décodés (gazole_prix,
 * sp95_rupture_type, services_service…). On lit les champs à plat, plus stables.
 * Seuls les horaires sont lus dans la chaîne JSON « horaires ».</p>
 *
 * <p>Aucune valeur absente, null ou mal formée ne fait planter la conversion :
 * elle donne null, false ou une liste vide.</p>
 *
 * <p>Cette classe ne fait aucun appel HTTP (voir CarburantApiClient).</p>
 */
@Component
public class StationJsonConverter {

    /** Les coordonnées latitude/longitude de l'API sont des degrés multipliés par 100 000. */
    private static final double FACTEUR_COORDONNEES = 100_000.0;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    /** Convertit la réponse complète de l'API : lit le tableau "results". */
    public List<Station> convertirListe(JsonNode reponse) {
        List<Station> stations = new ArrayList<>();
        if (reponse == null) {
            return stations;
        }
        JsonNode resultats = reponse.get("results");
        if (resultats == null || !resultats.isArray()) {
            return stations;
        }
        for (JsonNode enregistrement : resultats) {
            if (enregistrement != null && enregistrement.isObject()) {
                stations.add(convertir(enregistrement));
            }
        }
        return stations;
    }

    /** Convertit un enregistrement de l'API en station. Renvoie toujours une station, même pour {}. */
    public Station convertir(JsonNode enregistrement) {
        Station station = new Station();
        if (enregistrement == null || !enregistrement.isObject()) {
            return station;
        }
        station.setId(texte(enregistrement, "id"));
        // L'API ne fournit pas de nom de station : le nom reste null.
        station.setPosition(lirePosition(enregistrement));
        station.setVille(lireVille(enregistrement));
        station.setEmplacement(lireEmplacement(texte(enregistrement, "pop")));
        station.setAutomate24h("Oui".equalsIgnoreCase(texte(enregistrement, "horaires_automate_24_24")));
        station.setHoraires(lireHoraires(enregistrement));
        station.setServices(lireServices(enregistrement));
        station.setCarburants(lireCarburants(enregistrement));
        return station;
    }

    /**
     * Lit les coordonnées dans "geom" (degrés décimaux), sinon dans latitude/longitude divisées par 100 000,
     * ainsi que l'adresse. Renvoie une Position dès qu'il y a une adresse ou des coordonnées,
     * et null seulement s'il n'y a ni l'une ni les autres.
     */
    private Position lirePosition(JsonNode enregistrement) {
        Double latitude = null;
        Double longitude = null;
        JsonNode geom = enregistrement.get("geom");
        if (geom != null && geom.isObject()) {
            latitude = nombre(geom, "lat");
            longitude = nombre(geom, "lon");
        }
        if (latitude == null || longitude == null) {
            Double latitudeBrute = nombre(enregistrement, "latitude");
            Double longitudeBrute = nombre(enregistrement, "longitude");
            if (latitudeBrute != null && longitudeBrute != null) {
                latitude = latitudeBrute / FACTEUR_COORDONNEES;
                longitude = longitudeBrute / FACTEUR_COORDONNEES;
            }
        }
        if (latitude == null || longitude == null) {
            // Une coordonnée seule est inutilisable : on n'en garde aucune.
            latitude = null;
            longitude = null;
        }
        String adresse = texte(enregistrement, "adresse");
        if (latitude == null && adresse == null) {
            return null;
        }
        return new Position(latitude, longitude, adresse);
    }

    /** Construit la chaîne Ville → Departement → Region. */
    private Ville lireVille(JsonNode enregistrement) {
        Departement departement = lireDepartement(enregistrement);
        String nom = texte(enregistrement, "ville");
        String codePostal = texte(enregistrement, "cp");
        if (nom == null && codePostal == null && departement == null) {
            return null;
        }
        return new Ville(nom, codePostal, departement);
    }

    private Departement lireDepartement(JsonNode enregistrement) {
        Region region = lireRegion(enregistrement);
        String code = texte(enregistrement, "code_departement");
        String nom = texte(enregistrement, "departement");
        if (code == null && nom == null && region == null) {
            return null;
        }
        return new Departement(code, nom, region);
    }

    private Region lireRegion(JsonNode enregistrement) {
        String code = texte(enregistrement, "code_region");
        String nom = texte(enregistrement, "region");
        if (code == null && nom == null) {
            return null;
        }
        return new Region(code, nom);
    }

    /** "R" → ROUTE, "A" → AUTOROUTE, toute autre valeur → null. */
    private TypeEmplacement lireEmplacement(String pop) {
        if ("R".equalsIgnoreCase(pop)) {
            return TypeEmplacement.ROUTE;
        }
        if ("A".equalsIgnoreCase(pop)) {
            return TypeEmplacement.AUTOROUTE;
        }
        return null;
    }

    /** Lit le tableau à plat "services_service" (une chaîne seule est aussi acceptée). */
    private List<Service> lireServices(JsonNode enregistrement) {
        List<Service> services = new ArrayList<>();
        for (JsonNode element : enTableau(enregistrement.get("services_service"))) {
            String libelle = texte(element);
            if (libelle != null) {
                services.add(new Service(libelle));
            }
        }
        return services;
    }

    /**
     * Décode une seconde fois la chaîne JSON "horaires".
     * "jour" et "horaire" peuvent être un objet ou un tableau.
     * Un jour marqué fermé (@ferme = "1") donne ferme = true, ouverture et fermeture null.
     * Un jour sans plage horaire et non marqué fermé donne ferme = false, ouverture et fermeture null
     * (horaires inconnus). Avec plusieurs plages dans la journée, on garde la première ouverture
     * et la dernière fermeture.
     */
    private List<Horaire> lireHoraires(JsonNode enregistrement) {
        List<Horaire> horaires = new ArrayList<>();
        String brut = texte(enregistrement, "horaires");
        if (brut == null) {
            return horaires;
        }
        JsonNode racine;
        try {
            racine = jsonMapper.readTree(brut);
        } catch (RuntimeException e) {
            return horaires;
        }
        if (racine == null || !racine.isObject()) {
            return horaires;
        }
        for (JsonNode jourJson : enTableau(racine.get("jour"))) {
            DayOfWeek jour = lireJour(jourJson);
            if (jour == null) {
                continue;
            }
            boolean ferme = "1".equals(texte(jourJson, "@ferme"));
            LocalTime ouverture = null;
            LocalTime fermeture = null;
            if (!ferme) {
                List<JsonNode> plages = enTableau(jourJson.get("horaire"));
                if (!plages.isEmpty()) {
                    ouverture = heure(texte(plages.get(0), "@ouverture"));
                    fermeture = heure(texte(plages.get(plages.size() - 1), "@fermeture"));
                }
            }
            horaires.add(new Horaire(jour, ferme, ouverture, fermeture));
        }
        return horaires;
    }

    /** Jour à partir de "@id" (1 = lundi … 7 = dimanche), ou à défaut à partir de "@nom". */
    private DayOfWeek lireJour(JsonNode jourJson) {
        Double id = nombre(jourJson, "@id");
        if (id != null && id >= 1 && id <= 7 && id == Math.floor(id)) {
            return DayOfWeek.of(id.intValue());
        }
        String nom = texte(jourJson, "@nom");
        if (nom == null) {
            return null;
        }
        return switch (nom.trim().toLowerCase(Locale.FRENCH)) {
            case "lundi" -> DayOfWeek.MONDAY;
            case "mardi" -> DayOfWeek.TUESDAY;
            case "mercredi" -> DayOfWeek.WEDNESDAY;
            case "jeudi" -> DayOfWeek.THURSDAY;
            case "vendredi" -> DayOfWeek.FRIDAY;
            case "samedi" -> DayOfWeek.SATURDAY;
            case "dimanche" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }

    /** Un PrixCarburant par carburant qui a un prix ou une rupture ; les autres ne sont pas vendus. */
    private List<PrixCarburant> lireCarburants(JsonNode enregistrement) {
        List<PrixCarburant> carburants = new ArrayList<>();
        for (Carburant carburant : Carburant.values()) {
            PrixCarburant prixCarburant = lirePrixCarburant(enregistrement, carburant);
            if (prixCarburant != null) {
                carburants.add(prixCarburant);
            }
        }
        return carburants;
    }

    /** Lit les champs à plat <prefixe>_prix, _maj, _rupture_type et _rupture_debut (prefixe : gazole, sp95…, gplc). */
    private PrixCarburant lirePrixCarburant(JsonNode enregistrement, Carburant carburant) {
        String prefixe = carburant.name().toLowerCase(Locale.ROOT);
        Double prix = nombre(enregistrement, prefixe + "_prix");
        LocalDateTime dateMaj = date(texte(enregistrement, prefixe + "_maj"));
        String typeRuptureBrut = texte(enregistrement, prefixe + "_rupture_type");
        LocalDateTime debutRupture = date(texte(enregistrement, prefixe + "_rupture_debut"));

        boolean enRupture = typeRuptureBrut != null || debutRupture != null;
        if (prix == null && !enRupture) {
            return null;
        }
        return new PrixCarburant(carburant, prix, dateMaj, !enRupture, typeRupture(typeRuptureBrut), debutRupture);
    }

    // ------------------------------------------------------------------
    // Lecture sûre des valeurs : jamais d'exception, null en cas de problème
    // ------------------------------------------------------------------

    /** Valeur du champ sous forme de texte ; null si absent, null, vide ou non scalaire. */
    private String texte(JsonNode parent, String champ) {
        return parent == null ? null : texte(parent.get(champ));
    }

    private String texte(JsonNode noeud) {
        if (noeud == null || noeud.isNull() || !noeud.isValueNode()) {
            return null;
        }
        String valeur = noeud.asString().trim();
        return valeur.isEmpty() ? null : valeur;
    }

    /** Valeur numérique du champ (nombre JSON ou chaîne contenant un nombre) ; null sinon. */
    private Double nombre(JsonNode parent, String champ) {
        JsonNode noeud = parent == null ? null : parent.get(champ);
        if (noeud == null || noeud.isNull()) {
            return null;
        }
        if (noeud.isNumber()) {
            return noeud.doubleValue();
        }
        String valeur = texte(noeud);
        if (valeur == null) {
            return null;
        }
        try {
            double resultat = Double.parseDouble(valeur.replace(',', '.'));
            return Double.isFinite(resultat) ? resultat : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Date de l'API en LocalDateTime à l'heure de Paris.
     *
     * <p>Les champs à plat sont écrits "2026-11-04T08:56:01+00:00", mais le décalage +00:00 est faux :
     * l'heure est celle de Paris. On le voit en comparant avec la chaîne JSON "prix", qui donne
     * "@maj": "2026-11-04 08:56:01" (heure locale) pour la même mise à jour. On ignore donc le décalage
     * et on garde l'heure telle quelle. La conversion en UTC demandée par le contrat d'API
     * se fera lors du passage en DTO.</p>
     *
     * <p>Le format "2026-11-04 08:56:01" (chaînes JSON imbriquées) est aussi accepté.</p>
     */
    private LocalDateTime date(String valeur) {
        if (valeur == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(valeur).toLocalDateTime();
        } catch (RuntimeException e) {
            // Pas de décalage horaire : on essaie le format local.
        }
        try {
            return LocalDateTime.parse(valeur.replace(' ', 'T'));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Heure de l'API au format "07.00" (ou "07:00") ; null si mal formée. */
    private LocalTime heure(String valeur) {
        if (valeur == null) {
            return null;
        }
        try {
            return LocalTime.parse(valeur.replace('.', ':'));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** "temporaire" → TEMPORAIRE, "definitive" (ou "définitive") → DEFINITIVE, sinon null. */
    private TypeRupture typeRupture(String valeur) {
        if (valeur == null) {
            return null;
        }
        return switch (valeur.trim().toLowerCase(Locale.FRENCH)) {
            case "temporaire" -> TypeRupture.TEMPORAIRE;
            case "definitive", "définitive" -> TypeRupture.DEFINITIVE;
            default -> null;
        };
    }

    /** Un tableau donne ses éléments, un objet ou une valeur seule donne une liste d'un élément, null donne une liste vide. */
    private List<JsonNode> enTableau(JsonNode noeud) {
        List<JsonNode> elements = new ArrayList<>();
        if (noeud == null || noeud.isNull() || noeud.isMissingNode()) {
            return elements;
        }
        if (noeud.isArray()) {
            for (JsonNode element : noeud) {
                elements.add(element);
            }
        } else {
            elements.add(noeud);
        }
        return elements;
    }
}

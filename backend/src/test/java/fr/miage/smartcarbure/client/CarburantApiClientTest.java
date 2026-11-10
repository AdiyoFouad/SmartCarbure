package fr.miage.smartcarbure.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import fr.miage.smartcarbure.exception.ApiIndisponibleException;
import fr.miage.smartcarbure.model.Position;
import fr.miage.smartcarbure.model.Station;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/** Tests de CarburantApiClient sans appel réseau : l'API est simulée avec MockRestServiceServer. */
class CarburantApiClientTest {

    private static final String URL_API = "https://api.test/records";

    private MockRestServiceServer serveur;
    private CarburantApiClient client;

    @BeforeEach
    void creerClient() {
        client = creerClient(500);
    }

    private CarburantApiClient creerClient(int maxResultats) {
        RestClient.Builder builder = RestClient.builder().baseUrl(URL_API);
        serveur = MockRestServiceServer.bindTo(builder).build();
        return new CarburantApiClient(builder.build(), new StationJsonConverter(), maxResultats);
    }

    // ------------------------------------------------------------------
    // Outils
    // ------------------------------------------------------------------

    private static String fichier(String chemin) throws IOException {
        try (InputStream flux = CarburantApiClientTest.class.getResourceAsStream(chemin)) {
            assertNotNull(flux, "Fichier introuvable : " + chemin);
            return new String(flux.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Réponse de l'API avec le total annoncé et les enregistrements donnés. */
    private static String reponse(long total, List<String> enregistrements) {
        return "{\"total_count\": " + total + ", \"results\": [" + String.join(", ", enregistrements) + "]}";
    }

    private static String reponse(long total, String... enregistrements) {
        return reponse(total, List.of(enregistrements));
    }

    private static String station(long id, String ville) {
        return "{\"id\": " + id + ", \"ville\": \"" + ville + "\"}";
    }

    /** Enregistrements d'ids consécutifs (de premier à dernier inclus). */
    private static List<String> stations(long premier, long dernier) {
        return IntStream.rangeClosed((int) premier, (int) dernier)
                .mapToObj(id -> station(id, "Ville"))
                .toList();
    }

    private static List<String> ids(List<Station> stations) {
        return stations.stream().map(Station::getId).toList();
    }

    /** Vérifie la valeur (décodée) d'un paramètre de la requête. */
    private static RequestMatcher parametre(String nom, String attendu) {
        return requete -> {
            MultiValueMap<String, String> parametres =
                    UriComponentsBuilder.fromUri(requete.getURI()).build().getQueryParams();
            String brut = parametres.getFirst(nom);
            assertNotNull(brut, "Paramètre absent : " + nom + " dans " + requete.getURI());
            assertEquals(attendu, URLDecoder.decode(brut, StandardCharsets.UTF_8), "Paramètre " + nom);
        };
    }

    private static RequestMatcher sansParametre(String nom) {
        return requete -> assertFalse(
                UriComponentsBuilder.fromUri(requete.getURI()).build().getQueryParams().containsKey(nom),
                "Paramètre inattendu : " + nom);
    }

    // ------------------------------------------------------------------
    // Recherche autour d'une position
    // ------------------------------------------------------------------

    @Test
    void rechercherAutourEnvoieLeFiltreGeographiqueEtTrieParDistance() throws IOException {
        serveur.expect(once(), method(HttpMethod.GET))
                .andExpect(parametre("where", "within_distance(geom, geom'POINT(-1.6778 48.1173)', 10km)"))
                .andExpect(parametre("order_by", "distance(geom, geom'POINT(-1.6778 48.1173)')"))
                .andExpect(parametre("limit", "100"))
                .andExpect(parametre("offset", "0"))
                .andRespond(withSuccess(fichier("/api-carburants-exemple.json"), MediaType.APPLICATION_JSON));

        List<Station> stations = client.rechercherAutour(48.1173, -1.6778, 10);

        serveur.verify();
        // total_count annonce 15 mais le fichier ne contient que 5 enregistrements : page incomplète → arrêt
        assertEquals(5, stations.size());
        assertEquals("35200008", stations.get(0).getId());
    }

    @Test
    void rayonDecimalEcritAvecUnPoint() {
        serveur.expect(once(), parametre("where", "within_distance(geom, geom'POINT(-1.5 48.25)', 2.5km)"))
                .andRespond(withSuccess(reponse(0), MediaType.APPLICATION_JSON));

        assertTrue(client.rechercherAutour(48.25, -1.5, 2.5).isEmpty());
        serveur.verify();
    }

    // ------------------------------------------------------------------
    // Pagination, plafond et doublons
    // ------------------------------------------------------------------

    @Test
    void paginationRecupereToutesLesPages() {
        serveur.expect(once(), parametre("offset", "0"))
                .andExpect(parametre("limit", "100"))
                .andRespond(withSuccess(reponse(150, stations(1, 100)), MediaType.APPLICATION_JSON));
        serveur.expect(once(), parametre("offset", "100"))
                .andExpect(parametre("limit", "100"))
                .andRespond(withSuccess(reponse(150, stations(101, 150)), MediaType.APPLICATION_JSON));

        List<Station> stations = client.rechercherParCodePostal("35000");

        serveur.verify();
        assertEquals(150, stations.size());
        assertEquals("1", stations.get(0).getId());
        assertEquals("150", stations.get(149).getId());
    }

    @Test
    void plafondLimiteLeNombreDAppels() {
        client = creerClient(150);
        serveur.expect(once(), parametre("offset", "0"))
                .andExpect(parametre("limit", "100"))
                .andRespond(withSuccess(reponse(2000, stations(1, 100)), MediaType.APPLICATION_JSON));
        serveur.expect(once(), parametre("offset", "100"))
                .andExpect(parametre("limit", "50"))
                .andRespond(withSuccess(reponse(2000, stations(101, 150)), MediaType.APPLICATION_JSON));

        List<Station> stations = client.rechercherParDepartement("35");

        serveur.verify(); // exactement 2 appels, pas un de plus
        assertEquals(150, stations.size());
    }

    @Test
    void pageVideArreteLaPagination() {
        serveur.expect(once(), parametre("offset", "0"))
                .andRespond(withSuccess(reponse(500), MediaType.APPLICATION_JSON));

        assertTrue(client.rechercherParCodePostal("35000").isEmpty());
        serveur.verify();
    }

    @Test
    void doublonsSupprimesEnGardantLePremier() {
        // Page 1 : ids 1 à 100 ; page 2 : ids 100 à 150 (l'id 100 apparaît deux fois, avec une autre ville)
        List<String> page2 = new ArrayList<>();
        page2.add(station(100, "Doublon"));
        page2.addAll(stations(101, 150));
        serveur.expect(once(), parametre("offset", "0"))
                .andRespond(withSuccess(reponse(151, stations(1, 100)), MediaType.APPLICATION_JSON));
        serveur.expect(once(), parametre("offset", "100"))
                .andRespond(withSuccess(reponse(151, page2), MediaType.APPLICATION_JSON));

        List<Station> stations = client.rechercherAutour(48.1, -1.6, 50);

        serveur.verify();
        assertEquals(150, stations.size());
        Station station100 = stations.stream().filter(s -> "100".equals(s.getId())).findFirst().orElseThrow();
        assertEquals("Ville", station100.getVille().getNom());
    }

    @Test
    void doublonsDansUneMemePage() {
        serveur.expect(once(), method(HttpMethod.GET))
                .andRespond(withSuccess(reponse(3, station(1, "Rennes"), station(1, "Autre"), station(2, "Rennes")),
                        MediaType.APPLICATION_JSON));

        List<Station> stations = client.rechercherParVille("Rennes");

        assertEquals(List.of("1", "2"), ids(stations));
    }

    // ------------------------------------------------------------------
    // Ville, code postal, département
    // ------------------------------------------------------------------

    @Test
    void rechercherParVilleGardeSeulementLesVillesEgales() {
        serveur.expect(once(), parametre("where", "ville like \"rennes\""))
                .andExpect(parametre("order_by", "id"))
                .andRespond(withSuccess(reponse(3,
                        station(1, "Rennes"), station(2, "Rennes-le-Château"), station(3, "RENNES")),
                        MediaType.APPLICATION_JSON));

        List<Station> stations = client.rechercherParVille("rennes");

        serveur.verify();
        assertEquals(List.of("1", "3"), ids(stations));
    }

    @Test
    void rechercherParVilleIgnoreAccentsTiretsEtEspaces() {
        serveur.expect(once(), parametre("where", "ville like \"evry courcouronnes\""))
                .andRespond(withSuccess(reponse(2,
                        station(1, "\\u00c9vry-Courcouronnes"), station(2, "\\u00c9vry-Gr\\u00e9gy-sur-Yerre")),
                        MediaType.APPLICATION_JSON));

        List<Station> stations = client.rechercherParVille("  evry courcouronnes ");

        serveur.verify();
        assertEquals(List.of("1"), ids(stations));
    }

    @Test
    void villeAvecGuillemetEstEchappee() {
        serveur.expect(once(), parametre("where", "ville like \"a\\\"b\\\\c\""))
                .andRespond(withSuccess(reponse(0), MediaType.APPLICATION_JSON));

        assertTrue(client.rechercherParVille("a\"b\\c").isEmpty());
        serveur.verify();
    }

    @Test
    void caracteresSpeciauxEncodesDansLUrl() {
        // « & », « = » et « + » ne doivent pas casser la requête
        serveur.expect(once(), parametre("where", "ville like \"a&b=c+d\""))
                .andRespond(withSuccess(reponse(0), MediaType.APPLICATION_JSON));

        client.rechercherParVille("a&b=c+d");
        serveur.verify();
    }

    @Test
    void rechercherParCodePostal() {
        serveur.expect(once(), parametre("where", "cp=\"35000\""))
                .andExpect(parametre("order_by", "id"))
                .andRespond(withSuccess(reponse(1, station(35000015, "Rennes")), MediaType.APPLICATION_JSON));

        assertEquals(List.of("35000015"), ids(client.rechercherParCodePostal("35000")));
        serveur.verify();
    }

    @Test
    void rechercherParDepartementAvecUnCode() {
        serveur.expect(once(), parametre("where", "code_departement=\"2A\""))
                .andRespond(withSuccess(reponse(0), MediaType.APPLICATION_JSON));
        client.rechercherParDepartement("2a");
        serveur.verify();

        client = creerClient(500);
        serveur.expect(once(), parametre("where", "code_departement=\"974\""))
                .andRespond(withSuccess(reponse(0), MediaType.APPLICATION_JSON));
        client.rechercherParDepartement("974");
        serveur.verify();
    }

    @Test
    void rechercherParDepartementAvecUnNom() {
        String stationIlleEtVilaine = "{\"id\": 1, \"ville\": \"Rennes\", \"code_departement\": \"35\", "
                + "\"departement\": \"Ille-et-Vilaine\"}";
        String autreDepartement = "{\"id\": 2, \"ville\": \"Vitré\", \"code_departement\": \"99\", "
                + "\"departement\": \"Ille\"}";
        serveur.expect(once(), parametre("where", "departement like \"ille et vilaine\""))
                .andRespond(withSuccess(reponse(2, stationIlleEtVilaine, autreDepartement), MediaType.APPLICATION_JSON));

        List<Station> stations = client.rechercherParDepartement("ille et vilaine");

        serveur.verify();
        assertEquals(List.of("1"), ids(stations));
    }

    // ------------------------------------------------------------------
    // Recherche par id
    // ------------------------------------------------------------------

    @Test
    void trouverParIdExistant() throws IOException {
        serveur.expect(once(), parametre("where", "id=35200008"))
                .andExpect(parametre("limit", "1"))
                .andExpect(sansParametre("order_by"))
                .andRespond(withSuccess(fichier("/api-carburants-exemple.json"), MediaType.APPLICATION_JSON));

        Optional<Station> station = client.trouverParId("35200008");

        serveur.verify();
        assertTrue(station.isPresent());
        assertEquals("35200008", station.get().getId());
    }

    @Test
    void trouverParIdInexistant() {
        serveur.expect(once(), parametre("where", "id=99999999"))
                .andRespond(withSuccess(reponse(0), MediaType.APPLICATION_JSON));

        assertTrue(client.trouverParId("99999999").isEmpty());
        serveur.verify();
    }

    @Test
    void trouverParIdNonNumeriqueSansAppel() {
        assertTrue(client.trouverParId("abc").isEmpty());
        assertTrue(client.trouverParId("1 OR 1=1").isEmpty());
        assertTrue(client.trouverParId(null).isEmpty());
        serveur.verify(); // aucun appel attendu
    }

    @Test
    void trouverParIds() {
        serveur.expect(once(), parametre("where", "id in (35200008, 35000013)"))
                .andRespond(withSuccess(reponse(2, station(35200008, "Rennes"), station(35000013, "Rennes")),
                        MediaType.APPLICATION_JSON));

        List<Station> stations = client.trouverParIds(List.of("35200008", "abc", "35000013", "35200008"));

        serveur.verify();
        assertEquals(List.of("35200008", "35000013"), ids(stations));
    }

    @Test
    void trouverParIdsVideSansAppel() {
        assertTrue(client.trouverParIds(List.of()).isEmpty());
        assertTrue(client.trouverParIds(null).isEmpty());
        assertTrue(client.trouverParIds(List.of("abc")).isEmpty());
        serveur.verify();
    }

    // ------------------------------------------------------------------
    // Points d'un trajet
    // ------------------------------------------------------------------

    @Test
    void rechercherAutourDePointsUnSeulAppel() {
        List<Position> points = List.of(new Position(48.1173, -1.6778, null), new Position(47.2184, -1.5536, null));
        serveur.expect(once(), parametre("where",
                        "within_distance(geom, geom'POINT(-1.6778 48.1173)', 5km)"
                                + " OR within_distance(geom, geom'POINT(-1.5536 47.2184)', 5km)"))
                .andRespond(withSuccess(reponse(2, station(1, "Rennes"), station(2, "Nantes")),
                        MediaType.APPLICATION_JSON));

        assertEquals(List.of("1", "2"), ids(client.rechercherAutourDePoints(points, 5)));
        serveur.verify();
    }

    @Test
    void rechercherAutourDePointsParPaquetsSansDoublon() {
        List<Position> points = IntStream.range(0, 150)
                .mapToObj(i -> new Position(47.0 + i * 0.01, -1.6, null))
                .toList();
        String premierPaquet = IntStream.range(0, 100)
                .mapToObj(i -> "within_distance(geom, geom'POINT(-1.6 " + coordonnee(47.0 + i * 0.01) + ")', 5km)")
                .collect(Collectors.joining(" OR "));
        serveur.expect(once(), parametre("where", premierPaquet))
                .andRespond(withSuccess(reponse(2, station(1, "A"), station(2, "B")), MediaType.APPLICATION_JSON));
        // Deuxième paquet (50 points) : la station 2 revient, elle ne doit pas être dupliquée
        serveur.expect(once(), method(HttpMethod.GET))
                .andRespond(withSuccess(reponse(2, station(2, "B"), station(3, "C")), MediaType.APPLICATION_JSON));

        List<Station> stations = client.rechercherAutourDePoints(points, 5);

        serveur.verify();
        assertEquals(List.of("1", "2", "3"), ids(stations));
    }

    private static String coordonnee(double valeur) {
        return java.math.BigDecimal.valueOf(valeur).stripTrailingZeros().toPlainString();
    }

    @Test
    void rechercherAutourDePointsSansCoordonneesRefuse() {
        List<Position> points = List.of(new Position(null, null, "1 rue X"));
        assertThrows(IllegalArgumentException.class, () -> client.rechercherAutourDePoints(points, 5));
        assertTrue(client.rechercherAutourDePoints(List.of(), 5).isEmpty());
        serveur.verify();
    }

    // ------------------------------------------------------------------
    // Erreurs de l'API
    // ------------------------------------------------------------------

    @Test
    void erreur503LeveApiIndisponible() {
        serveur.expect(once(), method(HttpMethod.GET)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertThrows(ApiIndisponibleException.class, () -> client.rechercherParCodePostal("35000"));
    }

    @Test
    void erreur500LeveApiIndisponible() {
        serveur.expect(once(), method(HttpMethod.GET)).andRespond(withServerError());
        assertThrows(ApiIndisponibleException.class, () -> client.trouverParId("35200008"));
    }

    @Test
    void erreur400LeveApiIndisponibleAvecLeMessageDeLApi() {
        serveur.expect(once(), method(HttpMethod.GET)).andRespond(withBadRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error_code\": \"ODSQLError\", \"message\": \"ODSQL query is malformed\"}"));

        ApiIndisponibleException e = assertThrows(ApiIndisponibleException.class,
                () -> client.rechercherParCodePostal("35000"));
        assertTrue(e.getMessage().contains("400"));
        assertTrue(e.getMessage().contains("ODSQL query is malformed"));
    }

    @Test
    void delaiDepasseLeveApiIndisponible() {
        serveur.expect(once(), method(HttpMethod.GET))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));
        assertThrows(ApiIndisponibleException.class, () -> client.rechercherAutour(48.1, -1.6, 10));
    }

    @Test
    void reponseNonJsonLeveApiIndisponible() {
        serveur.expect(once(), method(HttpMethod.GET))
                .andRespond(withSuccess("<html>Maintenance</html>", MediaType.TEXT_HTML));
        assertThrows(ApiIndisponibleException.class, () -> client.rechercherParCodePostal("35000"));
    }

    @Test
    void reponseSansResultsLeveApiIndisponible() {
        serveur.expect(once(), method(HttpMethod.GET))
                .andRespond(withSuccess("{\"total_count\": 3}", MediaType.APPLICATION_JSON));
        assertThrows(ApiIndisponibleException.class, () -> client.rechercherParCodePostal("35000"));
    }

    @Test
    void reponseVideLeveApiIndisponible() {
        serveur.expect(once(), method(HttpMethod.GET)).andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        assertThrows(ApiIndisponibleException.class, () -> client.rechercherParCodePostal("35000"));
    }

    @Test
    void erreurSurLaDeuxiemePageLeveApiIndisponible() {
        serveur.expect(once(), parametre("offset", "0"))
                .andRespond(withSuccess(reponse(150, stations(1, 100)), MediaType.APPLICATION_JSON));
        serveur.expect(once(), parametre("offset", "100")).andRespond(withServerError());

        assertThrows(ApiIndisponibleException.class, () -> client.rechercherParCodePostal("35000"));
    }

    @Test
    void aucunResultatDonneListeVide() {
        serveur.expect(once(), method(HttpMethod.GET)).andRespond(withSuccess(reponse(0), MediaType.APPLICATION_JSON));
        assertTrue(client.rechercherParVille("Inconnue").isEmpty());
    }

    // ------------------------------------------------------------------
    // Arguments invalides : aucune requête envoyée
    // ------------------------------------------------------------------

    @Test
    void argumentsInvalidesSansAppel() {
        assertThrows(IllegalArgumentException.class, () -> client.rechercherParVille(null));
        assertThrows(IllegalArgumentException.class, () -> client.rechercherParVille("  "));
        assertThrows(IllegalArgumentException.class, () -> client.rechercherParCodePostal("3500"));
        assertThrows(IllegalArgumentException.class, () -> client.rechercherParDepartement(""));
        assertThrows(IllegalArgumentException.class, () -> client.rechercherAutour(48.1, -1.6, 0));
        assertThrows(IllegalArgumentException.class, () -> client.rechercherAutour(91, -1.6, 10));
        assertThrows(IllegalArgumentException.class, () -> client.rechercherAutour(48.1, Double.NaN, 10));
        assertThrows(IllegalArgumentException.class, () -> client.rechercherAutourDePoints(List.of(), -1));
        serveur.verify();
    }

    @Test
    void normalisation() {
        assertEquals("saintmalo", CarburantApiClient.normaliser("Saint-Malo"));
        assertEquals("saintmalo", CarburantApiClient.normaliser(" saint  malo "));
        assertEquals("evrycourcouronnes", CarburantApiClient.normaliser("Évry-Courcouronnes"));
        assertEquals("", CarburantApiClient.normaliser(null));
    }
}

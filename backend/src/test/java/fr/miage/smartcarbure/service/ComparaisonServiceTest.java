package fr.miage.smartcarbure.service;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fr.miage.smartcarbure.client.CarburantApiClient;
import fr.miage.smartcarbure.dto.ComparaisonResponse;
import fr.miage.smartcarbure.enums.Carburant;
import fr.miage.smartcarbure.model.Departement;
import fr.miage.smartcarbure.model.PrixCarburant;
import fr.miage.smartcarbure.model.Station;
import fr.miage.smartcarbure.model.Ville;

class ComparaisonServiceTest {

    private final CarburantApiClient client = mock(CarburantApiClient.class);
    private final ComparaisonService service = new ComparaisonService(client);

    @Test
    void compareLesPrixDisponiblesDeDeuxVillesEtCalculeLEcart() {
        when(client.rechercherParVille("Rennes")).thenReturn(List.of(
                station("1", "Rennes", "35", "Ille-et-Vilaine", 1.742),
                station("2", "Rennes", "35", "Ille-et-Vilaine", 1.743),
                station("3", "Rennes", "35", "Ille-et-Vilaine", 1.800, false)));
        when(client.rechercherParVille("Nantes")).thenReturn(List.of(
                station("4", "Nantes", "44", "Loire-Atlantique", 1.746)));

        ComparaisonResponse reponse = service.comparer("ville", " Rennes ", "Nantes", Carburant.GAZOLE);

        assertEquals("VILLE", reponse.type());
        assertEquals("Rennes", reponse.zone1().nom());
        assertEquals(2, reponse.zone1().nombreStations());
        assertEquals(new BigDecimal("1.743"), reponse.zone1().prixMoyen());
        assertEquals("Nantes", reponse.zone2().nom());
        assertEquals(new BigDecimal("1.746"), reponse.zone2().prixMoyen());
        assertEquals(new BigDecimal("-0.003"), reponse.ecart());
    }

    @Test
    void utiliseLeCodePostalEtFormateLeNomDuDepartement() {
        when(client.rechercherParCodePostal("35000")).thenReturn(List.of(
                station("1", "Rennes", "35", "Ille-et-Vilaine", 1.733)));
        when(client.rechercherParDepartement("35")).thenReturn(List.of(
                station("1", "Rennes", "35", "Ille-et-Vilaine", 1.733)));
        when(client.rechercherParDepartement("44")).thenReturn(List.of(
                station("2", "Nantes", "44", "Loire-Atlantique", 1.755)));

        ComparaisonResponse ville = service.comparer("VILLE", "35000", "35000", Carburant.GAZOLE);
        ComparaisonResponse departement = service.comparer("DEPARTEMENT", "35", "44", Carburant.GAZOLE);

        verify(client, times(2)).rechercherParCodePostal("35000");
        assertEquals("Ille-et-Vilaine (35)", departement.zone1().nom());
        assertEquals("Loire-Atlantique (44)", departement.zone2().nom());
        assertEquals(new BigDecimal("-0.022"), departement.ecart());
        assertEquals(new BigDecimal("0.000"), ville.ecart());
    }

    @Test
    void zoneSansPrixRenvoieDesValeursNullesSansEcart() {
        when(client.rechercherParVille("Ville vide")).thenReturn(List.of());
        when(client.rechercherParVille("Rennes")).thenReturn(List.of(
                station("1", "Rennes", "35", "Ille-et-Vilaine", 1.742)));

        ComparaisonResponse reponse = service.comparer("VILLE", "Ville vide", "Rennes", Carburant.GAZOLE);

        assertEquals("Ville vide", reponse.zone1().nom());
        assertNull(reponse.zone1().prixMoyen());
        assertEquals(0, reponse.zone1().nombreStations());
        assertNull(reponse.ecart());
    }

    @Test
    void typeInconnuOuZoneVideEstRefuseAvantToutAppel() {
        assertEquals("Le type doit être VILLE ou DEPARTEMENT.", assertThrows(IllegalArgumentException.class,
                () -> service.comparer("REGION", "35", "44", Carburant.GAZOLE)).getMessage());
        assertEquals("Le paramètre zone1 est obligatoire.", assertThrows(IllegalArgumentException.class,
                () -> service.comparer("VILLE", " ", "Nantes", Carburant.GAZOLE)).getMessage());
        verifyNoInteractions(client);
    }

    private static Station station(String id, String villeNom, String codeDepartement,
                                   String departementNom, double prix) {
        return station(id, villeNom, codeDepartement, departementNom, prix, true);
    }

    private static Station station(String id, String villeNom, String codeDepartement,
                                   String departementNom, double prix, boolean disponible) {
        Departement departement = new Departement(codeDepartement, departementNom, null);
        Ville ville = new Ville(villeNom, null, departement);
        PrixCarburant prixCarburant = PrixCarburant.builder().carburant(Carburant.GAZOLE)
                .prix(prix).disponible(disponible).build();
        return Station.builder().id(id).ville(ville).carburants(List.of(prixCarburant)).build();
    }
}
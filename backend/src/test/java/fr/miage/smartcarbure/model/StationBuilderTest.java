package fr.miage.smartcarbure.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.miage.smartcarbure.enums.Carburant;
import fr.miage.smartcarbure.enums.TypeEmplacement;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests du builder de Station. */
class StationBuilderTest {

    @Test
    void sansIdLeveIllegalStateException() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> Station.builder().nom("Station").build());
        assertTrue(e.getMessage().contains("identifiant"));
    }

    @Test
    void listesVidesParDefaut() {
        Station s = Station.builder().id("1").build();

        assertNotNull(s.getHoraires());
        assertNotNull(s.getServices());
        assertNotNull(s.getCarburants());
        assertTrue(s.getHoraires().isEmpty());
        assertTrue(s.getServices().isEmpty());
        assertTrue(s.getCarburants().isEmpty());
        assertFalse(s.isAutomate24h());
        assertNull(s.getNom());
    }

    @Test
    void listesNullDonnentListesVides() {
        Station s = Station.builder().id("1").horaires(null).services(null).carburants(null).build();

        assertTrue(s.getHoraires().isEmpty());
        assertTrue(s.getServices().isEmpty());
        assertTrue(s.getCarburants().isEmpty());
    }

    @Test
    void listesNonModifiablesEtCopiees() {
        List<ServiceStation> services = new ArrayList<>(List.of(new ServiceStation("Lavage")));
        Station s = Station.builder().id("1").services(services).build();

        // Modifier la liste d'origine ne change pas la station
        services.add(new ServiceStation("Boutique"));
        assertEquals(1, s.getServices().size());

        // La liste renvoyée ne peut pas être modifiée
        assertThrows(UnsupportedOperationException.class, () -> s.getServices().add(new ServiceStation("Gonflage")));
        assertThrows(UnsupportedOperationException.class, () -> s.getHoraires().clear());
        assertThrows(UnsupportedOperationException.class, () -> s.getCarburants().clear());
    }

    @Test
    void tousLesChampsSontTransmis() {
        Region region = new Region("53", "Bretagne");
        Departement departement = new Departement("35", "Ille-et-Vilaine", region);
        Ville ville = new Ville("Rennes", "35000", departement);
        Position position = new Position(48.1, -1.68, "1 rue X");
        PrixCarburant gazole = PrixCarburant.builder().carburant(Carburant.GAZOLE).prix(1.789).build();
        Horaire lundi = new Horaire(DayOfWeek.MONDAY, false, null, null);

        Station s = Station.builder()
                .id("35000001")
                .nom("Station test")
                .position(position)
                .ville(ville)
                .emplacement(TypeEmplacement.AUTOROUTE)
                .automate24h(true)
                .horaires(List.of(lundi))
                .services(List.of(new ServiceStation("Lavage")))
                .carburants(List.of(gazole))
                .build();

        assertEquals("35000001", s.getId());
        assertEquals("Station test", s.getNom());
        assertSame(position, s.getPosition());
        assertSame(ville, s.getVille());
        assertEquals(TypeEmplacement.AUTOROUTE, s.getEmplacement());
        assertTrue(s.isAutomate24h());
        assertEquals(List.of(lundi), s.getHoraires());
        assertEquals(List.of(new ServiceStation("Lavage")), s.getServices());
        assertSame(gazole, s.getCarburants().get(0));
        assertSame(departement, s.getDepartement());
        assertSame(region, s.getRegion());
    }

    @Test
    void raccourcisGeographiquesNullSiMaillonManquant() {
        assertNull(Station.builder().id("1").build().getDepartement());
        assertNull(Station.builder().id("1").build().getRegion());

        Station sansRegion = Station.builder()
                .id("1")
                .ville(new Ville("Rennes", null, new Departement("35", "Ille-et-Vilaine", null)))
                .build();
        assertEquals("35", sansRegion.getDepartement().getCode());
        assertNull(sansRegion.getRegion());
    }
}

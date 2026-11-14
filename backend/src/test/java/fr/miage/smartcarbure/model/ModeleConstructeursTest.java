package fr.miage.smartcarbure.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

/** Tests des constructeurs (champs obligatoires) et de equals/hashCode des objets du modèle. */
class ModeleConstructeursTest {

    // --- Champs obligatoires ---

    @Test
    void regionCodeEtNomObligatoires() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new Region(null, "Bretagne"));
        assertTrue(e.getMessage().contains("code de la région"));
        assertThrows(NullPointerException.class, () -> new Region("53", null));
    }

    @Test
    void departementCodeEtNomObligatoiresRegionFacultative() {
        assertThrows(NullPointerException.class, () -> new Departement(null, "Ille-et-Vilaine", null));
        assertThrows(NullPointerException.class, () -> new Departement("35", null, null));
        assertDoesNotThrow(() -> new Departement("35", "Ille-et-Vilaine", null));
    }

    @Test
    void villeNomOuCodePostalObligatoire() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Ville(null, null, null));
        assertTrue(e.getMessage().contains("nom ou un code postal"));
        assertDoesNotThrow(() -> new Ville("Rennes", null, null));
        assertDoesNotThrow(() -> new Ville(null, "35000", null));
    }

    @Test
    void serviceNomObligatoire() {
        assertThrows(NullPointerException.class, () -> new ServiceStation(null));
    }

    @Test
    void horaireJourObligatoire() {
        assertThrows(NullPointerException.class, () -> new Horaire(null, true, null, null));
        assertDoesNotThrow(() -> new Horaire(DayOfWeek.MONDAY, false, null, null));
    }

    @Test
    void positionAdresseOuDeuxCoordonnees() {
        assertThrows(IllegalArgumentException.class, () -> new Position(null, null, null));
        assertThrows(IllegalArgumentException.class, () -> new Position(48.1, null, "1 rue X"));
        assertThrows(IllegalArgumentException.class, () -> new Position(null, -1.6, null));
        assertDoesNotThrow(() -> new Position(null, null, "1 rue X"));
        assertDoesNotThrow(() -> new Position(48.1, -1.6, null));
    }

    @Test
    void positionRefuseLesCoordonneesHorsLimitesOuNonFinies() {
        assertThrows(IllegalArgumentException.class, () -> new Position(90.1, 0.0, null));
        assertThrows(IllegalArgumentException.class, () -> new Position(-90.1, 0.0, null));
        assertThrows(IllegalArgumentException.class, () -> new Position(0.0, 180.1, null));
        assertThrows(IllegalArgumentException.class, () -> new Position(0.0, -180.1, null));
        assertThrows(IllegalArgumentException.class, () -> new Position(Double.NaN, 0.0, null));
        assertThrows(IllegalArgumentException.class, () -> new Position(0.0, Double.POSITIVE_INFINITY, null));
        assertDoesNotThrow(() -> new Position(90.0, 180.0, null));
        assertDoesNotThrow(() -> new Position(-90.0, -180.0, null));
    }

    // --- equals / hashCode ---

    @Test
    void equalsEtHashCodeSurTousLesAttributs() {
        Region bretagne = new Region("53", "Bretagne");
        assertEquals(bretagne, new Region("53", "Bretagne"));
        assertEquals(bretagne.hashCode(), new Region("53", "Bretagne").hashCode());
        assertNotEquals(bretagne, new Region("53", "Normandie"));

        Departement d1 = new Departement("35", "Ille-et-Vilaine", bretagne);
        assertEquals(d1, new Departement("35", "Ille-et-Vilaine", new Region("53", "Bretagne")));
        assertNotEquals(d1, new Departement("35", "Ille-et-Vilaine", null));

        Ville v1 = new Ville("Rennes", "35000", d1);
        assertEquals(v1, new Ville("Rennes", "35000", d1));
        assertEquals(v1.hashCode(), new Ville("Rennes", "35000", d1).hashCode());
        assertNotEquals(v1, new Ville("Rennes", "35200", d1));

        assertEquals(new ServiceStation("Lavage"), new ServiceStation("Lavage"));
        assertNotEquals(new ServiceStation("Lavage"), new ServiceStation("Boutique"));

        assertEquals(new Position(48.1, -1.6, "A"), new Position(48.1, -1.6, "A"));
        assertNotEquals(new Position(48.1, -1.6, "A"), new Position(48.1, -1.6, "B"));

        Horaire h = new Horaire(DayOfWeek.MONDAY, false, LocalTime.of(7, 0), LocalTime.of(22, 0));
        assertEquals(h, new Horaire(DayOfWeek.MONDAY, false, LocalTime.of(7, 0), LocalTime.of(22, 0)));
        assertEquals(h.hashCode(), new Horaire(DayOfWeek.MONDAY, false, LocalTime.of(7, 0), LocalTime.of(22, 0)).hashCode());
        assertNotEquals(h, new Horaire(DayOfWeek.MONDAY, true, null, null));
    }

    @Test
    void toStringContientLesValeurs() {
        assertTrue(new Region("53", "Bretagne").toString().contains("Bretagne"));
        assertTrue(new Ville(null, "35000", null).toString().contains("35000"));
        assertTrue(new Horaire(DayOfWeek.SUNDAY, true, null, null).toString().contains("SUNDAY"));
    }
}

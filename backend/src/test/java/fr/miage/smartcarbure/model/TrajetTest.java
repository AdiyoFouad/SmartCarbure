package fr.miage.smartcarbure.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TrajetTest {

    private final Position rennes = new Position(48.1173, -1.6778, null);
    private final Position nantes = new Position(47.2184, -1.5536, null);

    @Test
    void creeUnTrajetValide() {
        Trajet trajet = new Trajet(rennes, nantes, List.of(rennes, nantes), 107.4);

        assertEquals(rennes, trajet.getDepart());
        assertEquals(nantes, trajet.getArrivee());
        assertEquals(2, trajet.getPoints().size());
        assertEquals(107.4, trajet.getDistanceKm());
    }

    @Test
    void laListeDesPointsNEstPasModifiable() {
        List<Position> points = new ArrayList<>(List.of(rennes, nantes));
        Trajet trajet = new Trajet(rennes, nantes, points, 107.4);
        points.clear();

        assertEquals(2, trajet.getPoints().size());
        assertThrows(UnsupportedOperationException.class, () -> trajet.getPoints().add(rennes));
    }

    @Test
    void refuseUnDepartOuUneArriveeNull() {
        assertThrows(NullPointerException.class, () -> new Trajet(null, nantes, List.of(rennes, nantes), 1));
        assertThrows(NullPointerException.class, () -> new Trajet(rennes, null, List.of(rennes, nantes), 1));
    }

    @Test
    void refuseUnePositionSansCoordonnees() {
        Position adresseSeule = new Position(null, null, "Rennes");

        assertThrows(IllegalArgumentException.class,
                () -> new Trajet(adresseSeule, nantes, List.of(rennes, nantes), 1));
        assertThrows(IllegalArgumentException.class,
                () -> new Trajet(rennes, nantes, List.of(rennes, adresseSeule, nantes), 1));
    }

    @Test
    void refuseMoinsDeDeuxPoints() {
        assertThrows(IllegalArgumentException.class, () -> new Trajet(rennes, nantes, List.of(rennes), 1));
        assertThrows(NullPointerException.class, () -> new Trajet(rennes, nantes, null, 1));
    }

    @Test
    void refuseUneDistanceNegativeOuInvalide() {
        assertThrows(IllegalArgumentException.class, () -> new Trajet(rennes, nantes, List.of(rennes, nantes), -1));
        assertThrows(IllegalArgumentException.class,
                () -> new Trajet(rennes, nantes, List.of(rennes, nantes), Double.NaN));
    }
}

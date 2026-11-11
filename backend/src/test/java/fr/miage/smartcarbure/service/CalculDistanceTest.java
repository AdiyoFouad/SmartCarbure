package fr.miage.smartcarbure.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.miage.smartcarbure.model.Position;

class CalculDistanceTest {

    private CalculDistance calculDistance;
    private Position paris;
    private Position londres;
    private Position adresseSeule;

    @BeforeEach
    void setUp() {
        calculDistance = new CalculDistance();
        paris = new Position(48.8566, 2.3522, "Paris");
        londres = new Position(51.5074, -0.1278, "Londres");
        adresseSeule = new Position(null, null, "Paris");
    }

    @Test
    void distanceEntreParisEtLondresEstEnKilometres() {
        assertEquals(343.6, calculDistance.calculerDistance(paris, londres), 0.2);
    }

    @Test
    void distanceEntreUnePositionEtElleMemeEstNulle() {
        assertEquals(0.0, calculDistance.calculerDistance(paris, paris), 0.000001);
    }

    @Test
    void distanceEstSymetrique() {
        assertEquals(calculDistance.calculerDistance(paris, londres),
                calculDistance.calculerDistance(londres, paris), 0.000001);
    }

    @Test
    void lesPositionsSansCoordonneesNePeuventPasEtreUtiliseesPourCalculer() {
        assertThrows(NullPointerException.class,
                () -> calculDistance.calculerDistance(adresseSeule, paris));
    }

    @Test
    void refuseUnePositionNulle() {
        assertThrows(NullPointerException.class,
                () -> calculDistance.calculerDistance(null, paris));
        assertThrows(NullPointerException.class,
                () -> calculDistance.calculerDistance(paris, null));
    }
}

package fr.miage.smartcarbure.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import fr.miage.smartcarbure.enums.Carburant;
import fr.miage.smartcarbure.enums.TypeRupture;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** Tests du builder de PrixCarburant. */
class PrixCarburantBuilderTest {

    @Test
    void sansCarburantLeveIllegalStateException() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> PrixCarburant.builder().prix(1.789).build());
        assertTrue(e.getMessage().contains("carburant"));
    }

    @Test
    void valeursParDefaut() {
        PrixCarburant p = PrixCarburant.builder().carburant(Carburant.E10).build();

        assertEquals(Carburant.E10, p.getCarburant());
        assertTrue(p.isDisponible());
        assertNull(p.getPrix());
        assertNull(p.getDateMaj());
        assertNull(p.getTypeRupture());
        assertNull(p.getDebutRupture());
    }

    @Test
    void tousLesChampsSontTransmis() {
        LocalDateTime maj = LocalDateTime.of(2026, 11, 4, 8, 56, 1);
        LocalDateTime debut = LocalDateTime.of(2026, 11, 5, 10, 0);

        PrixCarburant p = PrixCarburant.builder()
                .carburant(Carburant.E85)
                .prix(0.899)
                .dateMaj(maj)
                .disponible(false)
                .typeRupture(TypeRupture.TEMPORAIRE)
                .debutRupture(debut)
                .build();

        assertEquals(Carburant.E85, p.getCarburant());
        assertEquals(0.899, p.getPrix(), 1e-9);
        assertEquals(maj, p.getDateMaj());
        assertFalse(p.isDisponible());
        assertEquals(TypeRupture.TEMPORAIRE, p.getTypeRupture());
        assertEquals(debut, p.getDebutRupture());
    }
}

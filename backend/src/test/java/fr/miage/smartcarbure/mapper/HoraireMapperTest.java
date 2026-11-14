package fr.miage.smartcarbure.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import fr.miage.smartcarbure.dto.HoraireDTO;
import fr.miage.smartcarbure.model.Horaire;

class HoraireMapperTest {

    private final HoraireMapper mapper = new HoraireMapper();

    @Test
    void convertitHoraireOuvert() {
        HoraireDTO dto = mapper.toDTO(
                new Horaire(DayOfWeek.MONDAY, false, LocalTime.of(8, 0), LocalTime.of(20, 0)));

        assertEquals("lundi", dto.getJour());
        assertFalse(dto.isFerme());
        assertEquals(LocalTime.of(8, 0), dto.getOuverture());
        assertEquals(LocalTime.of(20, 0), dto.getFermeture());
    }

    @Test
    void conserveUnHoraireFermeSansPlage() {
        HoraireDTO dto = mapper.toDTO(new Horaire(DayOfWeek.SUNDAY, true, null, null));

        assertEquals("dimanche", dto.getJour());
        assertTrue(dto.isFerme());
        assertNull(dto.getOuverture());
        assertNull(dto.getFermeture());
    }

}

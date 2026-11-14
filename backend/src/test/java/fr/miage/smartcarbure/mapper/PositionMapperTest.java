package fr.miage.smartcarbure.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import fr.miage.smartcarbure.dto.PositionDTO;
import fr.miage.smartcarbure.model.Position;

class PositionMapperTest {

    private final PositionMapper mapper = new PositionMapper();

    @Test
    void convertitPosition() {
        PositionDTO dto = mapper.toDTO(new Position(48.1, -1.68, "1 rue X"));

        assertEquals(48.1, dto.getLatitude());
        assertEquals(-1.68, dto.getLongitude());
        assertEquals("1 rue X", dto.getAdresse());
    }

    @Test
    void renvoieNullPourUnePositionNulle() {
        assertNull(mapper.toDTO(null));
    }
}

package fr.miage.smartcarbure.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import fr.miage.smartcarbure.dto.PrixCarburantDTO;
import fr.miage.smartcarbure.enums.Carburant;
import fr.miage.smartcarbure.enums.TypeRupture;
import fr.miage.smartcarbure.model.PrixCarburant;

class PrixCarburantMapperTest {

    private final PrixCarburantMapper mapper = new PrixCarburantMapper();

    @Test
    void convertitPrixEtDatesDeParisEnUtc() {
        PrixCarburant prix = PrixCarburant.builder()
                .carburant(Carburant.GAZOLE)
                .prix(1.789)
                .dateMaj(LocalDateTime.of(2026, 11, 4, 8, 56, 1))
                .disponible(false)
                .typeRupture(TypeRupture.TEMPORAIRE)
                .debutRupture(LocalDateTime.of(2026, 11, 4, 9, 0))
                .build();

        PrixCarburantDTO dto = mapper.toDTO(prix);

        assertEquals(Carburant.GAZOLE, dto.getCarburant());
        assertEquals(1.789, dto.getPrix());
        assertEquals(Instant.parse("2026-11-04T07:56:01Z"), dto.getDateMaj());
        assertEquals(Instant.parse("2026-11-04T08:00:00Z"), dto.getDebutRupture());
        assertEquals(TypeRupture.TEMPORAIRE, dto.getTypeRupture());
        assertFalse(dto.isDisponible());
    }

    @Test
    void conserveLesDatesNulles() {
        PrixCarburantDTO dto = mapper.toDTO(
                PrixCarburant.builder().carburant(Carburant.GAZOLE).build());

        assertNull(dto.getDateMaj());
        assertNull(dto.getDebutRupture());
    }

    @Test
    void convertitPrixDepuisDtoEtDatesUtcVersHeureDeParis() {
        PrixCarburantDTO dto = new PrixCarburantDTO();
        dto.setCarburant(Carburant.GAZOLE);
        dto.setPrix(1.789);
        dto.setDateMaj(Instant.parse("2026-11-04T07:56:01Z"));
        dto.setDisponible(false);
        dto.setTypeRupture(TypeRupture.TEMPORAIRE);
        dto.setDebutRupture(Instant.parse("2026-11-04T08:00:00Z"));

        PrixCarburant prix = mapper.fromDTO(dto);

        assertEquals(Carburant.GAZOLE, prix.getCarburant());
        assertEquals(1.789, prix.getPrix());
        assertEquals(LocalDateTime.of(2026, 11, 4, 8, 56, 1), prix.getDateMaj());
        assertEquals(LocalDateTime.of(2026, 11, 4, 9, 0), prix.getDebutRupture());
        assertEquals(TypeRupture.TEMPORAIRE, prix.getTypeRupture());
        assertFalse(prix.isDisponible());
    }
}

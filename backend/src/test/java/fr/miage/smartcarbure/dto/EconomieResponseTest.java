package fr.miage.smartcarbure.dto;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Vérifie que EconomieResponse produit le JSON attendu par le contrat d'API (docs/contrat-api.md). */
class EconomieResponseTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void jsonContientLesQuatreChampsDuContrat() {
        EconomieResponse reponse = new EconomieResponse(
                new BigDecimal("1.749"), new BigDecimal("1.771"), 50, new BigDecimal("1.10"));

        String json = jsonMapper.writeValueAsString(reponse);

        assertTrue(json.contains("\"prixStation\":1.749"), json);
        assertTrue(json.contains("\"prixMoyenLocal\":1.771"), json);
        assertTrue(json.contains("\"litres\":50.0"), json);
        assertTrue(json.contains("\"montant\":1.10"), json);
    }

    @Test
    void montantNegatifPourUnSurcout() {
        EconomieResponse reponse = new EconomieResponse(
                new BigDecimal("1.799"), new BigDecimal("1.766"), 40, new BigDecimal("-1.32"));

        String json = jsonMapper.writeValueAsString(reponse);

        assertTrue(json.contains("\"montant\":-1.32"), json);
    }
}

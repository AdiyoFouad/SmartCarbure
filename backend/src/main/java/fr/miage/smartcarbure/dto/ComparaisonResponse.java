package fr.miage.smartcarbure.dto;

import java.math.BigDecimal;

import fr.miage.smartcarbure.enums.Carburant;

/** Réponse de {@code GET /api/comparer} (F16). */
public record ComparaisonResponse(String type, Carburant carburant, ZoneComparaisonDTO zone1,
                                  ZoneComparaisonDTO zone2, BigDecimal ecart) {
}
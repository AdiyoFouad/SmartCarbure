package fr.miage.smartcarbure.dto;

import java.math.BigDecimal;
import java.util.Map;

import fr.miage.smartcarbure.enums.Carburant;

/** Prix moyen et nombre de stations disponibles dans une zone comparée. */
public record ZoneComparaisonDTO(String nom, BigDecimal prixMoyen, int nombreStations,
								 Map<Carburant, BigDecimal> moyennes,
								 Map<Carburant, Integer> nombreStationsParCarburant,
								 int nombreStationsTotal) {
}
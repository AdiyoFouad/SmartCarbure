package fr.miage.smartcarbure.dto;

import java.math.BigDecimal;

/** Prix moyen et nombre de stations disponibles dans une zone comparée. */
public record ZoneComparaisonDTO(String nom, BigDecimal prixMoyen, int nombreStations) {
}
package fr.miage.smartcarbure.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import lombok.Data;

@Data
public class HoraireDTO {
    private DayOfWeek jour;
    private boolean ferme;
    private LocalTime ouverture;
    private LocalTime fermeture;
}

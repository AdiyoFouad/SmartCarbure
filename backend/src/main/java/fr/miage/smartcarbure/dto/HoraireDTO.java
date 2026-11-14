package fr.miage.smartcarbure.dto;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class HoraireDTO {
    private String jour;

    @JsonIgnore
    private boolean ferme;
    private LocalTime ouverture;
    private LocalTime fermeture;
}

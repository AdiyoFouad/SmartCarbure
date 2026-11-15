package fr.miage.smartcarbure.dto;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

@Data
public class HoraireDTO {
    private String jour;

    private boolean ferme;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime ouverture;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime fermeture;
}

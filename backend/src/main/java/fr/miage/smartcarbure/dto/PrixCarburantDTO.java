package fr.miage.smartcarbure.dto;

import java.time.Instant;

import fr.miage.smartcarbure.enums.Carburant;
import fr.miage.smartcarbure.enums.TypeRupture;
import lombok.Data;

@Data
public class PrixCarburantDTO {
    private Carburant carburant;
    private Double prix;
    private Instant dateMaj;
    private boolean disponible;
    private TypeRupture typeRupture;
    private Instant debutRupture;
}

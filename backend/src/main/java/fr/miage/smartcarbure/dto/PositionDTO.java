package fr.miage.smartcarbure.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class PositionDTO {
    private Double latitude;
    private Double longitude;

    @JsonIgnore
    private String adresse;
}

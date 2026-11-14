package fr.miage.smartcarbure.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class DepartementDTO {
    private String code;
    private String nom;

    @JsonIgnore
    private RegionDTO region;
}

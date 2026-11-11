package fr.miage.smartcarbure.dto;

import lombok.Data;

@Data
public class DepartementDTO {
    private String code;
    private String nom;
    private RegionDTO region;
}

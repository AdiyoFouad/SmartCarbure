package fr.miage.smartcarbure.dto;

import lombok.Data;

@Data
public class VilleDTO {
    private String nom;
    private String codePostal;
    private DepartementDTO departement;
}

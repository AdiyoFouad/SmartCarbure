package fr.miage.smartcarbure.mapper;

import org.springframework.stereotype.Component;

import fr.miage.smartcarbure.dto.HoraireDTO;
import fr.miage.smartcarbure.model.Horaire;

@Component
public class HoraireMapper {

    public HoraireDTO toDTO(Horaire horaire) {
        HoraireDTO dto = new HoraireDTO();
        dto.setJour(horaire.getJour());
        dto.setFerme(horaire.isFerme());
        dto.setOuverture(horaire.getOuverture());
        dto.setFermeture(horaire.getFermeture());
        return dto;
    }

    public Horaire fromDTO(HoraireDTO dto) {
        return new Horaire(dto.getJour(), dto.isFerme(), dto.getOuverture(), dto.getFermeture());
    }
}

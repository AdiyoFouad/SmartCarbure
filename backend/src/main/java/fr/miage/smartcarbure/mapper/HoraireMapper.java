package fr.miage.smartcarbure.mapper;

import org.springframework.stereotype.Component;

import fr.miage.smartcarbure.dto.HoraireDTO;
import fr.miage.smartcarbure.model.Horaire;

import java.time.format.TextStyle;
import java.util.Locale;

@Component
public class HoraireMapper {

    private static final Locale LOCALE_FRANCAIS = Locale.FRENCH;

    public HoraireDTO toDTO(Horaire horaire) {
        HoraireDTO dto = new HoraireDTO();
        dto.setJour(horaire.getJour().getDisplayName(TextStyle.FULL, LOCALE_FRANCAIS));
        dto.setFerme(horaire.isFerme());
        dto.setOuverture(horaire.getOuverture());
        dto.setFermeture(horaire.getFermeture());
        return dto;
    }
}

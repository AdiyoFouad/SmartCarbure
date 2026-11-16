package fr.miage.smartcarbure.service;

import fr.miage.smartcarbure.client.CarburantApiClient;
import fr.miage.smartcarbure.dto.StationDTO;
import fr.miage.smartcarbure.exception.StationIntrouvableException;
import fr.miage.smartcarbure.mapper.StationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Fournit le détail d'une station et de ses prix (F7), y compris les carburants en rupture (F8).
 *
 * <p>Erreurs : station inconnue ou identifiant non numérique → StationIntrouvableException (404) ;
 * API Prix des carburants en panne → ApiIndisponibleException (503), levée par CarburantApiClient.</p>
 */
@Service
@RequiredArgsConstructor
public class StationService {

    private final CarburantApiClient carburantApiClient;
    private final StationMapper stationMapper;

    /**
     * Fiche complète d'une station : tous ses carburants, ruptures comprises.
     *
     * @param id identifiant de la station dans l'API Prix des carburants (par exemple 35000013)
     * @return la station au format du contrat d'API
     * @throws StationIntrouvableException si la station n'existe pas ou si l'identifiant n'est pas numérique
     */
    public StationDTO getFiche(String id) {
        return carburantApiClient.trouverParId(id)
                .map(stationMapper::toDTO)
                .orElseThrow(() -> new StationIntrouvableException(id));
    }
}

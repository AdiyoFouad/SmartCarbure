package fr.miage.smartcarbure.controller;

import fr.miage.smartcarbure.dto.StationDTO;
import fr.miage.smartcarbure.service.StationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expose les routes REST de recherche et de détail des stations.
 *
 * <p>Les erreurs sont transformées en { statut, message } par GlobalExceptionHandler :
 * station inconnue → 404, API Prix des carburants indisponible → 503.</p>
 */
@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
public class StationController {

    /** Injection par constructeur (@RequiredArgsConstructor) ; la recherche (RechercheService) sera ajoutée ici. */
    private final StationService stationService;

    /** GET /api/stations/{id} : fiche complète d'une station (F7, F8). */
    @GetMapping("/{id}")
    public StationDTO getFiche(@PathVariable("id") String id) {
        return stationService.getFiche(id);
    }
}

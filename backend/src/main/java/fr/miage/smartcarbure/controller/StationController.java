package fr.miage.smartcarbure.controller;

import fr.miage.smartcarbure.dto.CritereRechercheDTO;
import fr.miage.smartcarbure.dto.StationDTO;
import fr.miage.smartcarbure.enums.Carburant;
import fr.miage.smartcarbure.enums.TypeTri;
import fr.miage.smartcarbure.service.RechercheService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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

	private final RechercheService rechercheService;

	/** Recherche par coordonnées (F1) ou par ville/code postal (F2). */
	@GetMapping
	public List<StationDTO> rechercher(
			@RequestParam(name = "lat", required = false) Double latitude,
			@RequestParam(name = "lon", required = false) Double longitude,
			@RequestParam(name = "rayon", required = false) Integer rayon,
			@RequestParam(name = "ville", required = false) String ville,
			@RequestParam(name = "carburant", required = false) Carburant carburant,
			@RequestParam(name = "tri", required = false) TypeTri tri) {
		CritereRechercheDTO criteres = new CritereRechercheDTO();
		criteres.setLat(latitude);
		criteres.setLon(longitude);
		criteres.setRayon(rayon);
		criteres.setVille(ville);
		criteres.setCarburant(carburant);
		criteres.setTri(tri == null ? TypeTri.PRIX : tri);
		return rechercheService.rechercher(criteres);
	}

    /** Injection par constructeur (@RequiredArgsConstructor) ; la recherche (RechercheService) sera ajoutée ici. */
    private final StationService stationService;

    /** GET /api/stations/{id} : fiche complète d'une station (F7, F8). */
    @GetMapping("/{id}")
    public StationDTO getFiche(@PathVariable("id") String id) {
        return stationService.getFiche(id);
    }
}

package fr.miage.smartcarbure.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.miage.smartcarbure.dto.ComparaisonResponse;
import fr.miage.smartcarbure.enums.Carburant;
import fr.miage.smartcarbure.service.ComparaisonService;

/** Expose la route REST de comparaison des prix entre deux zones. */
@RestController
@RequestMapping("/api/comparer")
public class ComparaisonController {

	private final ComparaisonService comparaisonService;

	public ComparaisonController(ComparaisonService comparaisonService) {
		this.comparaisonService = comparaisonService;
	}

	@GetMapping
	public ComparaisonResponse comparer(@RequestParam("type") String type,
										@RequestParam("zone1") String zone1,
										@RequestParam("zone2") String zone2,
										@RequestParam("carburant") Carburant carburant) {
		return comparaisonService.comparer(type, zone1, zone2, carburant);
	}
}

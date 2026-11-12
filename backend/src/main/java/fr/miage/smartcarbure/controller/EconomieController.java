package fr.miage.smartcarbure.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.miage.smartcarbure.dto.EconomieResponse;
import fr.miage.smartcarbure.enums.Carburant;
import fr.miage.smartcarbure.service.EconomieService;

/**
 * Expose la route REST du calcul d'économie sur un plein (F15).
 *
 * <p>Exemple : {@code GET /api/economie?idStation=35000001&carburant=GAZOLE&litres=50}</p>
 *
 * <p>Les erreurs sont transformées en réponses { statut, message } par GlobalExceptionHandler :
 * paramètre manquant ou invalide → 400, station inconnue → 404, API Prix des carburants indisponible → 503.</p>
 */
@RestController
@RequestMapping("/api/economie")
public class EconomieController {

    /** Service qui fait le calcul : le contrôleur se contente de lire les paramètres et de renvoyer le résultat. */
    private final EconomieService economieService;

    /** Injection par constructeur : Spring fournit l'EconomieService. */
    public EconomieController(EconomieService economieService) {
        this.economieService = economieService;
    }

    /**
     * Économie (ou surcoût) d'un plein dans la station donnée, par rapport au prix moyen de sa ville.
     *
     * @param idStation identifiant de la station (par exemple 35000001)
     * @param carburant code du carburant (GAZOLE, SP95…) ; Spring convertit le texte en enum, sinon 400
     * @param litres    volume du plein ; un texte non numérique donne un 400
     * @return le résultat du calcul, converti en JSON par Spring
     */
    @GetMapping
    public EconomieResponse calculer(@RequestParam("idStation") String idStation,
                                     @RequestParam("carburant") Carburant carburant,
                                     @RequestParam("litres") double litres) {
        return economieService.calculerPourStation(idStation, carburant, litres);
    }
}

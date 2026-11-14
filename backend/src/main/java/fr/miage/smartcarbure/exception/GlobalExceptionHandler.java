package fr.miage.smartcarbure.exception;

import fr.miage.smartcarbure.dto.ErreurDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Transforme les exceptions en réponses HTTP au format du contrat d'API : { "statut": 404, "message": "…" }.
 *
 * <ul>
 *   <li>Exceptions métier : ApiIndisponibleException → 503, StationIntrouvableException → 404,
 *       UtilisateurDejaExistantException → 409, IdentifiantsInvalidesException → 401,
 *       IllegalArgumentException → 400.</li>
 *   <li>Toute exception annotée {@code @ResponseStatus} : code de l'annotation et message de l'exception.
 *       Chacun peut ainsi créer ses exceptions sans modifier cette classe.</li>
 *   <li>Erreurs de Spring MVC : saisie invalide → 400, route inexistante → 404, méthode non autorisée → 405,
 *       corps qui n'est pas du JSON → 415.</li>
 *   <li>Toute autre exception → 500 avec un message générique.</li>
 * </ul>
 *
 * <p>La réponse ne contient jamais de pile d'appels ni de détail technique. Journalisation : 500 en ERROR
 * avec la pile, 503 en WARN sans pile (la cause est déjà journalisée par CarburantApiClient), 4xx en DEBUG.</p>
 *
 * <p>Les refus de Spring Security (401 sans jeton) n'arrivent pas jusqu'ici : ils sont traités par
 * l'AuthenticationEntryPoint de SecurityConfig.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    static final String MESSAGE_API_INDISPONIBLE =
            "Le service Prix des carburants est temporairement indisponible. Réessayez dans quelques instants.";
    static final String MESSAGE_SAISIE_INVALIDE = "Saisie invalide.";
    static final String MESSAGE_ERREUR_INTERNE = "Erreur interne du serveur.";

    // ------------------------------------------------------------------
    // Exceptions métier
    // ------------------------------------------------------------------

    @ExceptionHandler(ApiIndisponibleException.class)
    public ResponseEntity<ErreurDTO> apiIndisponible(ApiIndisponibleException ex, HttpServletRequest requete) {
        // Le message de l'exception peut contenir l'erreur brute de l'API : il reste dans les logs.
        LOG.warn("503 {} {} : {}", requete.getMethod(), requete.getRequestURI(), ex.getMessage());
        return corps(HttpStatus.SERVICE_UNAVAILABLE, MESSAGE_API_INDISPONIBLE);
    }

    @ExceptionHandler(StationIntrouvableException.class)
    public ResponseEntity<ErreurDTO> stationIntrouvable(StationIntrouvableException ex, HttpServletRequest requete) {
        return erreurClient(HttpStatus.NOT_FOUND, ex.getMessage(), requete);
    }

    @ExceptionHandler(UtilisateurDejaExistantException.class)
    public ResponseEntity<ErreurDTO> utilisateurDejaExistant(UtilisateurDejaExistantException ex,
                                                             HttpServletRequest requete) {
        return erreurClient(HttpStatus.CONFLICT, ex.getMessage(), requete);
    }

    @ExceptionHandler(IdentifiantsInvalidesException.class)
    public ResponseEntity<ErreurDTO> identifiantsInvalides(IdentifiantsInvalidesException ex,
                                                           HttpServletRequest requete) {
        return erreurClient(HttpStatus.UNAUTHORIZED, ex.getMessage(), requete);
    }

    /** Saisie invalide détectée par nos services : leurs messages sont en français et utiles au frontend. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErreurDTO> argumentInvalide(IllegalArgumentException ex, HttpServletRequest requete) {
        return erreurClient(HttpStatus.BAD_REQUEST, messageOuDefaut(ex.getMessage(), MESSAGE_SAISIE_INVALIDE), requete);
    }

    // ------------------------------------------------------------------
    // Erreurs de Spring MVC : saisie invalide (400)
    // ------------------------------------------------------------------

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErreurDTO> parametreManquant(MissingServletRequestParameterException ex,
                                                       HttpServletRequest requete) {
        return erreurClient(HttpStatus.BAD_REQUEST,
                "Paramètre obligatoire manquant : " + ex.getParameterName() + ".", requete);
    }

    /** Paramètre de mauvais type : ?carburant=XYZ, ?rayon=abc… */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErreurDTO> parametreDeMauvaisType(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest requete) {
        StringBuilder message = new StringBuilder("Valeur invalide pour le paramètre ")
                .append(ex.getName()).append(" : ").append(ex.getValue()).append('.');
        Class<?> typeAttendu = ex.getRequiredType();
        if (typeAttendu != null && typeAttendu.isEnum()) {
            String valeurs = Arrays.stream(typeAttendu.getEnumConstants())
                    .map(constante -> ((Enum<?>) constante).name())
                    .collect(Collectors.joining(", "));
            message.append(" Valeurs acceptées : ").append(valeurs).append('.');
        }
        return erreurClient(HttpStatus.BAD_REQUEST, message.toString(), requete);
    }

    /** Corps JSON annoté @Valid qui ne respecte pas ses contraintes. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErreurDTO> corpsInvalide(MethodArgumentNotValidException ex, HttpServletRequest requete) {
        List<String> erreurs = new ArrayList<>();
        for (FieldError erreur : ex.getBindingResult().getFieldErrors()) {
            erreurs.add(erreur.getField() + " : " + erreur.getDefaultMessage());
        }
        ex.getBindingResult().getGlobalErrors()
                .forEach(erreur -> erreurs.add(erreur.getDefaultMessage()));
        return erreurClient(HttpStatus.BAD_REQUEST, joindre(erreurs), requete);
    }

    /** Paramètres de méthode annotés @Min, @Max, @NotBlank… (validation intégrée de Spring MVC). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErreurDTO> parametresInvalides(HandlerMethodValidationException ex,
                                                         HttpServletRequest requete) {
        List<String> erreurs = new ArrayList<>();
        for (ParameterValidationResult resultat : ex.getParameterValidationResults()) {
            String parametre = resultat.getMethodParameter().getParameterName();
            for (MessageSourceResolvable erreur : resultat.getResolvableErrors()) {
                erreurs.add((parametre == null ? "" : parametre + " : ") + erreur.getDefaultMessage());
            }
        }
        return erreurClient(HttpStatus.BAD_REQUEST, joindre(erreurs), requete);
    }

    /** Validation de méthode avec @Validated sur la classe. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErreurDTO> contraintesInvalides(ConstraintViolationException ex,
                                                          HttpServletRequest requete) {
        List<String> erreurs = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String chemin = violation.getPropertyPath() == null ? "" : violation.getPropertyPath().toString();
            String parametre = chemin.contains(".") ? chemin.substring(chemin.lastIndexOf('.') + 1) : chemin;
            erreurs.add((parametre.isEmpty() ? "" : parametre + " : ") + violation.getMessage());
        }
        return erreurClient(HttpStatus.BAD_REQUEST, joindre(erreurs), requete);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErreurDTO> corpsIllisible(HttpMessageNotReadableException ex, HttpServletRequest requete) {
        return erreurClient(HttpStatus.BAD_REQUEST, "Corps de la requête absent ou JSON mal formé.", requete);
    }

    // ------------------------------------------------------------------
    // Erreurs de Spring MVC : route, méthode, type de contenu
    // ------------------------------------------------------------------

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErreurDTO> routeInexistante(Exception ex, HttpServletRequest requete) {
        return erreurClient(HttpStatus.NOT_FOUND,
                "Route inexistante : " + requete.getMethod() + " " + requete.getRequestURI() + ".", requete);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErreurDTO> methodeNonAutorisee(HttpRequestMethodNotSupportedException ex,
                                                         HttpServletRequest requete) {
        return erreurClient(HttpStatus.METHOD_NOT_ALLOWED,
                "Méthode " + ex.getMethod() + " non autorisée pour cette route.", requete);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErreurDTO> typeDeContenuNonSupporte(HttpMediaTypeNotSupportedException ex,
                                                              HttpServletRequest requete) {
        return erreurClient(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Le corps de la requête doit être en JSON.", requete);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErreurDTO> responseStatus(ResponseStatusException ex, HttpServletRequest requete) {
        HttpStatusCode statut = ex.getStatusCode();
        return repondre(statut, messageOuDefaut(ex.getReason(), messageGenerique(statut)), requete, ex);
    }

    // ------------------------------------------------------------------
    // Toutes les autres exceptions
    // ------------------------------------------------------------------

    /**
     * Exception annotée @ResponseStatus : code de l'annotation et message de l'exception.
     * Autre exception de Spring MVC : son propre code avec un message générique.
     * Sinon : 500 avec un message générique.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErreurDTO> autreException(Exception ex, HttpServletRequest requete) {
        ResponseStatus annotation = AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class);
        if (annotation != null) {
            HttpStatus statut = annotation.code();
            String message = messageOuDefaut(ex.getMessage(),
                    messageOuDefaut(annotation.reason(), messageGenerique(statut)));
            return repondre(statut, message, requete, ex);
        }
        if (ex instanceof ErrorResponse erreurSpring) {
            HttpStatusCode statut = erreurSpring.getStatusCode();
            return repondre(statut, messageGenerique(statut), requete, ex);
        }
        return repondre(HttpStatus.INTERNAL_SERVER_ERROR, MESSAGE_ERREUR_INTERNE, requete, ex);
    }

    // ------------------------------------------------------------------
    // Outils
    // ------------------------------------------------------------------

    private ResponseEntity<ErreurDTO> erreurClient(HttpStatus statut, String message, HttpServletRequest requete) {
        return repondre(statut, message, requete, null);
    }

    /** Journalise selon le code puis construit la réponse. */
    private ResponseEntity<ErreurDTO> repondre(HttpStatusCode statut, String message, HttpServletRequest requete,
                                               Exception ex) {
        if (statut.is5xxServerError()) {
            if (statut.value() == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                LOG.error("{} {} {} : {}", statut.value(), requete.getMethod(), requete.getRequestURI(),
                        ex == null ? message : ex.toString(), ex);
            } else {
                LOG.warn("{} {} {} : {}", statut.value(), requete.getMethod(), requete.getRequestURI(),
                        ex == null ? message : ex.toString());
            }
        } else {
            LOG.debug("{} {} {} : {}", statut.value(), requete.getMethod(), requete.getRequestURI(), message);
        }
        return corps(statut, message);
    }

    private static ResponseEntity<ErreurDTO> corps(HttpStatusCode statut, String message) {
        return ResponseEntity.status(statut)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErreurDTO(statut.value(), message));
    }

    private static String messageOuDefaut(String message, String defaut) {
        return message == null || message.isBlank() ? defaut : message;
    }

    private static String joindre(List<String> erreurs) {
        return erreurs.isEmpty() ? MESSAGE_SAISIE_INVALIDE : String.join(" ; ", erreurs);
    }

    /** Message générique en français selon le code (aucun détail technique). */
    static String messageGenerique(HttpStatusCode statut) {
        return switch (statut.value()) {
            case 400 -> MESSAGE_SAISIE_INVALIDE;
            case 401 -> "Authentification requise.";
            case 403 -> "Accès refusé.";
            case 404 -> "Ressource introuvable.";
            case 405 -> "Méthode non autorisée pour cette route.";
            case 409 -> "Conflit avec l'état actuel de la ressource.";
            case 415 -> "Le corps de la requête doit être en JSON.";
            case 503 -> "Service temporairement indisponible.";
            default -> statut.is4xxClientError() ? "Requête invalide." : MESSAGE_ERREUR_INTERNE;
        };
    }
}

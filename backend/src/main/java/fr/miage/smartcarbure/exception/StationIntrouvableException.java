package fr.miage.smartcarbure.exception;

/** Levée quand une station demandée n'existe pas (renvoyée en 404). */
public class StationIntrouvableException extends RuntimeException {

    private final String idStation;

    public StationIntrouvableException(String idStation) {
        super("Station introuvable : " + idStation + ".");
        this.idStation = idStation;
    }

    /** Identifiant de la station introuvable. */
    public String getIdStation() {
        return idStation;
    }
}

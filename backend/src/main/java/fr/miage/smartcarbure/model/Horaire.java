package fr.miage.smartcarbure.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Horaires d'ouverture d'une station pour un jour de la semaine.
 * <ul>
 *   <li>Jour fermé : ferme = true, ouverture et fermeture null.</li>
 *   <li>Horaires inconnus (l'API ne donne aucune plage) : ferme = false, ouverture et fermeture null.</li>
 *   <li>Plusieurs plages dans la journée : on garde la première ouverture et la dernière fermeture.</li>
 * </ul>
 */
public class Horaire {

    private DayOfWeek jour;
    private boolean ferme;
    private LocalTime ouverture;
    private LocalTime fermeture;

    public Horaire() {
    }

    public Horaire(DayOfWeek jour, boolean ferme, LocalTime ouverture, LocalTime fermeture) {
        this.jour = jour;
        this.ferme = ferme;
        this.ouverture = ouverture;
        this.fermeture = fermeture;
    }

    public DayOfWeek getJour() {
        return jour;
    }

    public void setJour(DayOfWeek jour) {
        this.jour = jour;
    }

    public boolean isFerme() {
        return ferme;
    }

    public void setFerme(boolean ferme) {
        this.ferme = ferme;
    }

    public LocalTime getOuverture() {
        return ouverture;
    }

    public void setOuverture(LocalTime ouverture) {
        this.ouverture = ouverture;
    }

    public LocalTime getFermeture() {
        return fermeture;
    }

    public void setFermeture(LocalTime fermeture) {
        this.fermeture = fermeture;
    }

    @Override
    public String toString() {
        return "Horaire{jour=" + jour + ", ferme=" + ferme + ", ouverture=" + ouverture
                + ", fermeture=" + fermeture + "}";
    }
}

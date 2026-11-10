package fr.miage.smartcarbure.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Horaires d'ouverture d'une station pour un jour de la semaine (le jour est obligatoire).
 * <ul>
 *   <li>Jour fermé : ferme = true, ouverture et fermeture null.</li>
 *   <li>Horaires inconnus (l'API ne donne aucune plage) : ferme = false, ouverture et fermeture null.</li>
 *   <li>Plusieurs plages dans la journée : on garde la première ouverture et la dernière fermeture.</li>
 * </ul>
 */
public class Horaire {

    private final DayOfWeek jour;
    private final boolean ferme;
    private final LocalTime ouverture;
    private final LocalTime fermeture;

    public Horaire(DayOfWeek jour, boolean ferme, LocalTime ouverture, LocalTime fermeture) {
        this.jour = Objects.requireNonNull(jour, "Le jour de l'horaire est obligatoire.");
        this.ferme = ferme;
        this.ouverture = ouverture;
        this.fermeture = fermeture;
    }

    public DayOfWeek getJour() {
        return jour;
    }

    public boolean isFerme() {
        return ferme;
    }

    public LocalTime getOuverture() {
        return ouverture;
    }

    public LocalTime getFermeture() {
        return fermeture;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Horaire autre)) {
            return false;
        }
        return jour == autre.jour
                && ferme == autre.ferme
                && Objects.equals(ouverture, autre.ouverture)
                && Objects.equals(fermeture, autre.fermeture);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jour, ferme, ouverture, fermeture);
    }

    @Override
    public String toString() {
        return "Horaire{jour=" + jour + ", ferme=" + ferme + ", ouverture=" + ouverture
                + ", fermeture=" + fermeture + "}";
    }
}

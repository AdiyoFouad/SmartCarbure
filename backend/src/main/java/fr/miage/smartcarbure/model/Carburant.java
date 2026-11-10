package fr.miage.smartcarbure.model;

/** Type de carburant (Gazole, SP95, SP98, E10, E85, GPLc). */
public enum Carburant {

    GAZOLE("Gazole"),
    SP95("SP95"),
    SP98("SP98"),
    E10("E10"),
    E85("E85"),
    GPLC("GPLc");

    /** Nom du carburant tel qu'il est écrit dans l'API Prix des carburants. */
    private final String libelle;

    Carburant(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    /**
     * Retrouve un carburant à partir de son nom dans l'API (« Gazole », « GPLc »…).
     * La casse est ignorée. Renvoie null si le nom est inconnu ou null.
     */
    public static Carburant depuisNomApi(String nom) {
        if (nom == null) {
            return null;
        }
        String nomNettoye = nom.trim();
        for (Carburant carburant : values()) {
            if (carburant.libelle.equalsIgnoreCase(nomNettoye)) {
                return carburant;
            }
        }
        return null;
    }
}

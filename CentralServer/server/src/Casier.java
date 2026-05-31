/**
 * POJO representing a casier sub-document inside a 'bornes' document.
 * {
 *   numero: Number,
 *   taille: Enum ("S", "M", "L"),
 *   etat_occupation: Enum ("VIDE", "OCCUPE"),
 *   etat_materiel: Enum ("OK", "ERREUR_OUVERTURE", "ERREUR_FERMETURE")
 * }
 */
public class Casier {

    private int numero;
    private String taille;           // "S", "M", "L"
    private String etat_occupation;  // "VIDE", "OCCUPE"
    private String etat_materiel;    // "OK", "ERREUR_OUVERTURE", "ERREUR_FERMETURE"

    public Casier() {}

    public Casier(int numero, String taille) {
        this.numero = numero;
        this.taille = taille;
        this.etat_occupation = "VIDE";
        this.etat_materiel = "OK";
    }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }

    public String getTaille() { return taille; }
    public void setTaille(String taille) { this.taille = taille; }

    public String getEtat_occupation() { return etat_occupation; }
    public void setEtat_occupation(String etat_occupation) { this.etat_occupation = etat_occupation; }

    public String getEtat_materiel() { return etat_materiel; }
    public void setEtat_materiel(String etat_materiel) { this.etat_materiel = etat_materiel; }

    @Override
    public String toString() {
        return "casier#" + numero + " taille=" + taille
                + " occupation=" + etat_occupation + " materiel=" + etat_materiel;
    }
}

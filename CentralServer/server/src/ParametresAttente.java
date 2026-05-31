/**
 * POJO representing the 'parametres_attente' sub-document of a Borne.
 * {
 *   delai_A: Number,  // secondes avant buzzer (livreur)
 *   delai_B: Number,  // secondes avant alerte serveur (livreur)
 *   delai_X: Number,  // secondes avant buzzer (client)
 *   delai_Y: Number   // secondes avant alerte serveur (client)
 * }
 */
public class ParametresAttente {

    private int delai_A;
    private int delai_B;
    private int delai_X;
    private int delai_Y;

    public ParametresAttente() {}

    public ParametresAttente(int delai_A, int delai_B, int delai_X, int delai_Y) {
        this.delai_A = delai_A;
        this.delai_B = delai_B;
        this.delai_X = delai_X;
        this.delai_Y = delai_Y;
    }

    public int getDelai_A() { return delai_A; }
    public void setDelai_A(int delai_A) { this.delai_A = delai_A; }

    public int getDelai_B() { return delai_B; }
    public void setDelai_B(int delai_B) { this.delai_B = delai_B; }

    public int getDelai_X() { return delai_X; }
    public void setDelai_X(int delai_X) { this.delai_X = delai_X; }

    public int getDelai_Y() { return delai_Y; }
    public void setDelai_Y(int delai_Y) { this.delai_Y = delai_Y; }

    @Override
    public String toString() {
        return "A=" + delai_A + " B=" + delai_B + " X=" + delai_X + " Y=" + delai_Y;
    }
}

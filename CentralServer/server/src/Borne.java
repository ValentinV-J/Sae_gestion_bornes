import org.bson.types.ObjectId;
import java.util.List;

/**
 * POJO representing a 'bornes' document in MongoDB.
 * {
 *   _id: ObjectId,
 *   nom: String,
 *   adresse: String,
 *   parametres_attente: { delai_A, delai_B, delai_X, delai_Y },
 *   casiers: [ { numero, taille, etat_occupation, etat_materiel } ]
 * }
 */
public class Borne {

    private ObjectId id;
    private String nom;
    private String adresse;
    private ParametresAttente parametres_attente;
    private List<Casier> casiers;

    public Borne() {}

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public ParametresAttente getParametres_attente() { return parametres_attente; }
    public void setParametres_attente(ParametresAttente p) { this.parametres_attente = p; }

    public List<Casier> getCasiers() { return casiers; }
    public void setCasiers(List<Casier> casiers) { this.casiers = casiers; }

    @Override
    public String toString() {
        return id + " nom=" + nom + " adresse=" + adresse;
    }
}

import org.bson.types.ObjectId;

/**
 * POJO representing a 'colis' document in MongoDB.
 * The _id IS the uuid (UUID type).
 *
 * Statut lifecycle: ATTENTE_DEPOT → SCAN_MOBILE_OK → DEPOSE → RETIRE
 *
 * {
 *   _id/uuid: UUID,
 *   statut: Enum ("ATTENTE_DEPOT", "SCAN_MOBILE_OK", "DEPOSE", "RETIRE"),
 *   code_retrait: String (4 digits, generated when statut = DEPOSE),
 *   livreur_id: Reference (ObjectId),
 *   borne_id: Reference (ObjectId),
 *   casier_numero: Number,
 *   date_depot: Timestamp,
 *   date_retrait: Timestamp
 * }
 */
public class Colis {

    private String id;          // UUID stored as _id
    private String statut;      // ATTENTE_DEPOT | SCAN_MOBILE_OK | DEPOSE | RETIRE
    private String code_retrait; // 4-digit code, set when DEPOSE
    private ObjectId livreur_id;
    private ObjectId borne_id;
    private int casier_numero;
    private String taille_colis; // "S", "M", "L" — dimension du colis
    private String date_depot;
    private String date_retrait;

    public Colis() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getCode_retrait() { return code_retrait; }
    public void setCode_retrait(String code_retrait) { this.code_retrait = code_retrait; }

    public ObjectId getLivreur_id() { return livreur_id; }
    public void setLivreur_id(ObjectId livreur_id) { this.livreur_id = livreur_id; }

    public ObjectId getBorne_id() { return borne_id; }
    public void setBorne_id(ObjectId borne_id) { this.borne_id = borne_id; }

    public int getCasier_numero() { return casier_numero; }
    public void setCasier_numero(int casier_numero) { this.casier_numero = casier_numero; }

    public String getTaille_colis() { return taille_colis; }
    public void setTaille_colis(String taille_colis) { this.taille_colis = taille_colis; }

    public String getDate_depot() { return date_depot; }
    public void setDate_depot(String date_depot) { this.date_depot = date_depot; }

    public String getDate_retrait() { return date_retrait; }
    public void setDate_retrait(String date_retrait) { this.date_retrait = date_retrait; }

    @Override
    public String toString() {
        return "uuid=" + id + " statut=" + statut + " casier=" + casier_numero + " code=" + code_retrait;
    }
}

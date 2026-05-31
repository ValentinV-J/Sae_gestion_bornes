import org.bson.types.ObjectId;

/**
 * POJO representing a 'logs' document in MongoDB.
 * {
 *   _id: ObjectId,
 *   horodatage: Timestamp (stored as String ISO),
 *   niveau: Enum ("INFO", "WARNING", "CRITICAL"),
 *   action: String ("OUVERTURE_CASIER", "ECHEC_FERMETURE", "SAISIE_CODE_ERRONE",
 *                   "MISE_A_JOUR_PARAMETRES", "BUZZER_TIMEOUT", ...),
 *   borne_id: Reference (ObjectId, optional),
 *   casier_numero: Number (optional),
 *   details: String (free text, optional)
 * }
 */
public class Log {

    private ObjectId id;
    private String horodatage;
    private String niveau;        // "INFO", "WARNING", "CRITICAL"
    private String action;        // "OUVERTURE_CASIER", "ECHEC_FERMETURE", etc.
    private ObjectId borne_id;    // optional
    private Integer casier_numero; // optional
    private String details;        // optional free text

    public Log() {}

    public Log(String horodatage, String niveau, String action,
               ObjectId borne_id, Integer casier_numero, String details) {
        this.horodatage = horodatage;
        this.niveau = niveau;
        this.action = action;
        this.borne_id = borne_id;
        this.casier_numero = casier_numero;
        this.details = details;
    }

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public String getHorodatage() { return horodatage; }
    public void setHorodatage(String horodatage) { this.horodatage = horodatage; }

    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public ObjectId getBorne_id() { return borne_id; }
    public void setBorne_id(ObjectId borne_id) { this.borne_id = borne_id; }

    public Integer getCasier_numero() { return casier_numero; }
    public void setCasier_numero(Integer casier_numero) { this.casier_numero = casier_numero; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    @Override
    public String toString() {
        return horodatage + " [" + niveau + "] " + action
                + (details != null ? " | " + details : "");
    }
}

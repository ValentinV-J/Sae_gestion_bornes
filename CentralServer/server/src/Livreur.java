import org.bson.types.ObjectId;

/**
 * POJO representing a 'livreurs' document in MongoDB.
 * {
 *   _id: ObjectId,
 *   societe: String,
 *   id_badge_rfid: String
 * }
 */
public class Livreur {

    private ObjectId id;
    private String societe;
    private String id_badge_rfid;

    public Livreur() {}

    public Livreur(String societe, String id_badge_rfid) {
        this.societe = societe;
        this.id_badge_rfid = id_badge_rfid;
    }

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public String getSociete() { return societe; }
    public void setSociete(String societe) { this.societe = societe; }

    public String getId_badge_rfid() { return id_badge_rfid; }
    public void setId_badge_rfid(String id_badge_rfid) { this.id_badge_rfid = id_badge_rfid; }

    @Override
    public String toString() {
        return id + " societe=" + societe + " rfid=" + id_badge_rfid;
    }
}

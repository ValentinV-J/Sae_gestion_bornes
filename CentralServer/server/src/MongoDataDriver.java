import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import static com.mongodb.MongoClientSettings.getDefaultCodecRegistry;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.ne;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

/**
 * Direct MongoDB access implementation of DataDriver.
 * Used for fallback or operations that don't need to pass through the Node API.
 */
public class MongoDataDriver implements DataDriver {

    private String mongoURL;
    private CodecProvider pojoCodecProvider;
    private CodecRegistry pojoCodecRegistry;
    private MongoClient mongoClient;
    private MongoDatabase database;

    // Collections using POJOs
    private MongoCollection<Livreur> livreurs;
    private MongoCollection<Colis>   colis;
    private MongoCollection<Log>     logs;
    // Bornes uses raw Document due to complex nested casiers array
    private MongoCollection<Document> bornes;

    public MongoDataDriver(String mongoURL) {
        this.mongoURL = mongoURL;
        pojoCodecProvider = PojoCodecProvider.builder().automatic(true).build();
        pojoCodecRegistry = fromRegistries(getDefaultCodecRegistry(), fromProviders(pojoCodecProvider));
    }

    public boolean init() {
        mongoClient = MongoClients.create(mongoURL);
        try {
            database = mongoClient.getDatabase("bornesapi").withCodecRegistry(pojoCodecRegistry);
            livreurs = database.getCollection("livreurs", Livreur.class);
            colis    = database.getCollection("colis", Colis.class);
            logs     = database.getCollection("logs", Log.class);
            bornes   = database.getCollection("bornes");
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String generate4DigitCode() {
        Random rnd = new Random();
        return String.format("%04d", rnd.nextInt(10000));
    }

    // -------------------------------------------------------------------------
    // Bornes / Livreurs lookups
    // -------------------------------------------------------------------------

    public synchronized ObjectId getBorneIdByNom(String nomBorne) {
        Document borne = bornes.find(eq("nom", nomBorne)).first();
        return borne != null ? borne.getObjectId("_id") : null;
    }

    public synchronized ObjectId getLivreurIdByRfid(String id_badge_rfid) {
        Livreur livreur = livreurs.find(eq("id_badge_rfid", id_badge_rfid)).first();
        return livreur != null ? livreur.getId() : null;
    }

    // -------------------------------------------------------------------------
    // Livreurs
    // -------------------------------------------------------------------------

    public synchronized String verifierRfid(String id_badge_rfid) {
        Livreur livreur = livreurs.find(eq("id_badge_rfid", id_badge_rfid)).first();
        if (livreur == null) {
            return "ERR badge RFID non reconnu";
        }
        return "OK " + livreur.getSociete();
    }

    // -------------------------------------------------------------------------
    // Colis / Dépôt
    // -------------------------------------------------------------------------

    public synchronized String getCasierPourDepot(ObjectId borneId) {
        // Find a colis in ATTENTE_DEPOT or SCAN_MOBILE_OK for this borne
        Colis c = colis.find(and(
            eq("borne_id", borneId),
            ne("statut", "DEPOSE"),
            ne("statut", "RETIRE")
        )).first();
        if (c == null) {
            return "ERR aucun colis en attente de depot pour cette borne";
        }
        return "OK " + c.getCasier_numero() + " " + c.getId();
    }

    public synchronized String marquerColisDepose(String uuid, ObjectId livreurId) {
        String now = LocalDateTime.now().toString();
        // Generate unique 4-digit code
        String code = generate4DigitCode();
        com.mongodb.client.result.UpdateResult result = colis.updateOne(
            eq("_id", uuid),
            combine(
                set("statut", "DEPOSE"),
                set("code_retrait", code),
                set("livreur_id", livreurId),
                set("date_depot", now)
            )
        );
        if (result.getMatchedCount() == 0) {
            return "ERR colis introuvable (uuid=" + uuid + ")";
        }
        // Mark the casier as OCCUPE
        Colis c = colis.find(eq("_id", uuid)).first();
        if (c != null) {
            bornes.updateOne(
                and(eq("_id", c.getBorne_id()), eq("casiers.numero", c.getCasier_numero())),
                set("casiers.$.etat_occupation", "OCCUPE")
            );
        }
        return "OK " + code;
    }

    public synchronized String getCasierPourRetrait(ObjectId borneId, String code_retrait) {
        Colis c = colis.find(and(
            eq("borne_id", borneId),
            eq("code_retrait", code_retrait),
            eq("statut", "DEPOSE")
        )).first();
        if (c == null) {
            return "ERR code retrait invalide ou colis non disponible";
        }
        return "OK " + c.getCasier_numero() + " " + c.getId();
    }

    public synchronized String marquerColisRetire(String uuid, String queryString) {
        String now = LocalDateTime.now().toString();
        // Get colis first to know the casier
        Colis c = colis.find(eq("_id", uuid)).first();
        if (c == null) return "ERR colis introuvable";

        colis.updateOne(
            eq("_id", uuid),
            combine(set("statut", "RETIRE"), set("date_retrait", now))
        );
        // Free the casier
        bornes.updateOne(
            and(eq("_id", c.getBorne_id()), eq("casiers.numero", c.getCasier_numero())),
            set("casiers.$.etat_occupation", "VIDE")
        );
        return "OK";
    }

    // -------------------------------------------------------------------------
    // Casiers / Problèmes
    // -------------------------------------------------------------------------

    public synchronized String getCasierAlternatif(ObjectId borneId, int casier_numero_defaillant, String taille_colis) {
        // Mark faulty casier
        bornes.updateOne(
            and(eq("_id", borneId), eq("casiers.numero", casier_numero_defaillant)),
            set("casiers.$.etat_materiel", "ERREUR_OUVERTURE")
        );
        // Get the borne document and find a free casier with the right taille
        Document borne = bornes.find(eq("_id", borneId)).first();
        if (borne == null) return "ERR borne introuvable";

        List<Document> casiersList = borne.getList("casiers", Document.class);
        for (Document casier : casiersList) {
            int num = casier.getInteger("numero");
            String occupation = casier.getString("etat_occupation");
            String materiel   = casier.getString("etat_materiel");
            String taille     = casier.getString("taille");

            if (num != casier_numero_defaillant
                    && "VIDE".equals(occupation)
                    && "OK".equals(materiel)
                    && taille.equals(taille_colis)) {
                // Update the colis to point to new casier
                colis.updateOne(
                    and(eq("borne_id", borneId), ne("statut", "DEPOSE"), ne("statut", "RETIRE")),
                    set("casier_numero", num)
                );
                return "OK " + num;
            }
        }
        return "ERR aucun casier alternatif disponible de taille " + taille_colis;
    }

    public synchronized String marquerCasierLibre(ObjectId borneId, int casier_numero) {
        com.mongodb.client.result.UpdateResult result = bornes.updateOne(
            and(eq("_id", borneId), eq("casiers.numero", casier_numero)),
            set("casiers.$.etat_occupation", "VIDE")
        );
        if (result.getMatchedCount() == 0) {
            return "ERR casier introuvable";
        }
        return "OK";
    }

    // -------------------------------------------------------------------------
    // Logs
    // -------------------------------------------------------------------------

    public synchronized String enregistrerLog(String niveau, String action,
                                               ObjectId borneId, Integer casier_numero, String details) {
        String now = LocalDateTime.now().toString();
        Log log = new Log(now, niveau, action, borneId, casier_numero, details);
        logs.insertOne(log);
        return "OK";
    }
}

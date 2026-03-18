import org.bson.types.ObjectId;

/**

 * Two implementations: MongoDataDriver (direct) and HttpDataDriver (via Node API).
 *
 * All methods return a String:
 *   - "OK ..."  on success (with optional data after OK)
 *   - "ERR ..." on failure
 */
public interface DataDriver {

    /** Initialize the driver (connect to DB, collections, etc.) */
    boolean init();

    // --- Livreurs ---

    /**
     * Checks if the RFID badge is recognized.
     * Returns "OK <societe>" or "ERR ..."
     */
    String verifierRfid(String id_badge_rfid);

    // --- Colis / Dépôt ---

    /**
     * Finds a colis in ATTENTE_DEPOT (or SCAN_MOBILE_OK) status for this borne.
     * Returns "OK <casier_numero> <uuid>" or "ERR ..."
     */
    String getCasierPourDepot(ObjectId borneId);

    /**
     * Marks the colis as DEPOSE and generates the 4-digit code_retrait.
     * Returns "OK <code_retrait>" or "ERR ..."
     */
    String marquerColisDepose(String uuid, ObjectId livreurId);

    /**
     * Finds a colis with this code_retrait in DEPOSE status for this borne.
     * Returns "OK <casier_numero> <uuid>" or "ERR ..."
     */
    String getCasierPourRetrait(ObjectId borneId, String code_retrait);

    /**
     * Marks the colis as RETIRE.
     * Returns "OK" or "ERR ..."
     */
    String marquerColisRetire(String uuid);

    // --- Casiers / Problèmes ---

    /**
     * Marks a casier as ERREUR_OUVERTURE and finds an alternative free casier of the same taille.
     * Returns "OK <numeroCasier>" or "ERR ..."
     */
    String getCasierAlternatif(ObjectId borneId, int casier_numero_defaillant, String taille_colis);

    /**
     * Marks a casier as VIDE (after retrait or buzzer timeout).
     * Returns "OK" or "ERR ..."
     */
    String marquerCasierLibre(ObjectId borneId, int casier_numero);

    // --- Bornes ---

    /**
     * Finds the ObjectId of a borne by its nom (the identifier sent by µC).
     * Returns the ObjectId or null if not found.
     */
    ObjectId getBorneIdByNom(String nomBorne);

    /**
     * Finds the ObjectId of a livreur by its RFID badge.
     * Returns the ObjectId or null if not found.
     */
    ObjectId getLivreurIdByRfid(String id_badge_rfid);

    // --- Logs ---

    /**
     * Inserts a log entry.
     * niveau: "INFO" | "WARNING" | "CRITICAL"
     * action: "OUVERTURE_CASIER" | "ECHEC_FERMETURE" | "SAISIE_CODE_ERRONE" |
     *         "BUZZER_TIMEOUT" | "MISE_A_JOUR_PARAMETRES" | ...
     * Returns "OK" or "ERR ..."
     */
    String enregistrerLog(String niveau, String action, ObjectId borneId, Integer casier_numero, String details);
}

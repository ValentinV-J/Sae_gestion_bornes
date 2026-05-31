import org.bson.types.ObjectId;
import java.io.*;
import java.net.*;


/**
 * Thread handling one µC TCP connection.
 *
 * Protocol (text, one message per line):
 *
 * µC → Server:
 *   BADGE_RFID        <nomBorne> <id_badge_rfid>
 *   DEMANDE_DEPOT     <nomBorne>
 *   CODE_RETRAIT      <nomBorne> <code_retrait>
 *   CASIER_FERME      <nomBorne> <casier_numero> <contexte>     (contexte = depot|retrait)
 *   PROBLEME_OUVERTURE <nomBorne> <casier_numero> <taille_colis> <contexte>
 *   BUZZER_TIMEOUT    <nomBorne> <casier_numero> <contexte>
 *
 * Server → µC:
 *   OK                        (generic ack)
 *   OK RFID <societe>         (valid RFID badge)
 *   OK CASIER <numero>        (casier to open)
 *   OK CODE <code_retrait>    (4-digit code sent to customer, for logging)
 *   ERR <message>             (error - display on µC screen)
 *
 * NOTE: <nomBorne> is the "nom" field in the bornes collection (e.g., "Borne IUT Hall A").
 *       The server resolves it to an ObjectId before querying the DB.
 *       The µC's own borne name is configured at startup.
 */
class ThreadServer extends Thread {

    BufferedReader br;
    PrintStream ps;
    Socket sock;
    DataExchanger exchanger;
    int idThread;
    // Cache of borne name -> ObjectId to avoid repeated lookups
    private ObjectId cachedBorneId = null;
    private String cachedBorneName = null;
    // Cache of livreur RFID -> ObjectId
    private ObjectId cachedLivreurId = null;
    private String cachedRfid = null;

    public ThreadServer(int idThread, Socket sock, DataExchanger exchanger) {
        this.idThread = idThread;
        this.sock = sock;
        this.exchanger = exchanger;
    }

    public void run() {
        try {
            br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            ps = new PrintStream(sock.getOutputStream());
        } catch (IOException e) {
            System.err.println("[Thread " + idThread + "] Cannot create streams: " + e.getMessage());
            return;
        }
        requestLoop();
        System.out.println("[Thread " + idThread + "] Connection closed.");
    }

    // -------------------------------------------------------------------------
    // ObjectId resolution helpers (with per-thread caching)
    // -------------------------------------------------------------------------

    private ObjectId resolveBorneId(String nomBorne) {
        if (nomBorne.equals(cachedBorneName) && cachedBorneId != null) {
            return cachedBorneId;
        }
        // Prefer MongoDriver for this lookup (no HTTP route needed for simple lookup)
        ObjectId id = exchanger.getMongoDriver().getBorneIdByNom(nomBorne);
        if (id != null) {
            cachedBorneName = nomBorne;
            cachedBorneId = id;
        }
        return id;
    }

    private ObjectId resolveLivreurId(String rfid) {
        if (rfid.equals(cachedRfid) && cachedLivreurId != null) {
            return cachedLivreurId;
        }
        ObjectId id = exchanger.getMongoDriver().getLivreurIdByRfid(rfid);
        if (id == null) id = exchanger.getHttpDriver().getLivreurIdByRfid(rfid);
        if (id != null) {
            cachedRfid = rfid;
            cachedLivreurId = id;
        }
        return id;
    }

    // -------------------------------------------------------------------------
    // Main request loop
    // -------------------------------------------------------------------------

    public void requestLoop() {
        String req;
        try {
            while (true) {
                req = br.readLine();
                if (req == null || req.isEmpty()) break;
                System.out.println("[Thread " + idThread + "] Received: " + req);
                String[] parts = req.split(" ");
                String type = parts[0];

                switch (type) {
                    case "BADGE_RFID"         -> requestBadgeRfid(parts);
                    case "DEMANDE_DEPOT"      -> requestDemandeDepot(parts);
                    case "CODE_RETRAIT"       -> requestCodeRetrait(parts);
                    case "CASIER_FERME"       -> requestCasierFerme(parts);
                    case "PROBLEME_OUVERTURE" -> requestProblemeOuverture(parts);
                    case "BUZZER_TIMEOUT"     -> requestBuzzerTimeout(parts);
                    default -> {
                        System.err.println("[Thread " + idThread + "] Unknown: " + type);
                        ps.println("ERR type de requete inconnu: " + type);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("[Thread " + idThread + "] Connection error: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // BADGE_RFID <nomBorne> <id_badge_rfid>
    // Livreur scans his RFID badge to enter "depot mode"
    // -------------------------------------------------------------------------
    protected void requestBadgeRfid(String[] parts) throws IOException {
        System.out.println("[Thread " + idThread + "] BADGE_RFID");
        if (parts.length < 3) {
            ps.println("ERR BADGE_RFID: parametres manquants");
            return;
        }
        String nomBorne    = parts[1];
        String id_badge_rfid = parts[2];

        // Use HTTP driver (primary)
        String answer = exchanger.getHttpDriver().verifierRfid(id_badge_rfid);
        // String answer = exchanger.getMongoDriver().verifierRfid(id_badge_rfid);

        System.out.println("[Thread " + idThread + "] BADGE_RFID -> " + answer);

        if (answer.startsWith("OK")) {
            // Cache livreur ID for later marquerColisDepose
            resolveLivreurId(id_badge_rfid);
            // "OK <societe>" -> forward "OK RFID <societe>"
            String societe = answer.substring(3);
            ps.println("OK RFID " + societe);
            // Log event
            ObjectId borneId = resolveBorneId(nomBorne);
            exchanger.getHttpDriver().enregistrerLog("INFO", "BADGE_RFID_OK", borneId, null,
                    "Badge lu: " + id_badge_rfid + " societe: " + societe);
        } else {
            ps.println(answer); // ERR ...
            ObjectId borneId = resolveBorneId(nomBorne);
            exchanger.getHttpDriver().enregistrerLog("WARNING", "BADGE_RFID_INVALIDE", borneId, null,
                    "Badge inconnu: " + id_badge_rfid);
        }
    }

    // -------------------------------------------------------------------------
    // DEMANDE_DEPOT <nomBorne>
    // µC asks which casier to open for the pending deposit
    // -------------------------------------------------------------------------
    protected void requestDemandeDepot(String[] parts) throws IOException {
        System.out.println("[Thread " + idThread + "] DEMANDE_DEPOT");
        if (parts.length < 2) {
            ps.println("ERR DEMANDE_DEPOT: parametres manquants");
            return;
        }
        String nomBorne = parts[1];
        ObjectId borneId = resolveBorneId(nomBorne);
        if (borneId == null) {
            ps.println("ERR borne inconnue: " + nomBorne);
            return;
        }

        String answer = exchanger.getHttpDriver().getCasierPourDepot(borneId);
        // String answer = exchanger.getMongoDriver().getCasierPourDepot(borneId);

        System.out.println("[Thread " + idThread + "] DEMANDE_DEPOT -> " + answer);
        if (answer.startsWith("OK")) {
            // "OK <casier_numero> <uuid>" -> "OK CASIER <casier_numero>"
            String[] okParts = answer.split(" ");
            int casierNum = Integer.parseInt(okParts[1]);
            ps.println("OK CASIER " + casierNum);
            exchanger.getHttpDriver().enregistrerLog("INFO", "OUVERTURE_CASIER", borneId,
                    casierNum, "Casier ouvert pour depot");
        } else {
            ps.println(answer); // ERR ...
        }
    }

    // -------------------------------------------------------------------------
    // CODE_RETRAIT <nomBorne> <code_retrait>
    // Client types their 4-digit code to retrieve their parcel
    // -------------------------------------------------------------------------
    protected void requestCodeRetrait(String[] parts) throws IOException {
        System.out.println("[Thread " + idThread + "] CODE_RETRAIT");
        if (parts.length < 3) {
            ps.println("ERR CODE_RETRAIT: parametres manquants");
            return;
        }
        String nomBorne    = parts[1];
        String code_retrait = parts[2];
        ObjectId borneId = resolveBorneId(nomBorne);
        if (borneId == null) {
            ps.println("ERR borne inconnue: " + nomBorne);
            return;
        }

        String answer = exchanger.getHttpDriver().getCasierPourRetrait(borneId, code_retrait);
        // String answer = exchanger.getMongoDriver().getCasierPourRetrait(borneId, code_retrait);

        System.out.println("[Thread " + idThread + "] CODE_RETRAIT -> " + answer);
        if (answer.startsWith("OK")) {
            String[] okParts = answer.split(" ");
            int casierNum = Integer.parseInt(okParts[1]);
            ps.println("OK CASIER " + casierNum);
            exchanger.getHttpDriver().enregistrerLog("INFO", "OUVERTURE_CASIER", borneId,
                    casierNum, "Casier ouvert pour retrait (code: " + code_retrait + ")");
        } else {
            ps.println(answer); // ERR ...
            exchanger.getHttpDriver().enregistrerLog("WARNING", "SAISIE_CODE_ERRONE", borneId, null,
                    "Code invalide: " + code_retrait);
        }
    }

    // -------------------------------------------------------------------------
    // CASIER_FERME <nomBorne> <casier_numero> <contexte>
    // The casier sensor detected closure (after depot or retrait)
    // -------------------------------------------------------------------------
    protected void requestCasierFerme(String[] parts) throws IOException {
        System.out.println("[Thread " + idThread + "] CASIER_FERME");
        if (parts.length < 4) {
            ps.println("ERR CASIER_FERME: parametres manquants");
            return;
        }
        String nomBorne    = parts[1];
        int casier_numero  = Integer.parseInt(parts[2]);
        String contexte    = parts[3]; // "depot" or "retrait"

        ObjectId borneId = resolveBorneId(nomBorne);
        if (borneId == null) {
            ps.println("ERR borne inconnue: " + nomBorne);
            return;
        }

        String answer;
        String logAction;

        if ("depot".equals(contexte)) {
            // Need the colis UUID — get it from DB via getCasierPourDepot to find the UUID
            // then mark it as DEPOSE (generating the code_retrait)
            String colisInfo = exchanger.getHttpDriver().getCasierPourDepot(borneId);
            if (!colisInfo.startsWith("OK")) {
                ps.println(colisInfo); // ERR
                return;
            }
            String uuid = colisInfo.split(" ")[2];
            answer = exchanger.getHttpDriver().marquerColisDepose(uuid, cachedLivreurId);
            // String answer = exchanger.getMongoDriver().marquerColisDepose(uuid, cachedLivreurId);
            logAction = "DEPOT_CONFIRME";
        } else {
            // retrait: find the colis in this casier and mark as RETIRE
            // We don't have the UUID here, so we use a special approach via the driver
            // The best approach: find by borneId + casier_numero + statut=DEPOSE
            // For HTTP, the API needs a dedicated endpoint
            // We pass UUID="unknown" and let the API figure it out by borneId+casier
            answer = exchanger.getHttpDriver().marquerColisRetire("?borne_id=" + borneId.toHexString()
                    + "&casier_numero=" + casier_numero);
            // String answer = exchanger.getMongoDriver().marquerColisRetire(...);
            logAction = "RETRAIT_CONFIRME";
        }

        System.out.println("[Thread " + idThread + "] CASIER_FERME -> " + answer);
        exchanger.getHttpDriver().enregistrerLog("INFO", logAction, borneId,
                casier_numero, "Casier " + casier_numero + " ferme apres " + contexte);
        ps.println(answer.startsWith("OK") ? "OK" : answer);
    }

    // -------------------------------------------------------------------------
    // PROBLEME_OUVERTURE <nomBorne> <casier_numero> <taille_colis> <contexte>
    // The electro-magnet failed to open the casier
    // -------------------------------------------------------------------------
    protected void requestProblemeOuverture(String[] parts) throws IOException {
        System.out.println("[Thread " + idThread + "] PROBLEME_OUVERTURE");
        if (parts.length < 5) {
            ps.println("ERR PROBLEME_OUVERTURE: parametres manquants (nomBorne casier taille contexte)");
            return;
        }
        String nomBorne    = parts[1];
        int casier_numero  = Integer.parseInt(parts[2]);
        String taille_colis = parts[3]; // "S", "M" ou "L"
        String contexte    = parts[4];

        ObjectId borneId = resolveBorneId(nomBorne);
        if (borneId == null) {
            ps.println("ERR borne inconnue: " + nomBorne);
            return;
        }

        // Log the problem
        exchanger.getHttpDriver().enregistrerLog("CRITICAL", "ERREUR_OUVERTURE_CASIER", borneId,
                casier_numero, "Echec ouverture casier " + casier_numero + " (" + contexte + ")");

        // Try to find an alternative casier
        String answer = exchanger.getHttpDriver().getCasierAlternatif(borneId, casier_numero, taille_colis);
        // String answer = exchanger.getMongoDriver().getCasierAlternatif(borneId, casier_numero, taille_colis);

        System.out.println("[Thread " + idThread + "] PROBLEME_OUVERTURE -> " + answer);
        if (answer.startsWith("OK")) {
            String[] okParts = answer.split(" ");
            int altCasier = Integer.parseInt(okParts[1]);
            ps.println("OK CASIER " + altCasier);
            exchanger.getHttpDriver().enregistrerLog("INFO", "OUVERTURE_CASIER", borneId,
                    altCasier, "Casier alternatif ouvert (remplace casier " + casier_numero + ")");
        } else {
            ps.println(answer); // ERR ...
        }
    }

    // -------------------------------------------------------------------------
    // BUZZER_TIMEOUT <nomBorne> <casier_numero> <contexte>
    // Casier not closed after B seconds (depot) or Y seconds (retrait)
    // -------------------------------------------------------------------------
    protected void requestBuzzerTimeout(String[] parts) throws IOException {
        System.out.println("[Thread " + idThread + "] BUZZER_TIMEOUT");
        if (parts.length < 4) {
            ps.println("ERR BUZZER_TIMEOUT: parametres manquants");
            return;
        }
        String nomBorne    = parts[1];
        int casier_numero  = Integer.parseInt(parts[2]);
        String contexte    = parts[3]; // "depot" or "retrait"

        ObjectId borneId = resolveBorneId(nomBorne);
        if (borneId == null) {
            ps.println("ERR borne inconnue: " + nomBorne);
            return;
        }

        // Log the buzzer timeout
        exchanger.getHttpDriver().enregistrerLog("WARNING", "BUZZER_TIMEOUT", borneId,
                casier_numero, "Casier " + casier_numero + " non ferme apres timeout (" + contexte + ")");

        if ("retrait".equals(contexte)) {
            // Package was probably taken — mark as RETIRE
            String answer = exchanger.getHttpDriver().marquerColisRetire("?borne_id=" + borneId.toHexString()
                    + "&casier_numero=" + casier_numero);
            System.out.println("[Thread " + idThread + "] Auto-RETIRE: " + answer);
            // Also mark casier as free (client may have left with the package)
            exchanger.getHttpDriver().marquerCasierLibre(borneId, casier_numero);
        }
        // For depot: the livreur may have left without closing — just log it

        ps.println("OK");
    }
}

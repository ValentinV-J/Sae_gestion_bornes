import java.io.*;
import java.net.*;

/* COMMENTS
 *
 * Client de test simulant un µC connecté au serveur de centralisation.
 *
 * Schéma MongoDB réel :
 *   - Bornes   : nom (String), casiers[{numero, taille (S/M/L), etat_occupation, etat_materiel}]
 *   - Livreurs : id_badge_rfid, societe
 *   - Colis    : uuid (_id), statut (ATTENTE_DEPOT/SCAN_MOBILE_OK/DEPOSE/RETIRE), code_retrait (4 chiffres)
 *   - Logs     : niveau (INFO/WARNING/CRITICAL), action, borne_id, casier_numero, details
 *
 * Commandes disponibles :
 *
 *   rfid <nomBorne> <id_badge_rfid>
 *     ex: rfid BorneA A1B2C3D4
 *
 *   depot <nomBorne>
 *     ex: depot BorneA
 *
 *   retrait <nomBorne> <code_retrait>     (4 chiffres)
 *     ex: retrait BorneA 4321
 *
 *   ferme <nomBorne> <casier_numero> <contexte>   (contexte = depot|retrait)
 *     ex: ferme BorneA 3 depot
 *
 *   probleme <nomBorne> <casier_numero> <taille> <contexte>   (taille = S|M|L)
 *     ex: probleme BorneA 3 M depot
 *
 *   timeout <nomBorne> <casier_numero> <contexte>
 *     ex: timeout BorneA 3 retrait
 *
 *   quit  → quitte
 */
class MainClient {

    BufferedReader br;
    PrintStream ps;
    Socket sock;
    BufferedReader consoleIn;

    public MainClient(String serverAddr, int port) throws IOException {
        consoleIn = new BufferedReader(new InputStreamReader(System.in));
        sock = new Socket(serverAddr, port);
        br   = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        ps   = new PrintStream(sock.getOutputStream());
    }

    public void mainLoop() {
        boolean stop = false;
        try {
            while (!stop) {
                System.out.print("BornesClient> ");
                String req = consoleIn.readLine();
                if (req == null || "quit".equals(req.trim())) {
                    stop = true;
                    continue;
                }

                String[] parts = req.trim().split(" ");
                if (parts.length == 0 || parts[0].isEmpty()) continue;

                switch (parts[0]) {
                    case "rfid"     -> requestBadgeRfid(parts);
                    case "depot"    -> requestDemandeDepot(parts);
                    case "retrait"  -> requestCodeRetrait(parts);
                    case "ferme"    -> requestCasierFerme(parts);
                    case "probleme" -> requestProblemeOuverture(parts);
                    case "timeout"  -> requestBuzzerTimeout(parts);
                    default -> System.out.println("Commande inconnue. Commandes: rfid, depot, retrait, ferme, probleme, timeout, quit");
                }
            }
        } catch (IOException e) {
            System.out.println("Erreur de communication avec le serveur: " + e.getMessage());
        }
        System.out.println("Client déconnecté.");
    }

    // Sends a request and reads/prints the server response
    private void sendAndReceive(String req) throws IOException {
        System.out.println("[SEND] " + req);
        ps.println(req);
        String answer = br.readLine();
        System.out.println("[RECV] " + (answer != null ? answer : "(no response)"));
    }

    // rfid <borneId> <idRfid>
    protected void requestBadgeRfid(String[] parts) throws IOException {
        if (parts.length < 3) { System.out.println("Usage: rfid <borneId> <idRfid>"); return; }
        sendAndReceive("BADGE_RFID " + parts[1] + " " + parts[2]);
    }

    // depot <borneId>
    protected void requestDemandeDepot(String[] parts) throws IOException {
        if (parts.length < 2) { System.out.println("Usage: depot <borneId>"); return; }
        sendAndReceive("DEMANDE_DEPOT " + parts[1]);
    }

    // retrait <borneId> <code>
    protected void requestCodeRetrait(String[] parts) throws IOException {
        if (parts.length < 3) { System.out.println("Usage: retrait <borneId> <code>"); return; }
        sendAndReceive("CODE_RETRAIT " + parts[1] + " " + parts[2]);
    }

    // ferme <borneId> <numeroCasier> <contexte>
    protected void requestCasierFerme(String[] parts) throws IOException {
        if (parts.length < 4) { System.out.println("Usage: ferme <borneId> <numeroCasier> <contexte>"); return; }
        sendAndReceive("CASIER_FERME " + parts[1] + " " + parts[2] + " " + parts[3]);
    }

    // probleme <borneId> <numeroCasier> <contexte>
    protected void requestProblemeOuverture(String[] parts) throws IOException {
        if (parts.length < 4) { System.out.println("Usage: probleme <borneId> <numeroCasier> <contexte>"); return; }
        sendAndReceive("PROBLEME_OUVERTURE " + parts[1] + " " + parts[2] + " " + parts[3]);
    }

    // timeout <borneId> <numeroCasier> <contexte>
    protected void requestBuzzerTimeout(String[] parts) throws IOException {
        if (parts.length < 4) { System.out.println("Usage: timeout <borneId> <numeroCasier> <contexte>"); return; }
        sendAndReceive("BUZZER_TIMEOUT " + parts[1] + " " + parts[2] + " " + parts[3]);
    }
}

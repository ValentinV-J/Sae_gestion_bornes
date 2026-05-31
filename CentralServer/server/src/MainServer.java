import java.io.*;
import java.net.*;

/*
 * CENTRAL SERVER — Borne de dépôt-retrait de colis
 *
 * Protocole TCP (texte, une requête par ligne) :
 *
 * µC --> Serveur :
 *   BADGE_RFID   <borneId> <idRfid>
 *   DEMANDE_DEPOT   <borneId>
 *   CODE_RETRAIT    <borneId> <codeRetrait>
 *   CASIER_FERME    <borneId> <numeroCasier> <contexte>   (contexte = depot|retrait)
 *   PROBLEME_OUVERTURE  <borneId> <numeroCasier> <contexte>
 *   BUZZER_TIMEOUT  <borneId> <numeroCasier> <contexte>
 *
 * Serveur --> µC :
 *   OK                         (acknowlegement générique)
 *   OK RFID <nom> <prenom>     (badge RFID valide)
 *   OK CASIER <numero>         (numéro de casier à ouvrir)
 *   ERR <message>              (erreur)
 */
class MainServer {

    ServerSocket conn;
    Socket sock;
    int port;
    DataExchanger exchanger;
    int idThread;

    public MainServer(int port, String apiURL, String mongoURL) throws IOException {
        this.port = port;
        conn = new ServerSocket(port, 10);
        idThread = 1;
        exchanger = new DataExchanger(apiURL, mongoURL);
        // Initialize Mongo driver for direct access (used as fallback)
        if (!exchanger.getMongoDriver().init()) {
            System.err.println("[WARN] Cannot reach MongoDB directly. Only HTTP driver will work.");
        }
    }

    public void mainLoop() throws IOException {
        System.out.println("[SERVER] Listening on port " + port);
        while (true) {
            sock = conn.accept();
            System.out.println("[SERVER] New client connected (thread " + idThread + "): "
                    + sock.getInetAddress().getHostAddress());
            ThreadServer t = new ThreadServer(idThread++, sock, exchanger);
            t.start();
        }
    }
}

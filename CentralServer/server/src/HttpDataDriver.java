import org.bson.Document;
import org.bson.types.ObjectId;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.io.*;

/**
 * HTTP-based implementation of DataDriver.
 * Delegates all operations to the Node.js REST API (primary driver).
 */
public class HttpDataDriver implements DataDriver {

    private HttpClient client;
    private String apiURL;

    public HttpDataDriver(String apiURL) {
        this.apiURL = apiURL;
        client = HttpClient.newHttpClient();
    }

    public boolean init() {
        return true;
    }

    // -------------------------------------------------------------------------
    // HTTP helpers
    // -------------------------------------------------------------------------

    private Document getRequest(String route) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiURL + route))
                .header("Content-Type", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            System.out.println("[HTTP GET] " + route + " -> " + response.body());
            return Document.parse(response.body());
        } catch (InterruptedException | IOException e) {
            System.err.println("[HTTP GET] Error: " + e.getMessage());
            return null;
        }
    }

    private Document postRequest(String route, String payload) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiURL + route))
                .header("Content-Type", "application/json")
                .method("POST", HttpRequest.BodyPublishers.ofString(payload))
                .build();
        try {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            System.out.println("[HTTP POST] " + route + " -> " + response.body());
            return Document.parse(response.body());
        } catch (InterruptedException | IOException e) {
            System.err.println("[HTTP POST] Error: " + e.getMessage());
            return null;
        }
    }

    private Document patchRequest(String route, String payload) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiURL + route))
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(payload))
                .build();
        try {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            System.out.println("[HTTP PATCH] " + route + " -> " + response.body());
            return Document.parse(response.body());
        } catch (InterruptedException | IOException e) {
            System.err.println("[HTTP PATCH] Error: " + e.getMessage());
            return null;
        }
    }

    /** Returns null if no error, or the error string if there was one */
    private String checkError(Document doc) {
        if (doc == null) return "ERR impossible de joindre l'API";
        // API returns { error: 0, data: ... } or { error: 1, message: "..." }
        Object errorField = doc.get("error");
        if (errorField != null) {
            int errorCode = (errorField instanceof Integer) ? (Integer) errorField : 0;
            if (errorCode != 0) {
                String msg = doc.getString("message");
                if (msg == null) msg = doc.getString("data");
                return "ERR " + msg;
            }
        }
        // Also handle standard HTTP error bodies
        if (doc.containsKey("message") && !doc.containsKey("data")) {
            return "ERR " + doc.getString("message");
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Bornes / Livreurs lookup (not via HTTP — done via MongoDriver when needed)
    // -------------------------------------------------------------------------

    public synchronized ObjectId getBorneIdByNom(String nomBorne) {
        // Not applicable for HTTP driver — use MongoDataDriver for this lookup
        Document doc = getRequest("/bornes?nom=" + nomBorne);
        String err = checkError(doc);
        if (err != null) return null;
        Document data = (Document) doc.get("data");
        if (data == null) return null;
        return new ObjectId(data.getString("_id"));
    }

    public synchronized ObjectId getLivreurIdByRfid(String id_badge_rfid) {
        Document doc = getRequest("/livreurs/rfid/" + id_badge_rfid);
        String err = checkError(doc);
        if (err != null) return null;
        Document data = (Document) doc.get("data");
        if (data == null) return null;
        return new ObjectId(data.getString("_id"));
    }

    // -------------------------------------------------------------------------
    // Livreurs
    // -------------------------------------------------------------------------

    public synchronized String verifierRfid(String id_badge_rfid) {
        Document doc = getRequest("/livreurs/rfid/" + id_badge_rfid);
        String err = checkError(doc);
        if (err != null) return err;
        Document data = (Document) doc.get("data");
        if (data == null) return "ERR badge RFID non reconnu";
        return "OK " + data.getString("societe");
    }

    // -------------------------------------------------------------------------
    // Colis / Dépôt
    // -------------------------------------------------------------------------

    public synchronized String getCasierPourDepot(ObjectId borneId) {
        Document doc = getRequest("/colis/depot?borne_id=" + borneId.toHexString());
        String err = checkError(doc);
        if (err != null) return err;
        Document data = (Document) doc.get("data");
        if (data == null) return "ERR aucun colis en attente de depot";
        
        String res = "OK " + data.getInteger("casier_numero") + " " + data.getString("uuid");
        Document params = (Document) data.get("parametres");
        if (params != null) {
            res += " " + params.getInteger("A") + " " + params.getInteger("B") 
                + " " + params.getInteger("X") + " " + params.getInteger("Y");
        }
        return res;
    }

    public synchronized String marquerColisDepose(String uuid, ObjectId livreurId) {
        String payload = "{\"livreur_id\": \"" + livreurId.toHexString() + "\"}";
        Document doc = patchRequest("/colis/" + uuid + "/depose", payload);
        String err = checkError(doc);
        if (err != null) return err;
        Document data = (Document) doc.get("data");
        if (data == null) return "ERR reponse API invalide";
        return "OK " + data.getString("code_retrait");
    }

    public synchronized String getCasierPourRetrait(ObjectId borneId, String code_retrait) {
        Document doc = getRequest("/colis/retrait?borne_id=" + borneId.toHexString()
                + "&code_retrait=" + code_retrait);
        String err = checkError(doc);
        if (err != null) return err;
        Document data = (Document) doc.get("data");
        if (data == null) return "ERR code retrait invalide";
        return "OK " + data.getInteger("casier_numero") + " " + data.getString("uuid");
    }

    public synchronized String marquerColisRetire(String uuid, String queryString) {
        String query = queryString != null ? queryString : "";
        Document doc = patchRequest("/colis/" + uuid + "/retire" + query, "{}");
        String err = checkError(doc);
        if (err != null) return err;
        return "OK";
    }

    // -------------------------------------------------------------------------
    // Casiers / Problèmes
    // -------------------------------------------------------------------------

    public synchronized String getCasierAlternatif(ObjectId borneId, int casier_numero_defaillant, String taille_colis) {
        String payload = "{\"casier_defaillant\": " + casier_numero_defaillant
                + ", \"taille\": \"" + taille_colis + "\"}";
        Document doc = postRequest("/bornes/" + borneId.toHexString() + "/casier-alternatif", payload);
        String err = checkError(doc);
        if (err != null) return err;
        Document data = (Document) doc.get("data");
        if (data == null) return "ERR aucun casier alternatif";
        return "OK " + data.getInteger("casier_numero");
    }

    public synchronized String marquerCasierLibre(ObjectId borneId, int casier_numero) {
        String payload = "{\"etat_occupation\": \"VIDE\"}";
        Document doc = patchRequest("/bornes/" + borneId.toHexString()
                + "/casiers/" + casier_numero, payload);
        String err = checkError(doc);
        if (err != null) return err;
        return "OK";
    }

    // -------------------------------------------------------------------------
    // Logs
    // -------------------------------------------------------------------------

    public synchronized String enregistrerLog(String niveau, String action,
                                               ObjectId borneId, Integer casier_numero, String details) {
        String borneJson    = borneId != null ? "\"borne_id\": \"" + borneId.toHexString() + "\", " : "";
        String casierJson   = casier_numero != null ? "\"casier_numero\": " + casier_numero + ", " : "";
        String detailsJson  = details != null ? "\"details\": \"" + details + "\"" : "\"details\": \"\"";
        String payload = "{\"niveau\": \"" + niveau + "\", \"action\": \"" + action + "\", "
                + borneJson + casierJson + detailsJson + "}";
        Document doc = postRequest("/logs", payload);
        String err = checkError(doc);
        if (err != null) return err;
        return "OK";
    }
}

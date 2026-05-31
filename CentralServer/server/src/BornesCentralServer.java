import java.io.*;
import java.net.*;

/**
 * Entry point of the Bornes Central Server.
 *
 * Usage: BornesCentralServer <port> [<apiURL> <mongoURL>]
 *
 * Defaults:
 *   apiURL   = http://localhost:3000/api
 *   mongoURL = mongodb://localhost:27017
 */
class BornesCentralServer {

    public static void main(String[] args) {

        if (args.length < 1) {
            System.out.println("Usage: BornesCentralServer <port> [<apiURL> <mongoURL>]");
            System.exit(1);
        }

        int    port     = Integer.parseInt(args[0]);
        String apiURL   = args.length > 1 ? args[1] : "http://localhost:3000/api";
        String mongoURL = args.length > 2 ? args[2] : "mongodb://localhost:27017";

        System.out.println("[SERVER] Starting Bornes Central Server...");
        System.out.println("[SERVER] Port     : " + port);
        System.out.println("[SERVER] API URL  : " + apiURL);
        System.out.println("[SERVER] Mongo URL: " + mongoURL);

        try {
            MainServer server = new MainServer(port, apiURL, mongoURL);
            server.mainLoop();
        } catch (IOException e) {
            System.err.println("[SERVER] Fatal error: " + e.getMessage());
            System.exit(1);
        }
    }
}

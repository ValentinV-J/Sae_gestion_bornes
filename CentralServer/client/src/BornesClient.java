import java.io.*;
import java.net.*;

/**
 * Entry point for the test client (simulates a µC).
 * Usage: BornesClient <serverAddr> <port>
 */
class BornesClient {

    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Usage: BornesClient <serverAddr> <port>");
            System.exit(1);
        }

        try {
            String serverAddr = args[0];
            int port = Integer.parseInt(args[1]);
            MainClient client = new MainClient(serverAddr, port);
            client.mainLoop();
        } catch (IOException e) {
            System.out.println("Impossible de se connecter au serveur: " + e.getMessage());
            System.exit(1);
        }
    }
}

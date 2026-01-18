import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {

    public static void main(String[] args) {
        int port = 1500;

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Serveren lytter på port " + port);

            while (true) {
                Socket sock = serverSocket.accept();
                System.out.println("Forbindelse oprettet: " + sock);

                // Start en ny tråd pr. client, så main ikke lukker noget bagefter
                new Thread(() -> {
                    GameSession session = new GameSession(sock);
                    session.run();
                }).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

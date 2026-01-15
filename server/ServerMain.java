import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {

    public static void main(String[] args) {

        int port = 1500;

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Serveren er startet og lytter på port " + port);

            while (true) {
                System.out.println("Venter på ny forbindelse...");

                Socket sock = serverSocket.accept();
                System.out.println("Forbindelse oprettet, starter spil");

                Thread t = new Thread(() -> {
                    try {
                        GameSession session = new GameSession(sock);
                        session.run();
                    } catch (Exception e) {
                        System.out.println("Forbindelsen blev afbrudt");
                    } finally {
                        try {
                            sock.close(); 
                        } catch (Exception e) {}
                    }
                });

                t.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

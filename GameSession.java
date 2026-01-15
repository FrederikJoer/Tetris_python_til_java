import java.net.Socket;

public class GameSession {

    private Socket sock;

    public GameSession(Socket sock) { // constructer
        this.sock = sock;
    }

    public void run() {
        System.out.println("GameSession kører for: " + sock);
    }
}


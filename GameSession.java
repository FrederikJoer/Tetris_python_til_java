import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class GameSession {
    private Socket sock;
    private Scanner netin;
    private PrintWriter netout;

    public GameSession(Socket sock) { // constructer
        this.sock = sock;

        try {
            this.netin = new Scanner(sock.getInputStream());
            this.netout = new PrintWriter(sock.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }


        //this.variable = new "anden_klasse"(); - I GameSession klassen private "klasse" variable; 
    }

    public void run() {
        System.out.println("GameSession kører for: " + sock);
    }


    public String fromClient() {
        String fromclient = netin.nextLine();
        System.out.println("from client: " + fromclient);
        return fromclient;
    }

    public void toClient(String msg) {
        System.out.println("to clinet: " + msg);
        netout.print(msg + "\r\n");
        netout.flush();
    }
}


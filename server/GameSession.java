import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class GameSession {
    private Socket sock;
    private Scanner netin;
    private PrintWriter netout;
    private ProtocolMSG protocol;
    private Board gameBoard;

    private Boolean activeGame = false;
    private Boolean chooseName = false;
    private Boolean startGame = false;

    public String playerName = "";
    public String start = "";

    public GameSession(Socket sock) { // constructer
        this.sock = sock;

        try {
            this.netin = new Scanner(sock.getInputStream());
            this.netout = new PrintWriter(sock.getOutputStream());
            this.protocol = new ProtocolMSG();
            this.gameBoard = new Board();

        } catch (Exception e) {
            e.printStackTrace();
        }


        //this.variable = new "anden_klasse"(); - I GameSession klassen private "klasse" variable; 
    }

    public void run() {
        System.out.println("GameSession kører for: " + sock);
        toClient("WELCOME TO TETRIS");

        chooseName = true;
        while(chooseName) {
            toClient("CHOOSE A NAME");
            playerName = fromClient();
            
            if (playerName.isEmpty()) {
                toClient(protocol.wrongName());
            } else {
                chooseName = false;
                startGame = true;
            }
        }

        while(startGame) {
            toClient("WRITE START TO START");

            start = fromClient();

            if (start.equals("START")) {
                activeGame = true;
                startGame = false;
            } else {
                toClient(protocol.unknowStart());
            }
        }


        String[] board = gameBoard.makeBoard();
        System.out.println(board);
        while(activeGame) {
            toClient("BOARD IS " + String.join("", board));
            activeGame = false;
        }

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
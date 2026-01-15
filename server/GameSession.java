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

    public String lastInput = null;
    public String playerName = "";
    public String start = "";

    // FIX: boardLock skal være et felt, ellers findes det ikke i run()
    private final Object boardLock = new Object(); // FIX

    // FIX: board skal være et felt, ellers kan lambda ikke ændre det
    private String[] board = null; // FIX


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
    }

    public void run() {
        System.out.println("GameSession kører for: " + sock);
        toClient("WELCOME TO TETRIS");

        chooseName = true;
        while (chooseName) {
            toClient("CHOOSE A NAME");
            playerName = fromClient();

            if (playerName.isEmpty()) {
                toClient(protocol.wrongName());
            } else {
                chooseName = false;
                startGame = true;
            }
        }

        while (startGame) {
            toClient("WRITE START TO START");

            start = fromClient();

            if (start.equals("START")) {
                activeGame = true;
                startGame = false;
            } else {
                toClient(protocol.unknowStart());
            }
        }

    
        board = gameBoard.makeBoard();
        System.out.println(board);

        startInputThread();

        Thread movement = new Thread(() -> {
            while (activeGame) {
                String gameStatus = ""; // Måske en klasse som holder styr på gamestatus og score
                String score = "";      // det samme som ovenfor

                synchronized (boardLock) {
                   //Her skal der kontaktes updateboardmovement i board
                }

                //Her skal der sendes beskeder til client

                try {
                    Thread.sleep(50); // opdatere hvert 0.05 seukund
                } catch (Exception e) {}
            }
        }); 

        movement.start(); //Threaden bliver startet

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

    public void startInputThread() {
        new Thread(() -> {
            while (true) {
                lastInput = fromClient();
            }
        }).start();
    }
}

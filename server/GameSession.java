import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class GameSession {
    private Socket sock;
    private Scanner netin;
    private PrintWriter netout;
    private ProtocolMSG protocol;
    private Board gameBoard;
    private ActivePiece activePieceMovement;
    private MaskPiece activeMaskPiece;

    private Boolean activeGame = false;
    private Boolean chooseName = false;
    private Boolean startGame = false;

    public String lastInput = null;
    public String playerName = "";
    public String start = "";

    public String pieceType = "";
    //public int globalBoardX = 0;
    //public int globalBoardY = 0;
    //public int localX = 0;
    //public int localY = 0;
    public int pieceX = 0;
    public int pieceY = 0;

    public int startPiecex = 3;
    public int startPiecey = 0;

    private final Object boardLock = new Object(); // FIX
    private String[] board = null; // FIX

    public GameSession(Socket sock) { // constructer
        this.sock = sock;

        try {
            this.netin = new Scanner(sock.getInputStream());
            this.netout = new PrintWriter(sock.getOutputStream());
            this.protocol = new ProtocolMSG();
            this.gameBoard = new Board();
            this.activePieceMovement = new ActivePiece();
            this.activeMaskPiece = new MaskPiece();

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
            toClient("WRITE s TO START");

            start = fromClient();

            if (start.equals("s")) {
                activeGame = true;
                startGame = false;
            } else {
                toClient(protocol.unknowStart());
            }
        }

        board = gameBoard.makeBoard();
        pieceX = startPiecex;
        pieceY = startPiecey;

        System.out.println(board);

        startInputThread();

        Thread movement = new Thread(() -> {
            while (activeGame) {
                String gameStatus = "";
                String score = "";

                synchronized (boardLock) {
                    //System.out.println("Er inden i syncronized for movement");
                }

                try {
                    Thread.sleep(50);
                } catch (Exception e) {}
            }
        });

        Thread gravity = new Thread(() -> {
            while (activeGame) {

                synchronized (boardLock) {
                    pieceType = "LONG";

                    int[][] mask = activeMaskPiece.longMask(0);

                    for (int[] m : mask) {
                        setCell(pieceX + m[0], pieceY + m[1], ".");
                    }

                    boolean collision = false;
                    for (int[] m : mask) {
                        int gx = pieceX + m[0];
                        int gy = (pieceY + 1) + m[1];

                        if (gx < 0 || gx >= 10) collision = true;
                        if (gy < 0 || gy >= 20) collision = true;

                        if (!collision) {
                            int index = gy * 10 + gx;
                            if (!board[index].equals(".")) collision = true;
                        }
                    }

                    if (!collision) {
                        pieceY = pieceY + 1;
                    }

                    for (int[] m : mask) {
                        setCell(pieceX + m[0], pieceY + m[1], "X");
                    }

                    toClient("BOARD IS " + String.join("", board));
                }

                try {
                    Thread.sleep(500);
                } catch (Exception e) {}
            }
        });

        movement.start();
        gravity.start();
    }

    public void setCell(int x, int y, String value) {
        int WIDTH = 10;
        int HEIGHT = board.length / WIDTH;

        if (x < 0 || x >= WIDTH) return;
        if (y < 0 || y >= HEIGHT) return;

        int index = y * WIDTH + x;
        board[index] = value;
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
            while (activeGame) {
                try {
                    lastInput = fromClient();
                } catch (Exception e) {
                    break;
                }
            }
        }).start();
    }
}

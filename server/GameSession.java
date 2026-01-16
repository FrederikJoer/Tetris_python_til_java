import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class GameSession {
    private Socket sock;
    private Scanner netin;
    private PrintWriter netout;

    private Board gameBoard;
    private MaskPiece activeMaskPiece;

    private boolean activeGame = false;
    private boolean chooseName = false;
    private boolean startGame = false;

    public String lastInput = null;
    public String playerName = "";
    public String start = "";

    public int pieceX = 3;
    public int pieceY = 0;

    private final Object boardLock = new Object();
    private String[] board = null;

    public GameSession(Socket sock) {
        this.sock = sock;

        try {
            this.netin = new Scanner(sock.getInputStream());
            this.netout = new PrintWriter(sock.getOutputStream()); // ingen autoflush

            this.gameBoard = new Board();
            this.activeMaskPiece = new MaskPiece();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        System.out.println("GameSession kører for: " + sock);

        send("WELCOME TO TETRIS");

        chooseName = true;
        while (chooseName) {
            send("CHOOSE A NAME");
            playerName = receive();

            if (playerName == null) {
                // client disconnect
                closeQuiet();
                return;
            }

            if (playerName.isEmpty()) {
                send("WRONG NAME");
            } else {
                chooseName = false;
                startGame = true;
            }
        }

        while (startGame) {
            send("WRITE START TO START");
            start = receive();

            if (start == null) {
                // client disconnect
                closeQuiet();
                return;
            }

            if (start.equals("START")) {
                activeGame = true;
                startGame = false;
            } else {
                send("UNKNOWN START");
            }
        }

        board = gameBoard.makeBoard();

        // Input-thread (læser input under spil)
        startInputThread();

        // Gravity-thread (sender board løbende)
        startGravityThread();
    }

    private void startGravityThread() {
        new Thread(() -> {
            while (activeGame) {
                try {
                    synchronized (boardLock) {
                        int[][] mask = activeMaskPiece.longMask(0);

                        // Slet gammel position
                        for (int[] m : mask) {
                            setCell(pieceX + m[0], pieceY + m[1], ".");
                        }

                        // Tjek collision 1 ned
                        boolean collision = false;
                        for (int[] m : mask) {
                            int gx = pieceX + m[0];
                            int gy = (pieceY + 1) + m[1];
                            if (gameBoard.collision(board, gx, gy)) {
                                collision = true;
                            }
                        }

                        if (!collision) {
                            pieceY = pieceY + 1;
                        }

                        // Tegn ny position
                        for (int[] m : mask) {
                            setCell(pieceX + m[0], pieceY + m[1], "X");
                        }

                        // Send board
                        send("BOARD IS: " + String.join("", board));
                    }

                    Thread.sleep(500);

                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }

            System.out.println("Gravity-thread stoppet for: " + sock);
        }).start();
    }

    private void startInputThread() {
        new Thread(() -> {
            while (activeGame) {
                String input = receive();
                if (input == null) {
                    System.out.println("Client disconnected (input-thread).");
                    activeGame = false;
                    closeQuiet();
                    break;
                }
                lastInput = input;
                System.out.println("Client input: " + input);
            }
        }).start();
    }

    private void setCell(int x, int y, String value) {
        if (x < 0 || x >= 10) return;
        if (y < 0 || y >= 20) return;
        board[y * 10 + x] = value;
    }

    private void send(String msg) {
        System.out.println("to client: " + msg);
        netout.println(msg);
        netout.flush();

        // PrintWriter kaster ofte ikke exception – checkError afslører fejl
        if (netout.checkError()) {
            System.out.println("SERVER: skrivefejl (client disconnected?)");
            activeGame = false;
        }
    }

    private String receive() {
        try {
            String line = netin.nextLine();
            System.out.println("from client: " + line);
            return line;
        } catch (Exception e) {
            return null;
        }
    }

    private void closeQuiet() {
        try { sock.close(); } catch (Exception e) {}
    }
}


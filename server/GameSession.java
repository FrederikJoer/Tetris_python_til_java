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

    public int pieceX = 3; //spawn position i x retning
    public int pieceY = 0; //spawn position i y retning

    private final Object boardLock = new Object(); //lås til ændring af board
    private String[] board = null; //selve boardet

    // gemmer mask
    public int[][] mask;

    // gemmer aktiv brik og næste brik og rotationIndex
    public int activePieceId = 0;
    public int nextActivePieceId = 0;
    public int rotationIndex = 0;

    public GameSession(Socket sock) {
        this.sock = sock;

        try {
            this.netin = new Scanner(sock.getInputStream());
            this.netout = new PrintWriter(sock.getOutputStream());
            this.gameBoard = new Board();
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

            if (playerName == null) {
                closeQuiet();
                return;
            }

            if (playerName.isEmpty()) {
                toClient("WRONG NAME");
            } else {
                chooseName = false;
                startGame = true;
            }
        }

        while (startGame) {
            toClient("WRITE START TO START");
            start = fromClient();

            if (start == null) {
                closeQuiet();
                return;
            }

            if (start.equals("START")) {
                activeGame = true;
                startGame = false;
            } else {
                toClient("UNKNOWN START");
            }
        }

        board = gameBoard.makeBoard();

        activePieceId = activeMaskPiece.randomNumber();
        nextActivePieceId = activeMaskPiece.randomNumber();

        rotationIndex = 0;
        mask = activeMaskPiece.getMask(activePieceId, rotationIndex);

        synchronized (boardLock) { //tegner den første brik med låsen
            for (int[] cord : mask) {
                int gx = pieceX + cord[0];
                int gy = pieceY + cord[1];
                setCell(gx, gy);
            }
            toClient("BOARD IS: " + String.join("", board));
        }

        startInputThread(); //starter tråd til at lytte til client uden at blokere de to tick-threads

        startGravityThread();   // starter tick for håndtering af gravity (y-retning)
        startMovementThread();  // starter tick for håndtering af movement (x-retningen og rotation)
    }

    private void startGravityThread() {
        new Thread(() -> {
            while (activeGame) {
                try {
                    synchronized (boardLock) {
                        boolean collisionDown = false;

                        // tjekker collision for alle felter i y++
                        for (int[] cord : mask) {
                            int gx = pieceX + cord[0];
                            int gy = (pieceY + 1) + cord[1];

                            if (gameBoard.collisionBottom(board, gx, gy)) {
                                collisionDown = true;
                            }
                        }

                        if (collisionDown) {
                            //Locker boardet
                            board = gameBoard.lockBoard(board);

                            activePieceId = nextActivePieceId;
                            nextActivePieceId = activeMaskPiece.randomNumber();

                            rotationIndex = 0;

                            //Nulstiller spawn position
                            pieceX = 3;
                            pieceY = 0;

                            mask = activeMaskPiece.getMask(activePieceId, rotationIndex);

                            // Tegner ny brik
                            for (int[] cord : mask) {
                                int gx = pieceX + cord[0];
                                int gy = pieceY + cord[1];
                                setCell(gx, gy);
                            }
                        } else {
                            //Sletter gammel position og tegner piece på en ny position
                            deleteOldPosition();
                            pieceY = pieceY + 1;

                            for (int[] cord : mask) {
                                int gx = pieceX + cord[0];
                                int gy = pieceY + cord[1];
                                setCell(gx, gy);
                            }
                        }
                    }

                    Thread.sleep(500); // tick: hver 0,5 sekund

                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }

            //System.out.println("Gravity thread stopped for: " + sock); //til test
        }).start();
    }

    private void startMovementThread() {
        new Thread(() -> {
            while (activeGame) {
                try {
                    String input = lastInput;
                    lastInput = null;

                    synchronized (boardLock) {
                        if (input != null) {
                            if (input.equals("LEFT")) {
                                boolean collisionLeft = false;

                                for (int[] cord : mask) {
                                    int gx = (pieceX - 1) + cord[0];
                                    int gy = pieceY + cord[1];

                                    if (gameBoard.collisionWall(board, gx, gy)) {
                                        collisionLeft = true;
                                    }
                                }

                                if (!collisionLeft) {
                                    deleteOldPosition();
                                    pieceX = pieceX - 1;

                                    for (int[] cord : mask) {
                                        int gx = pieceX + cord[0];
                                        int gy = pieceY + cord[1];
                                        setCell(gx, gy);
                                    }
                                }
                            }

                            if (input.equals("RIGHT")) {
                                boolean collisionRight = false;

                                for (int[] cord : mask) {
                                    int gx = (pieceX + 1) + cord[0];
                                    int gy = pieceY + cord[1];

                                    if (gameBoard.collisionWall(board, gx, gy)) {
                                        collisionRight = true;
                                    }
                                }

                                if (!collisionRight) {
                                    deleteOldPosition();
                                    pieceX = pieceX + 1;

                                    for (int[] cord : mask) {
                                        int gx = pieceX + cord[0];
                                        int gy = pieceY + cord[1];
                                        setCell(gx, gy);
                                    }
                                }
                            }

                            if (input.equals("ROTATE")) {
                                int nextRotationIndex = (rotationIndex + 1) % 4;

                                int[][] nextMask = activeMaskPiece.getMask(activePieceId, nextRotationIndex);

                                boolean canRotate = true;

                                for (int[] cord : nextMask) {
                                    int gx = pieceX + cord[0];
                                    int gy = pieceY + cord[1];

                                    if (gameBoard.collisionWall(board, gx, gy)) {
                                        canRotate = false;
                                        break;
                                    }
                                }
                                if (canRotate) {
                                    deleteOldPosition();

                                    rotationIndex = nextRotationIndex;
                                    mask = nextMask;

                                    for (int[] cord : mask) {
                                        int gx = pieceX + cord[0];
                                        int gy = pieceY + cord[1];
                                        setCell(gx, gy);
                                    }
                                }
                            }
                        }

                        toClient("BOARD IS: " + String.join("", board));
                    }

                    Thread.sleep(50);

                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        }).start();
    }

    private void startInputThread() {
        new Thread(() -> {
            while (activeGame) {
                String input = fromClient();
                if (input == null) {
                    System.out.println("Client disconnected...");
                    activeGame = false;
                    closeQuiet();
                    break;
                }
                lastInput = input;
                System.out.println("Client input: " + input);
            }
        }).start();
    }

    //metode til at slette det forgående position
    private void deleteOldPosition() {
        for (int[] cord : mask) {
            board[(pieceY + cord[1]) * 10 + (pieceX + cord[0])] = ".";
        }
    }

    //metode til at sette en cell for en brik
    private void setCell(int x, int y) {
        board[y * 10 + x] = "X";
    }

    //Metode til at sende til clienten
    private void toClient(String msg) {
        System.out.println("to client: " + msg);
        netout.println(msg);
        netout.flush();

        if (netout.checkError()) {
            System.out.println("ERROR: connection");
            activeGame = false;
        }
    }

    //Metode til at modtage fra clienten
    private String fromClient() {
        try {
            String line = netin.nextLine();
            System.out.println("from client: " + line);
            return line;
        } catch (Exception e) {
            return null;
        }
    }

    private void closeQuiet() {
        try {
            sock.close();
        } catch (Exception e) {}
    }
}

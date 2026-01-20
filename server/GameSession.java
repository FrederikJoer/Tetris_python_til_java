import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class GameSession {
    private Socket sock;
    private Scanner netin;
    private PrintWriter netout;

    private Board gameBoard;
    private MaskPiece activeMaskPiece;
    private userLog log;

    private boolean activeGame = false;
    private boolean chooseName = false;
    private boolean startGame = false;

    public String lastInput = null;
    public String playerName = "";
    public String start = "";

    public int pieceX = 3; //spawn position i x retning
    public int pieceY = 0; //spawn position i y retning

    public int gravityTick = 500;
    public int movementTick = 50;

    public int score = 0;

    private final int normalGravityTick = 500;
    private final int softGravityTick = 50;
    private int softFramesLeft = 0;

    private final Object boardLock = new Object(); //lås til ændring af board
    private String[] board = null; //selve boardet

    // gemmer mask
    public int[][] mask;

    // gemmer aktiv brik og næste brik og rotationIndex
    public int activePieceId = 0;
    public int nextActivePieceId = 0;
    public int rotationIndex = 0;

    private int rowsClearedLast = 0;

    public int totalRowsCleared = 0;
    public int level_int = 0;

    public GameSession(Socket sock) {
        this.sock = sock;

        try {
            this.netin = new Scanner(sock.getInputStream());
            this.netout = new PrintWriter(sock.getOutputStream());
            this.gameBoard = new Board();
            this.activeMaskPiece = new MaskPiece();
            this.log = new userLog();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        toClient("WELCOME TO TETRIS");

        chooseName = true;

        while (chooseName) {
            toClient("CHOOSE A NAME");
            playerName = fromClient();

            // FIX: score er nu et felt, så vi logger startscore her
            log.main(playerName, score); // FIX

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
            toClient("LEVEL " + level_int);
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

                        for (int[] cord : mask) {
                            int gx = pieceX + cord[0];
                            int gy = (pieceY + 1) + cord[1];

                            if (gameBoard.checkGameOver(board)) {
                                activeGame = false;
                                toClient("GAMEOVER");
                                toClient("YOUR SCORE IS: " + score);
                                break;
                            }

                            if (gameBoard.collisionBottom(board, gx, gy)) {
                                collisionDown = true;
                            }
                        }

                        if (collisionDown) {
                            //Locker boardet
                            board = gameBoard.lockBoard(board, activePieceId); // lock først
                            board = fullrow(board); // fullrow sætter rowsClearedLast

                            if (rowsClearedLast > 0) {

                                totalRowsCleared = totalRowsCleared + rowsClearedLast;

                                int oldLevel = level_int;
                                level_int = level(totalRowsCleared);

                                score += scoreForRows(rowsClearedLast); // behold din score som den er
                                log.main(playerName, score);
                            }

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

                        if (gravityTick < movementTick) {
                            sendGameInfo(board, score, activePieceId, nextActivePieceId); //Sender alt gameinfo til Client
                        }
                    }

                    Thread.sleep(gravityTick);

                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }

        }).start();
    }

    private void startMovementThread() {
        new Thread(() -> {
            while (activeGame) {
                try {
                    String input = lastInput;
                    lastInput = null;

                    synchronized (boardLock) {

                        if (softFramesLeft > 0) {
                            softFramesLeft--;
                            if (softFramesLeft == 0) {
                                gravityTick = normalGravityTick;
                            }
                        }

                        if (input != null) {

                            boolean soft = input.contains("SOFT");
                            boolean left = input.contains("LEFT");
                            boolean rigth = input.contains("RIGHT");
                            boolean roate = input.contains("ROTATE");
                            boolean hard = input.contains("HARD");

                            if (hard) {
                                boolean collisionDownHard = false;

                                while (!collisionDownHard) {

                                    // Tjek om der er kollision hvis vi går 1 ned
                                    for (int[] cord : mask) {
                                        int gx = pieceX + cord[0];
                                        int gy = (pieceY + 1) + cord[1];

                                        if (gameBoard.checkGameOver(board)) {
                                            activeGame = false;
                                            toClient("GAMEOVER");
                                            toClient("YOUR SCORE IS: " + score);
                                            collisionDownHard = true; // så vi også stopper while-loopen
                                            break;
                                        }

                                        if (gameBoard.collisionBottom(board, gx, gy)) {
                                            collisionDownHard = true;
                                            break;
                                        }
                                    }

                                    // Hvis ingen kollision: flyt brikken 1 ned og tegn den igen
                                    if (!collisionDownHard) {
                                        deleteOldPosition();
                                        pieceY = pieceY + 1;

                                        for (int[] cord : mask) {
                                            int gx = pieceX + cord[0];
                                            int gy = pieceY + cord[1];
                                            setCell(gx, gy);
                                        }
                                    }
                                }
                            }


                            if (soft) {
                                gravityTickSet("SOFT");
                            }

                            if (roate) {
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

                            if (left && !rigth) {
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

                            if (rigth && !left) {
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
                        }

                        if (gravityTick >= movementTick) {
                            sendGameInfo(board, score, activePieceId, nextActivePieceId);
                        }
                    }

                    Thread.sleep(movementTick);

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
                    System.out.println("Client disconnected...\r\nName: " + playerName + "\r\nSock: " + sock);
                    activeGame = false;
                    closeQuiet();
                    break;
                }
                lastInput = input;
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

    private void gravityTickSet(String reason) {
        if (reason.equals("SOFT")) {
            gravityTick = softGravityTick;
            softFramesLeft = 2;
        }
    }

    private int level(int totalRowsCleared) {
        return totalRowsCleared / 10;
    }

    // score tabel (1..5 rækker)
    private int scoreForRows(int rows) {
        if (rows == 1) {
            return 40;
        } else if (rows == 2) {
            return 100;
        } else if (rows == 3) {
            return 300;
        } else if (rows == 4) {
            return 1200;
        }
        return 0;
    }

    // fullrow returnerer board + sætter rowsClearedLast
    private String[] fullrow(String[] board) {
        int width = 10;
        int height = 20;

        String[] currentBoard = board.clone();

        boolean foundAnyFullRow = false;
        boolean foundFullRow = true;

        int rowsCleared = 0;

        while (foundFullRow) {
            foundFullRow = false;

            for (int row = height - 1; row >= 0; row--) {
                boolean RowFull = true;

                for (int col = 0; col < width; col++) {
                    int index = row * width + col;
                    if (currentBoard[index].equals(".")) {
                        RowFull = false;
                        break;
                    }
                }

                if (RowFull) {
                    foundFullRow = true;
                    foundAnyFullRow = true;
                    rowsCleared++;

                    for (int shiftRow = row; shiftRow > 0; shiftRow--) {
                        for (int col = 0; col < width; col++) {
                            int currentIndex = shiftRow * width + col;
                            int aboveIndex = (shiftRow - 1) * width + col;
                            currentBoard[currentIndex] = currentBoard[aboveIndex];
                        }
                    }

                    for (int col = 0; col < width; col++) {
                        currentBoard[col] = ".";
                    }

                    row++; // Check same row again
                }
            }
        }

        rowsClearedLast = rowsCleared;

        if (!foundAnyFullRow) {
            rowsClearedLast = 0;
            return board;
        }

        return currentBoard;
    }

    public void sendGameInfo(String[] board, int score, int activePiece, int nextActivePieceId) {
        toClient("BOARD IS: " + String.join("", board));
        toClient(" PIECE IS: " + (activePiece - 1));
        toClient("SCORE IS: " + score);
        toClient("LEVEL " + level_int);

        //toClient("NEXT PIECE IS: " + nextActivePieceId);
    }

    //Metode til at sende til clienten
    private void toClient(String msg) {
        synchronized (netout) {
            System.out.println("to client: " + msg);
            netout.println(msg);
            netout.flush();

            if (netout.checkError()) {
                System.out.println("ERROR: connection");
                activeGame = false;
            }
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
        } catch (Exception e) {
        }
    }
}

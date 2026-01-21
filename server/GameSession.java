import java.io.File;
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

    private int normalGravityTick = 500; // var final
    private final int softGravityTick = 50;
    private int softFramesLeft = 0;
    private int skipMovementTicks = 0;


    private final Object boardLock = new Object(); //lås til ændring af board
    private String[] board = null; //selve boardet

    // gemmer mask
    public int[][] mask;

    // gemmer aktiv brik og næste brik og rotationIndex
    public int activePieceId = 0;
    public int nextActivePieceId = 0;
    public int holdPieceId = 0;
    public boolean holdToCollision = false;
    public int rotationIndex = 0;

    private int rowsClearedLast = 0;

    public int totalRowsCleared = 0;
    public int level_int = 0;

    private boolean threadsStarted = false;

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

            log.main(playerName, score);

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

            toClient(getLeaderboard());
        }

        intiazieGame();
    }


    public void intiazieGame() {
        //
        synchronized (boardLock) {
            //Sikre at alle værie bliver sat til defualt så man kan starte spillet igen
            lastInput = null;
            softFramesLeft = 0;

            score = 0;
            totalRowsCleared = 0;
            level_int = 0;

            pieceX = 3;
            pieceY = 0;
            rotationIndex = 0;

            board = gameBoard.makeBoard();

            activePieceId = activeMaskPiece.randomNumber();
            nextActivePieceId = activeMaskPiece.randomNumber();

            mask = activeMaskPiece.getMask(activePieceId, rotationIndex);

            gravityTickSet("SET"); //gravity bliver sat til default fordi man starte i level 0

            //tegner den første brik med låsen
            for (int[] cord : mask) {
                int gx = pieceX + cord[0];
                int gy = pieceY + cord[1];
                setCell(gx, gy, "X");
            }

            toClient("BOARD IS: " + String.join("", board));
            toClient("LEVEL " + level_int);
        }

        if (!threadsStarted) { //Hvis allerede en aktiv Thread fra tidligere spil skal den bruge den så brugeren ikke starte en ny for hver restart game
            threadsStarted = true;

            startInputThread();  //starter tråd til at lytte til client uden at blokere de to tick-threads
            startGravityThread();   // starter tick for håndtering af gravity (y-retning)
            startMovementThread();  // starter tick for håndtering af movement (x-retningen og rotation)
        }    
    }


    private void startGravityThread() {
        new Thread(() -> {
            while (true) { 
                try {

                    if (!activeGame) {         
                        Thread.sleep(500);     
                        continue;             
                    }                          

                    synchronized (boardLock) {
                        boolean collisionDown = false;

                        for (int[] cord : mask) {
                            int gx = pieceX + cord[0];
                            int gy = (pieceY + 1) + cord[1];

                            if (gameBoard.checkGameOver(board)) {
                                activeGame = false;
                                toClient("GAMEOVER");
                                toClient("SCORE IS: " + score);
                                break;
                            }

                            if (gameBoard.collisionBottom(board, gx, gy)) {
                                collisionDown = true;
                            }
                        }


                        if (activeGame) {// gør at man ikke fortsætter efter gameover i samme tick
                            if (collisionDown) {
                                //Locker boardet
                                board = gameBoard.lockBoard(board, activePieceId); // lock først
                                board = fullrow(board); // fullrow sætter rowsClearedLast

                                if (rowsClearedLast > 0) {
                                    totalRowsCleared = totalRowsCleared + rowsClearedLast;

                                    int oldLevel = level_int;
                                    level_int = level(totalRowsCleared);

                                    //øger gravity når level ændrer sig
                                    if (level_int != oldLevel) {
                                        gravityTickSet("SET");
                                    }

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

                                holdToCollision = false; //sørger for at man først kan bruge hold før den næste collision

                                // Tegner ny brik
                                for (int[] cord : mask) {
                                    int gx = pieceX + cord[0];
                                    int gy = pieceY + cord[1];
                                    setCell(gx, gy, "X");
                                }
                            } else {
                                //Sletter gammel position og tegner piece på en ny position
                                deleteOldPosition();
                                pieceY = pieceY + 1;

                                for (int[] cord : mask) {
                                    int gx = pieceX + cord[0];
                                    int gy = pieceY + cord[1];
                                    setCell(gx, gy, "X");
                                }
                            }

                            if (gravityTick < movementTick) {
                                sendGameInfo(board, score, activePieceId, nextActivePieceId, holdPieceId); //Sender alt gameinfo til Client
                            }
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
            while (true) {
                try {

                    if (!activeGame) {        
                        Thread.sleep(500);      
                        continue;              
                    }                          

                    String input = lastInput;
                    lastInput = null;

                    synchronized (boardLock) {

                        if (softFramesLeft > 0) {
                            softFramesLeft--;
                            if (softFramesLeft == 0) {

                                gravityTick = normalGravityTick;
                            }
                        }

                        if (skipMovementTicks > 0) {
                            skipMovementTicks--;
                            continue;
                        }


                        boolean collisionDownShadow = false;
                        int shadowY = pieceY;

                         for (int i = 0; i < board.length; i++) {
                            if (board[i].equals("#")) {
                                board[i] = ".";
                            }
                        }

                        while (!collisionDownShadow) {

                            for (int[] cord : mask) {
                                int gx = pieceX + cord[0];
                                int gy = (shadowY + 1) + cord[1];

                                if (gameBoard.collisionBottom(board, gx, gy)) {
                                    collisionDownShadow = true;
                                    break;
                                }
                            }

                            if (!collisionDownShadow) {
                                shadowY = shadowY + 1;
                            }
                        }

                        for (int[] cord : mask) {
                            int gx = pieceX + cord[0];
                            int gy = shadowY + cord[1];

                            int index = gy * 10 + gx;
                            if (board[index].equals(".")) {
                                setCell(gx, gy, "#");
                            }
                        }


                        if (input != null) {

                            boolean soft = input.contains("SOFT");
                            boolean left = input.contains("LEFT");
                            boolean rigth = input.contains("RIGHT");
                            boolean roate = input.contains("ROTATE");
                            boolean hard = input.contains("HARD");
                            boolean hold = input.contains("HOLD");

                            if (hold && !holdToCollision) {
                                    int currentHoldPieceId = holdPieceId;
                                    int currentActivePieceId = activePieceId;
                                    holdToCollision = true;

                                    deleteOldPosition();


                                if (holdPieceId == 0) {
                                    holdPieceId = currentActivePieceId;
                                    activePieceId = nextActivePieceId;

                                    
                                    
                                    nextActivePieceId = activeMaskPiece.randomNumber();

                                    rotationIndex = 0;
                                    mask = activeMaskPiece.getMask(activePieceId, rotationIndex);
                                    pieceX = 3;
                                    pieceY = 0;


                                } else {

                                    holdPieceId = currentActivePieceId;
                                    activePieceId = currentHoldPieceId;

                                    nextActivePieceId = activeMaskPiece.randomNumber();

                                    rotationIndex = 0;
                                    mask = activeMaskPiece.getMask(activePieceId, rotationIndex);
                                    pieceX = 3;
                                    pieceY = 0;


                                }
                            }

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
                                    if (!collisionDownHard && activeGame) {
                                        deleteOldPosition();
                                        pieceY = pieceY + 1;

                                        for (int[] cord : mask) {
                                            int gx = pieceX + cord[0];
                                            int gy = pieceY + cord[1];
                                            setCell(gx, gy, "X");
                                        }
                                    }
                                }

                                if (activeGame) {

                                    board = gameBoard.lockBoard(board, activePieceId); // lock først
                                    board = fullrow(board); // fullrow sætter rowsClearedLast

                                    if (rowsClearedLast > 0) {
                                        totalRowsCleared = totalRowsCleared + rowsClearedLast;

                                        int oldLevel = level_int;
                                        level_int = level(totalRowsCleared);

                                        if (level_int != oldLevel) {
                                            gravityTickSet("SET");
                                        }

                                        score += scoreForRows(rowsClearedLast);
                                        log.main(playerName, score);
                                    }

                                    activePieceId = nextActivePieceId;
                                    nextActivePieceId = activeMaskPiece.randomNumber();

                                    rotationIndex = 0;

                                    pieceX = 3;
                                    pieceY = 0;

                                    mask = activeMaskPiece.getMask(activePieceId, rotationIndex);

                                    holdToCollision = false;

                                    for (int[] cord : mask) {
                                        int gx = pieceX + cord[0];
                                        int gy = pieceY + cord[1];
                                        setCell(gx, gy, "X");
                                    }
                                }

                                skipMovementTicks = 1; // så du ikke laver mere movement i samme tick
                            }


                            if (soft) {
                                boolean collisionDownSoftInstant = false;

                                for (int[] cord : mask) {
                                    int gx = pieceX + cord[0];
                                    int gy = (pieceY + 1) + cord[1];

                                    if (gameBoard.collisionBottom(board, gx, gy)) {
                                        collisionDownSoftInstant = true;
                                        break;
                                    }
                                }

                                if (!collisionDownSoftInstant) {
                                    deleteOldPosition();
                                    pieceY = pieceY + 1;

                                    for (int[] cord : mask) {
                                        int gx = pieceX + cord[0];
                                        int gy = pieceY + cord[1];
                                        setCell(gx, gy, "X");
                                    }
                                }

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
                                        setCell(gx, gy, "X");
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
                                        setCell(gx, gy, "X");
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
                                        setCell(gx, gy, "X");
                                    }
                                }
                            }
                        }

                        if (gravityTick >= movementTick) {
                            sendGameInfo(board, score, activePieceId, nextActivePieceId, holdPieceId);
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
            while (true) {
                String input = fromClient();
                if (input == null) {
                    System.out.println("Client disconnected...\r\nName: " + playerName + "\r\nSock: " + sock);
                    activeGame = false;
                    closeQuiet();
                    break;
                }

                if (input.equals("RESTART")) {           
                    activeGame = true;                   
                    intiazieGame();                       
                    continue; 
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
    private void setCell(int x, int y, String value) {
        board[y * 10 + x] = value;
    }

    private void gravityTickSet(String reason) {
        if (reason.equals("SOFT")) {
            gravityTick = softGravityTick; // 50
            softFramesLeft = 2;
        }

        if (reason.equals("SET")) { //normal gravity. Bliver højere for hver level man er i
            int newNormal = (int) (1000 * Math.pow(0.8, level_int));

            // så det ikke bliver 0 eller negativt
            if (newNormal < 1) {
                newNormal = 1;
            }

            normalGravityTick = newNormal;

            if (softFramesLeft <= 0) {
                gravityTick = normalGravityTick;
            }
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

    public void sendGameInfo(String[] board, int score, int activePiece, int nextActivePieceId, int holdPieceId) {
        toClient("BOARD IS: " + String.join("", board));
        toClient("PIECE IS: " + activePiece);
        toClient("NEXT PIECE IS: " + nextActivePieceId + activeMaskPiece.getMask(nextActivePieceId, 0));
        toClient("HOLD PIECE IS: " + holdPieceId + activeMaskPiece.getMask(holdPieceId, 0));
        toClient("SCORE IS: " + score);
        toClient("HIGHSCORE: " + log.fetchHighScore(playerName));
        toClient("LEVEL " + level_int);

        System.out.println(gravityTick);
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

            if (line.equals("CLOSE")) {
                closeQuiet();
            }

            return line;
        } catch (Exception e) {
            return null;
        }
    }

    private String getLeaderboard() {
        StringBuilder leaderboard = new StringBuilder();
        leaderboard.append("LEADERBOARD:");

        try {
            File file = new File("top10.txt");
            if (!file.exists()) {
                return "LEADERBOARD: No scores yet";
            }

            try (Scanner scanner = new Scanner(file)) { // Automatically closes scanner
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    leaderboard.append(line).append(";"); //Seperate lines with ;
                }
            }

        } catch (Exception e) {
            System.err.println("ERROR WHEN LOADING LEADERBOARD: " + e.getMessage());
            return "LEADERBOARD: ERROR";
        }

        return leaderboard.toString();
    }

    private void closeQuiet() {
        try {
            sock.close();
        } catch (Exception e) {
        }
    }
}


//Mangler: ved hard drop skal men ikke kunne lave movements. Shadow med ###
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import javax.swing.*;

public class DisplayBoard extends JFrame {

    // Simple GUI components
    private JTextField textField;
    private JTextField highScoreField;
    private JTextField scoreField;
    private JTextField statusField;
    private JPanel boardPanel;
    private JLabel[][] boardCells;
    private JTextArea leaderboardArea;

    // Network components
    private Socket sock;
    private Scanner netin;
    private PrintWriter netout;

    // Declare variables for storing colors of the tetris-pieces.
    private Color[] pieceColors;
    private int currentPieceType;

    // West side GUI (mini panels)
    private JLabel[][] nextCells;   // 4x4 (NEXT PIECE)
    private JLabel[][] holdCells;   // 4x4 (HOLD)

    public boolean inputLeft = false;
    public boolean inputRigth = false;
    public boolean inputRotate = false;
    public boolean inputSoft = false;
    public boolean inputHard = false;
    public boolean inputHold = false;

    private String nextPiece;
    private String holdPiece;

    // Constructor
    public DisplayBoard(Socket sock, Scanner netin, PrintWriter netout) {
        this.sock = sock;
        this.netin = netin;
        this.netout = netout;

        initializePieceColors();
        setupGUI();
    }

    private void setupGUI() {
        //Create the window as a border-layout.
        setTitle("Tetris");
        setLayout(new BorderLayout());

        // ===================== TOP PANEL =====================
        JPanel topPanel = new JPanel(new BorderLayout());

        JLabel titleLabel = new JLabel(
            "<html>" +
            "<span style='color:#f00000'>T</span>" +// cyan
            "<span style='color:#f0a000'>E</span>" +// purple
            "<span style='color:#f0f000'>T</span>" +// green
            "<span style='color:#00f000'>R</span>" +// red
            "<span style='color:#00f0f0'>I</span>" +// orange
            "<span style='color:#00f0f0'>S</span>" +// yellow
            "</html>",
            SwingConstants.CENTER
        );

        titleLabel.setFont(new Font("Arial", Font.BOLD, 64));
        titleLabel.setOpaque(true);
        Color titleBackGroundColor = new Color(33, 64, 128);
        titleLabel.setBackground(titleBackGroundColor);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));


        topPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel scorePanel = new JPanel();

        highScoreField = new JTextField("High Score: ", 15);
        highScoreField.setEditable(false);
        scorePanel.add(highScoreField);

        scoreField = new JTextField("Score: ", 15);
        scoreField.setEditable(false);
        scorePanel.add(scoreField);

        topPanel.add(scorePanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);

        // ===================== MAIN PANEL =====================
        JPanel mainPanel = new JPanel(new BorderLayout(10, 0)); // 10px horizontal gap

        // ===================== WEST PANEL (2x 4x4 GRIDS + TITLES) =====================
        JPanel westPanel = new JPanel();
        westPanel.setLayout(new BoxLayout(westPanel, BoxLayout.Y_AXIS));

        // tomt mellemrum mellem WEST-bokse og main (board)
        westPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 15)); // 20px til venstre, 15px til højre

        // -------- TOP MINI (NEXT PIECE) --------
        JPanel topMiniWrapper = new JPanel(new BorderLayout());

        JLabel nextTitle = new JLabel("NEXT PIECE", SwingConstants.CENTER);
        nextTitle.setFont(new Font("Arial", Font.BOLD, 12));
        topMiniWrapper.add(nextTitle, BorderLayout.NORTH);

        JPanel topMiniPanel = new JPanel(new GridLayout(4, 4, 1, 1));
        topMiniPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        nextCells = new JLabel[4][4]; // FIX: felt (ikke lokal variabel)
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                nextCells[row][col] = new JLabel();
                nextCells[row][col].setOpaque(true);
                nextCells[row][col].setBackground(Color.WHITE);
                topMiniPanel.add(nextCells[row][col]);
            }
        }

        topMiniWrapper.add(topMiniPanel, BorderLayout.CENTER);

        // -------- BOTTOM MINI (HOLD) --------
        JPanel bottomMiniWrapper = new JPanel(new BorderLayout());

        JLabel holdTitle = new JLabel("HOLD", SwingConstants.CENTER);
        holdTitle.setFont(new Font("Arial", Font.BOLD, 12));
        bottomMiniWrapper.add(holdTitle, BorderLayout.NORTH);

        JPanel bottomMiniPanel = new JPanel(new GridLayout(4, 4, 1, 1));
        bottomMiniPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        holdCells = new JLabel[4][4]; // FIX: felt (ikke lokal variabel)
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                holdCells[row][col] = new JLabel();
                holdCells[row][col].setOpaque(true);
                holdCells[row][col].setBackground(Color.WHITE);
                bottomMiniPanel.add(holdCells[row][col]);
            }
        }

        bottomMiniWrapper.add(bottomMiniPanel, BorderLayout.CENTER);

        // -------- SIZE + ADD TO WEST --------
        Dimension miniSize = new Dimension(160, 160); // FIX: kvadratisk så 4x4 celler ikke bliver stretched
        topMiniWrapper.setPreferredSize(miniSize);
        topMiniWrapper.setMaximumSize(miniSize);
        bottomMiniWrapper.setPreferredSize(miniSize);
        bottomMiniWrapper.setMaximumSize(miniSize);

        westPanel.add(topMiniWrapper);
        westPanel.add(Box.createVerticalGlue()); // NY: presser HOLD ned i bunden
        westPanel.add(bottomMiniWrapper);

        mainPanel.add(westPanel, BorderLayout.WEST);

        // ===================== BOARD =====================
        boardPanel = new JPanel(new GridLayout(20, 10, 1, 1));
        boardPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        boardCells = new JLabel[20][10];

        Dimension boardSize = new Dimension(340, 680);
        boardPanel.setPreferredSize(boardSize);
        boardPanel.setMinimumSize(boardSize);
        boardPanel.setMaximumSize(boardSize);

        for (int row = 0; row < 20; row++) {
            for (int col = 0; col < 10; col++) {
                boardCells[row][col] = new JLabel();
                boardCells[row][col].setOpaque(true);
                boardCells[row][col].setBackground(Color.WHITE);
                boardPanel.add(boardCells[row][col]);
            }
        }

        // ===================== LEADERBOARD =====================
        leaderboardArea = new JTextArea(12, 20);
        leaderboardArea.setText("Waiting for leaderboard...");
        leaderboardArea.setEditable(false);
        JScrollPane leaderboardScroll = new JScrollPane(leaderboardArea);
        leaderboardScroll.setPreferredSize(new Dimension(240, 520)); // FIX: mere naturlig sidebar

        // Add board to center, leaderboard to east
        JPanel boardWrapper = new JPanel(new GridBagLayout()); // NY: wrapper der må vokse
        boardWrapper.add(boardPanel);                          // NY: boardet holdes fast og centreret
        mainPanel.add(boardWrapper, BorderLayout.CENTER);      // NY: wrapperen er CENTER i stedet for boardet

        mainPanel.add(leaderboardScroll, BorderLayout.EAST);

        add(mainPanel, BorderLayout.CENTER);

        // ===================== BOTTOM =====================
        JPanel bottomPanel = new JPanel();
        statusField = new JTextField("Game Status: Active", 15);
        statusField.setEditable(false);
        bottomPanel.add(statusField);
        add(bottomPanel, BorderLayout.SOUTH);

        // ===================== FRAME =====================
        setSize(980, 940);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        //Add key bindings
        newSetupKeyBindings();
    }

    private void newSetupKeyBindings() {
        addKeyListener(new KeyAdapter() {

            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();

                if (keyCode == KeyEvent.VK_LEFT) {
                    inputLeft = true;
                } else if (keyCode == KeyEvent.VK_RIGHT) {
                    inputRigth = true;
                } else if (keyCode == KeyEvent.VK_UP) {
                    inputRotate = true;
                } else if (keyCode == KeyEvent.VK_DOWN) {
                    inputSoft = true;
                } else if (keyCode == KeyEvent.VK_SPACE) {
                    inputHard = true;
                } else if (keyCode == KeyEvent.VK_SHIFT) {
                    inputHold = true;
                }

                inputSend(); // send hver gang
            }

            public void keyReleased(KeyEvent e) {
                int keyCode = e.getKeyCode();

                if (keyCode == KeyEvent.VK_LEFT) {
                    inputLeft = false;
                } else if (keyCode == KeyEvent.VK_RIGHT) {
                    inputRigth = false;
                } else if (keyCode == KeyEvent.VK_UP) {
                    inputRotate = false;
                } else if (keyCode == KeyEvent.VK_DOWN) {
                    inputSoft = false;
                } else if (keyCode == KeyEvent.VK_SPACE) {
                    inputHard = false;
                } else if (keyCode == KeyEvent.VK_SHIFT) {
                    inputHold = false;
                }

                inputSend(); // send hver gang
            }
        });

        setFocusable(true);
        requestFocusInWindow();
    }

    public void inputSend() {
        String inputMSG = "";

        if (inputRigth) {
            inputMSG += "RIGHT + ";
        }
        if (inputLeft) {
            inputMSG += "LEFT + ";
        }
        if (inputRotate) {
            inputMSG += "ROTATE + ";
        }
        if (inputSoft) {
            inputMSG += "SOFT + ";
        }
        if (inputHard) {
            inputMSG += "HARD + ";
        }
        if (inputHold) {
            inputMSG += "HOLD + ";
        }

        if (!inputMSG.equals("")) {
            inputMSG = inputMSG.substring(0, inputMSG.length() - 3);
        } else {
            inputMSG = "NONE";
        }

        sendCommand(inputMSG);
    }

    //Starts the GUI and connects to the server.
    public void sendCommand(String command) {
        try {
            if (netout != null) {
                System.out.println("Sending move: " + command);
                netout.println(command);
                netout.flush();
            }
        } catch (Exception e) {
            System.err.println("Error printing command: " + e.getMessage());
        }
    }

    public void start() {
        setVisible(true);
        connectToServer();
    }

    //Method for connecting to server.
    private void connectToServer() {

        //Thread for connection, so that it doesnt block GUI.
        Thread connectionThread = new Thread(() -> {
            try {
                //Reads two first lines from server. Should contain a welcome message and a name request.
                String welcome = netin.nextLine();
                String enterName = netin.nextLine();

                System.out.println("DEBUG: Received from server - Welcome: " + welcome);
                System.out.println("DEBUG: Received from server - EnterName: " + enterName);

                //Show the welcome-window.
                showWelcome();

            } catch (Exception e) {
                System.err.println("Error in server-connection: " + e.getMessage());
                statusField.setText("Connection error: " + e.getMessage());
            }
        });

        connectionThread.start(); //Start thread
    }

    private void showWelcome() {
        //Create welcome-window.
        JDialog welcomeWindow = new JDialog(this, "Welcome to TETRIS", true);
        welcomeWindow.setLayout(new BorderLayout());
        welcomeWindow.setSize(400, 200);
        welcomeWindow.setLocationRelativeTo(this);

        // Create textlabels
        JLabel title = new JLabel("WELCOME TO TETRIS", SwingConstants.CENTER);
        JLabel name = new JLabel("ENTER YOUR NAME: ");

        //Create textfield to enter name in, and a start-button.
        JTextField nameField = new JTextField(20);
        JButton startButton = new JButton("START GAME");

        // Add an action listener to the start button. If the client has entered a name, start the game.
        startButton.addActionListener(e -> {
            String playerName = nameField.getText().trim();
            if (!playerName.isEmpty()) {
                startGame(playerName);
                welcomeWindow.dispose(); //Close the welcome window
            } else {
                JOptionPane.showMessageDialog(welcomeWindow,
                    "Please enter your name!", "Error", JOptionPane.WARNING_MESSAGE);
            }
        });

        //Add an actionlistener to the name field.
        nameField.addActionListener(e -> startButton.doClick());

        //Controls
        JLabel controlLabel = new JLabel(
            "<html>"
                + "CONTROLS:<br>"
                + "Movement: left and right arrows<br>"
                + "Rotation: Upwards arrow<br>"
                + "Soft drop: Downwards arrow<br>"
                + "Hard drop: Space<br>"
                + "Hold piece: Shift"
                + "</html>"
        );

        // Add the different fields to the window.
        JPanel panel = new JPanel();
        panel.add(name);
        panel.add(nameField);
        panel.add(controlLabel);
        panel.add(startButton);
        welcomeWindow.add(title, BorderLayout.NORTH);
        welcomeWindow.add(panel, BorderLayout.CENTER);

        welcomeWindow.setVisible(true);
    }

    private void startGame(String playerName) {
        //Thread for the game.
        Thread gameThread = new Thread(() -> {

            try {
                System.out.println("DEBUG: Starting game for player: " + playerName);

                //Send information to server in order to start game.
                netout.println(playerName);
                netout.println("START");
                netout.flush();

                //Listen to answers from server continuously
                while (true) {
                    if (netin.hasNextLine()) {
                        String serverMessage = netin.nextLine();
                        if (serverMessage.startsWith("BOARD")) {
                            System.out.println("DEBUG: Recieved board: " + serverMessage);
                        }
                        processMessage(serverMessage);
                    }
                }

            } catch (Exception e) {
                System.err.println("Connection error: " + e.getMessage());
                SwingUtilities.invokeLater(() ->
                    statusField.setText("Game error: " + e.getMessage()));
            }

        });

        gameThread.start(); //Start thread
    }

    //Method to process message from server
    private void processMessage(String serverMessage) {
        serverMessage = serverMessage.trim(); // så " PIECE..." også matches som "PIECE..."
        System.out.println("DEBUG: Processing message: " + serverMessage);

        // To set the Board in the GUI.
        if (serverMessage.startsWith("BOARD")) {
            System.out.println("DEBUG: Board message detected");
            updateBoard(serverMessage);

        } else if (serverMessage.startsWith("PIECE")) {
            System.err.println("Piece detected: " + serverMessage);
            String pieceString = serverMessage.substring(serverMessage.lastIndexOf(" ") + 1);
            currentPieceType = Integer.parseInt(pieceString) - 1;
            System.out.println("Current piece set to: " + currentPieceType);

        } else if (serverMessage.startsWith("NEXT PIECE")) {
            nextPiece = serverMessage.substring(serverMessage.lastIndexOf(" ") + 1).trim();

        } else if (serverMessage.startsWith("HOLD PIECE")) {
            holdPiece = serverMessage.substring(serverMessage.lastIndexOf(" ") + 1).trim();

            int nextId = Integer.parseInt(nextPiece);
            int holdId = Integer.parseInt(holdPiece);

            updateMiniPanel(nextId, holdId);

        } else if (serverMessage.startsWith("SCORE")) {
            System.out.println("DEBUG: Score message detected: " + serverMessage);
            scoreField.setText(serverMessage);

        } else if (serverMessage.startsWith("HIGHSCORE")) {
            highScoreField.setText(serverMessage);

        } else if (serverMessage.startsWith("GAME OVER")) {
            System.out.println("DEBUG: Game over message detected");
            statusField.setText("GAME OVER: " + serverMessage.substring(10));
            scoreField.setBackground(Color.GREEN);

        } else if (serverMessage.startsWith("LEADERBOARD")) {
            serverMessage = serverMessage.substring(12);
            serverMessage = serverMessage.replaceAll(";", "\n");
            leaderboardArea.setText("LEADERBOARD\n" + serverMessage);
            leaderboardArea.setCaretPosition(0);

        } else if (serverMessage.startsWith("LEVEL")) {
            statusField.setText(serverMessage);

        } else {
            System.out.println("DEBUG: Unknown message format");
        }
    }

    //Updates the board in the GUI
    private void updateBoard(String serverMessage) {
        String board = serverMessage.substring(10);
        System.out.println("Setting board: " + board);

        for (int i = 0; i < 200; i++) {
            int row = i / 10;
            int col = i % 10;

            if (row < 20 && col < 10) {
                char cell = board.charAt(i);

                if (cell == 'X') {
                    boardCells[row][col].setBackground(pieceColors[currentPieceType]);
                } else if (cell >= '1' && cell <= '7') {
                    boardCells[row][col].setBackground(pieceColors[cell - '1']);
                } else if (cell == '#') {
                    boardCells[row][col].setBackground(pieceColors[8]);
                } else {
                    boardCells[row][col].setBackground(Color.WHITE);
                }
            }
        }
    }

    private void updateMiniPanel(int nextID, int holdID) {

        if (nextCells == null || holdCells == null) {
            return;
        }

        // Nulstil begge mini grids
        for (int r = 0; r < 4; r++) {
            for (int c = 0; c < 4; c++) {
                nextCells[r][c].setBackground(Color.WHITE);
                holdCells[r][c].setBackground(Color.WHITE);
            }
        }
        int nextIDColor = nextID - 1;
        int holdIDColor = holdID - 1;

        if (nextID >= 1 && nextID <= 7) {
            //longMask
            if (nextID == 1) {
                nextCells[1][0].setBackground(pieceColors[nextIDColor]);
                nextCells[1][1].setBackground(pieceColors[nextIDColor]);
                nextCells[1][2].setBackground(pieceColors[nextIDColor]);
                nextCells[1][3].setBackground(pieceColors[nextIDColor]);
            }

            //tMask
            else if (nextID == 2) {
                nextCells[1][1].setBackground(pieceColors[nextIDColor]);
                nextCells[2][0].setBackground(pieceColors[nextIDColor]);
                nextCells[2][1].setBackground(pieceColors[nextIDColor]);
                nextCells[2][2].setBackground(pieceColors[nextIDColor]);
            }

            //zigzag
            else if (nextID == 3) {
                nextCells[2][0].setBackground(pieceColors[nextIDColor]);
                nextCells[2][1].setBackground(pieceColors[nextIDColor]);
                nextCells[1][1].setBackground(pieceColors[nextIDColor]);
                nextCells[1][2].setBackground(pieceColors[nextIDColor]);
            }

            //zigzagR
            else if (nextID == 4) {
                nextCells[1][0].setBackground(pieceColors[nextIDColor]);
                nextCells[1][1].setBackground(pieceColors[nextIDColor]);
                nextCells[2][1].setBackground(pieceColors[nextIDColor]);
                nextCells[2][2].setBackground(pieceColors[nextIDColor]);
            }

            //lLeft
            else if (nextID == 5) {
                nextCells[1][0].setBackground(pieceColors[nextIDColor]);
                nextCells[2][0].setBackground(pieceColors[nextIDColor]);
                nextCells[2][1].setBackground(pieceColors[nextIDColor]);
                nextCells[2][2].setBackground(pieceColors[nextIDColor]);
            }

            //lRight
            else if (nextID == 6) {
                nextCells[1][2].setBackground(pieceColors[nextIDColor]);
                nextCells[2][0].setBackground(pieceColors[nextIDColor]);
                nextCells[2][1].setBackground(pieceColors[nextIDColor]);
                nextCells[2][2].setBackground(pieceColors[nextIDColor]);
            }

            //square
            else if (nextID == 7) {
                nextCells[1][1].setBackground(pieceColors[nextIDColor]);
                nextCells[1][2].setBackground(pieceColors[nextIDColor]);
                nextCells[2][1].setBackground(pieceColors[nextIDColor]);
                nextCells[2][2].setBackground(pieceColors[nextIDColor]);
            }
        }

        if (holdID >= 1 && holdID <= 7) {
            //longMask
            if (holdID == 1) {
                holdCells[1][0].setBackground(pieceColors[holdIDColor]);
                holdCells[1][1].setBackground(pieceColors[holdIDColor]);
                holdCells[1][2].setBackground(pieceColors[holdIDColor]);
                holdCells[1][3].setBackground(pieceColors[holdIDColor]);
            }
            //tMask
            else if (holdID == 2) {
                holdCells[1][1].setBackground(pieceColors[holdIDColor]);
                holdCells[2][0].setBackground(pieceColors[holdIDColor]);
                holdCells[2][1].setBackground(pieceColors[holdIDColor]);
                holdCells[2][2].setBackground(pieceColors[holdIDColor]);
            }
            //zigzagL
            else if (holdID == 3) {
                holdCells[2][0].setBackground(pieceColors[holdIDColor]);
                holdCells[2][1].setBackground(pieceColors[holdIDColor]);
                holdCells[1][1].setBackground(pieceColors[holdIDColor]);
                holdCells[1][2].setBackground(pieceColors[holdIDColor]);
            }
            //zigzagR
            else if (holdID == 4) {
                holdCells[1][0].setBackground(pieceColors[holdIDColor]);
                holdCells[1][1].setBackground(pieceColors[holdIDColor]);
                holdCells[2][1].setBackground(pieceColors[holdIDColor]);
                holdCells[2][2].setBackground(pieceColors[holdIDColor]);
            }
            //lLeft
            else if (holdID == 5) {
                holdCells[1][0].setBackground(pieceColors[holdIDColor]);
                holdCells[2][0].setBackground(pieceColors[holdIDColor]);
                holdCells[2][1].setBackground(pieceColors[holdIDColor]);
                holdCells[2][2].setBackground(pieceColors[holdIDColor]);
            }
            //lRight
            else if (holdID == 6) {
                holdCells[1][2].setBackground(pieceColors[holdIDColor]);
                holdCells[2][0].setBackground(pieceColors[holdIDColor]);
                holdCells[2][1].setBackground(pieceColors[holdIDColor]);
                holdCells[2][2].setBackground(pieceColors[holdIDColor]);
            }
            //square
            else if (holdID == 7) {
                holdCells[1][1].setBackground(pieceColors[holdIDColor]);
                holdCells[1][2].setBackground(pieceColors[holdIDColor]);
                holdCells[2][1].setBackground(pieceColors[holdIDColor]);
                holdCells[2][2].setBackground(pieceColors[holdIDColor]);
            }
        }
    }

    private void initializePieceColors() {
        pieceColors = new Color[9];
        pieceColors[0] = new Color(0, 240, 240);   // cyan
        pieceColors[1] = new Color(160, 0, 240);   // purple
        pieceColors[2] = new Color(0, 240, 0);     // green
        pieceColors[3] = new Color(240, 0, 0);     // red
        pieceColors[4] = new Color(0, 0, 240);     // blue
        pieceColors[5] = new Color(240, 160, 0);   // orange
        pieceColors[6] = new Color(240, 240, 0);   // yellow
        pieceColors[8] = new Color(211, 211, 211); // lightgrey for shadow
    }
}

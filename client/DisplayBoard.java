import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import javax.swing.*;

public class DisplayBoard extends JFrame {

    // Simple GUI components
    private JTextField highScoreField;
    private JTextField scoreField;
    private JTextField statusField;
    private JPanel boardPanel;
    private JLabel[][] boardCells; 
    private JPanel pieceCellPanel;
    private JLabel[][] pieceCells;
    private JPanel holdPiecePanel;
    private JPanel nextPiecePanel;
    private JTextArea leaderboardArea;
    
    // Network components
    private Socket sock;
    private Scanner netin;
    private PrintWriter netout;

    // Declare variables for storing colors of the tetris-pieces.
    private Color[] pieceColors;
    private int currentPieceType;

    // Constructor. Sets layout
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

        setupTopPanel();
        setupMainPanel();
        setupBottomPanel();
        
        // Window properties
        setSize(700, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        pack(); 
        
        setupKeyBindings();
    }

    private void setupTopPanel() {
        JPanel topPanel = new JPanel();
        highScoreField = new JTextField("Highscore: ", 15);
        highScoreField.setEditable(false);
        topPanel.add(highScoreField);
        
        scoreField = new JTextField("Score: ", 15);
        scoreField.setEditable(false);
        topPanel.add(scoreField);
        
        add(topPanel, BorderLayout.NORTH);
    }
    
    private void setupMainPanel() {
        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        
        // Setup left side (Next and Hold)
        mainPanel.add(createLeftPanel(), BorderLayout.WEST);
        
        // Setup center (Board)
        mainPanel.add(setupBoard(), BorderLayout.CENTER);
        
        // Setup right side (Leaderboard)
        mainPanel.add(createRightPanel(), BorderLayout.EAST);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private JPanel createLeftPanel() {
        
        //Initialize panel for Hold-piece and Next-piece
        JPanel piecesPanel = new JPanel();
        piecesPanel.setLayout(new BoxLayout(piecesPanel, BoxLayout.Y_AXIS));  
        piecesPanel.setPreferredSize(new Dimension(300, 400));
        
        // Create next piece panel  
        nextPiecePanel = createMiniBoard();
        JLabel nextPieceTitle = new JLabel("NEXT PIECE");
        nextPiecePanel.add(nextPieceTitle, BorderLayout.NORTH);
        piecesPanel.add(nextPiecePanel);

        //Add space between the two panels
        piecesPanel.add(Box.createVerticalStrut(100)); 
        
        // Create hold piece panel
        holdPiecePanel = createMiniBoard();
        JLabel holdPieceTitle = new JLabel("HOLD PIECE");
        holdPiecePanel.add(holdPieceTitle, BorderLayout.NORTH);
        piecesPanel.add(holdPiecePanel);
        
        return piecesPanel;
    } 

    private JPanel createMiniBoard() {
        //Create board panel with cells
        JPanel container = new JPanel(new BorderLayout());

        pieceCellPanel = new JPanel(new GridLayout(4, 2, 1, 1));
        pieceCellPanel.setPreferredSize(new Dimension(80, 160));

        //Create the cells
        pieceCells = new JLabel[4][2];
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 2; col++) {
                pieceCells[row][col] = new JLabel();
                pieceCells[row][col].setOpaque(true);
                pieceCells[row][col].setBackground(Color.WHITE);
                pieceCells[row][col].setPreferredSize(new Dimension(40, 40));
                pieceCellPanel.add(pieceCells[row][col]);
            }
        }

        container.add(pieceCellPanel, BorderLayout.CENTER);
        return container;
    }
 
    private JPanel setupBoard() {
        //Create board panel with cells
        boardPanel = new JPanel(new GridLayout(20, 10, 1, 1));
        boardPanel.setPreferredSize(new Dimension(300, 400));
        
        //Create the 20x10 cells
        boardCells = new JLabel[20][10];
        for (int row = 0; row < 20; row++) {
            for (int col = 0; col < 10; col++) {
                boardCells[row][col] = new JLabel();
                boardCells[row][col].setOpaque(true);
                boardCells[row][col].setBackground(Color.WHITE);
                boardPanel.add(boardCells[row][col]);
            }
        }
        return boardPanel;
    }

    private JPanel createRightPanel() {
        //Create leaderboard panel
        JPanel leaderboardPanel = new JPanel();
        leaderboardPanel.setLayout(new BorderLayout());
        
        // Add title to leaderboard
        JLabel leaderboardTitle = new JLabel("LEADERBOARD");
        leaderboardPanel.add(leaderboardTitle, BorderLayout.NORTH);
        
        // Create the leaderboard area
        leaderboardArea = new JTextArea(12, 20);
        leaderboardArea.setText("Waiting for leaderboard");
        leaderboardArea.setEditable(false);
        JScrollPane leaderboardScroll = new JScrollPane(leaderboardArea);
        
        leaderboardScroll.setPreferredSize(new Dimension(180, 340));
        leaderboardPanel.add(leaderboardScroll, BorderLayout.CENTER);
        
        return leaderboardPanel;
    }

    private void setupBottomPanel() {
        //Create status panel
        JPanel bottomPanel = new JPanel();
        statusField = new JTextField("Game Status: Active", 15);
        statusField.setEditable(false);
        bottomPanel.add(statusField);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    //Method to setup the keys to enable controls.
    private void setupKeyBindings() {
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();
                if (keyCode == KeyEvent.VK_LEFT) {
                    sendCommand("LEFT");
                } else if (keyCode == KeyEvent.VK_RIGHT) {
                    sendCommand("RIGHT");
                } else if (keyCode == KeyEvent.VK_UP) {
                    sendCommand("ROTATE");
                } else if (keyCode == KeyEvent.VK_DOWN) {
                    sendCommand("SOFT");
                } else if (keyCode == KeyEvent.VK_SPACE) {
                    sendCommand("HARD");
                } else if (keyCode == KeyEvent.VK_SHIFT) {
                    sendCommand("HOLD");
                }
            }           
        });
        
        // Make sure the frame can receive key events
        setFocusable(true);
        requestFocusInWindow();
    }

    //Starts the GUI and connects to the server.
    public void sendCommand(String command) {
        try {
            if (netout != null) {
                System.out.println("Sending move: "+ command);
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
                
                //Send user ID. If it exists, send it to server. If not, create it and send it.

                //Show the welcome-window.
                showWelcome(); 

            } catch (Exception e){
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
        welcomeWindow.setSize(400, 150);
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

        // Add the different fields to the window.
        JPanel panel = new JPanel();
        panel.add(name);
        panel.add(nameField);
        panel.add(startButton);
        welcomeWindow.add(title, BorderLayout.NORTH);
        welcomeWindow.add(panel, BorderLayout.CENTER);

        welcomeWindow.setVisible(true); 
    }

    private void OpenGameOverWindow() {
        JDialog gameOverWindow = new JDialog(this, "GAME OVER", true);
        gameOverWindow.setLayout(new GridLayout(4, 1, 10, 10));
        gameOverWindow.setSize(300, 200);
        gameOverWindow.setLocationRelativeTo(this);

        JLabel title = new JLabel("GAME OVER", SwingConstants.CENTER);
        JLabel scoreLabel = new JLabel(scoreField.getText(), SwingConstants.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton playAgain = new JButton("Play Again");
        JButton exit = new JButton("Exit");
        buttonPanel.add(playAgain);
        buttonPanel.add(exit);

        playAgain.addActionListener(e -> {
            restartGame();
            gameOverWindow.dispose();
        });
        
        exit.addActionListener(e -> {
            gameOverWindow.dispose();
            System.exit(0);
        });

        gameOverWindow.add(title);
        gameOverWindow.add(scoreLabel);
        gameOverWindow.add(new JLabel()); 
        gameOverWindow.add(buttonPanel);

        gameOverWindow.setVisible(true);
    }

    private void restartGame() {
        scoreField.setText(" ");
        
        // Clear the board
        for (int row = 0; row < 20; row++) {
            for (int col = 0; col < 10; col++) {
                boardCells[row][col].setBackground(Color.WHITE);
            }
        }

        sendCommand("RESTART");

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
        serverMessage = serverMessage.trim(); // FIX: så " PIECE..." også matches som "PIECE..."
        System.out.println("DEBUG: Processing message: " + serverMessage);
        
        // To set the Board in the GUI.
        if (serverMessage.startsWith("BOARD")) {
            System.out.println("DEBUG: Board message detected");
            updateBoard(serverMessage);
        // To set the score
        } else if (serverMessage.startsWith("PIECE")) {
            System.err.println("Piece detected: " + serverMessage);
            String pieceString = serverMessage.substring(serverMessage.lastIndexOf(" ") + 1);
            currentPieceType = Integer.parseInt(pieceString) -1;
            System.out.println("Current piece set to: " + currentPieceType);
        } else if (serverMessage.startsWith("SCORE")) {
            System.out.println("DEBUG: Score message detected: " + serverMessage);
            scoreField.setText(serverMessage);
        //Set the high score
        } else if (serverMessage.startsWith("HIGHSCORE")) {
            highScoreField.setText(serverMessage);
        //To set the status-field. 
        } else if (serverMessage.startsWith("GAMEOVER")) {
            System.out.println("DEBUG: Game over message detected"); 
            OpenGameOverWindow();
        // Set leaderboard 
        } else if (serverMessage.startsWith("LEADERBOARD")) {
            serverMessage = serverMessage.substring(12);
            serverMessage = serverMessage.replaceAll(";", "\n");
            leaderboardArea.setText(serverMessage);
            leaderboardArea.setCaretPosition(0); //Scroll to the top
        } else if(serverMessage.startsWith("LEVEL")) {
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
                } else {
                    boardCells[row][col].setBackground(Color.WHITE);
                }
            }
        }
    }

    private void initializePieceColors() {
        pieceColors = new Color[7];

        pieceColors[0] = new Color(0, 240, 240);
        pieceColors[1] = Color.MAGENTA;
        pieceColors[2] = Color.GREEN;
        pieceColors[3] = Color.RED; 
        pieceColors[4] = Color.BLUE;
        pieceColors[5] = Color.ORANGE;       
        pieceColors[6] = Color.YELLOW;           
    }
}

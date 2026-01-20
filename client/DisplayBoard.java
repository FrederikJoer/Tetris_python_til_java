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
    private JTextArea leaderboardArea;
    
    // Network components
    private Socket sock;
    private Scanner netin;
    private PrintWriter netout;

    // Declare variables for storing colors of the tetris-pieces.
    private Color[] pieceColors;
    private int currentPieceType;

    //Constructor. Sets layout
    public DisplayBoard(Socket sock, Scanner netin, PrintWriter netout) {
        this.sock = sock;
        this.netin = netin;
        this.netout = netout;

        setupGUI();
    }
    
        private void setupGUI() {
        //Create the window as a border-layout.
        setTitle("Tetris");
        setLayout(new BorderLayout());

        // Create top panel with both score fields
        JPanel topPanel = new JPanel();
        highScoreField = new JTextField("High Score: ", 15);
        highScoreField.setEditable(false);
        topPanel.add(highScoreField);
        
        scoreField = new JTextField("Score: ", 15);
        scoreField.setEditable(false);
        topPanel.add(scoreField);
        
        add(topPanel, BorderLayout.NORTH);

        // Create main panel for board and highscore area
        JPanel mainPanel = new JPanel(new BorderLayout(10, 0)); // 10px horizontal gap
        
        //Create board panel with cells
        boardPanel = new JPanel(new GridLayout(20, 10, 1, 1));
        boardPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        boardCells = new JLabel[20][10];

        //Create the 20 cells and add them to the board panel.
        for (int row=0; row<20;row++) {
            for (int col=0; col<10; col++) {
                boardCells[row][col] = new JLabel();
                boardCells[row][col].setOpaque(true);
                boardCells[row][col].setBackground(Color.WHITE);
                boardPanel.add(boardCells[row][col]);
            }
        }
        
        //Create leaderboard - wider but shorter
        leaderboardArea = new JTextArea(12, 20); // 12 rows, 20 columns wide
        leaderboardArea.setText("Waiting for leaderboard...");
        leaderboardArea.setEditable(false);
        JScrollPane leaderboardScroll = new JScrollPane(leaderboardArea);
        leaderboardScroll.setPreferredSize(new Dimension(200, 300)); 
        
        // Add board to center, leaderboard to east
        mainPanel.add(boardPanel, BorderLayout.CENTER);
        mainPanel.add(leaderboardScroll, BorderLayout.EAST);
        
        add(mainPanel, BorderLayout.CENTER);

        //Create status panel
        JPanel bottomPanel = new JPanel();
        statusField = new JTextField("Game Status: Active", 15);
        statusField.setEditable(false);
        bottomPanel.add(statusField);
        add(bottomPanel, BorderLayout.SOUTH);

        // Set size etc.
        setSize(500, 600); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        //Add key bindings
        setupKeyBindings();
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
        System.out.println("DEBUG: Processing message: " + serverMessage);
        
        // To set the Board in the GUI.
        if (serverMessage.startsWith("BOARD")) {
            System.out.println("DEBUG: Board message detected");
            updateBoard(serverMessage);
        // To set the score
        } else if (serverMessage.startsWith("PIECE")) {
            System.err.println("Piece detected: " + serverMessage);
            String pieceString = serverMessage.substring(6); //Assuming "PIECE X"
            currentPieceType = Integer.parseInt(pieceString);
            System.out.println("Current piece set to: " + currentPieceType);
        } else if (serverMessage.startsWith("SCORE")) {
            System.out.println("DEBUG: Score message detected: " + serverMessage);
            scoreField.setText(serverMessage);
        //Set the high score
        } else if (serverMessage.startsWith("HIGHSCORE")) {
            highScoreField.setText(serverMessage);
        //To set the status-field. 
        } else if (serverMessage.startsWith("GAME OVER")) {
            System.out.println("DEBUG: Game over message detected"); 
            statusField.setText("GAME OVER: " + serverMessage.substring(10));
            scoreField.setBackground(Color.GREEN);
        // Set leaderboard 
        } else if (serverMessage.startsWith("LEADERBOARD")) {
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

        //Iterates over every 'block' in the GUI, and sets the color based on the board-string from the server.
        for (int i = 0; i < 200; i++) {
            int row = i / 10;  // Gives the row
            int col = (i % 10);  // Gives the column
           
            if (row < 20 && col < 10) {
                char cell = board.charAt(i);
            
                if (cell == 'X') {
                    try {
                        boardCells[row][col].setBackground(pieceColors[currentPieceType]);
                    } catch (Exception e) {
                        boardCells[row][col].setBackground(Color.RED);
                    }
                } else if (cell =='#'){
                    boardCells[row][col].setBackground(Color.BLACK);
                } else {
                    boardCells[row][col].setBackground(Color.WHITE);
                }
            }
        }
    }

     private void initializePieceColors() {
        pieceColors = new Color[7];
        pieceColors[0] = Color.CYAN;      
        pieceColors[1] = Color.BLUE;      
        pieceColors[2] = Color.ORANGE;    
        pieceColors[3] = Color.YELLOW;    
        pieceColors[4] = Color.GREEN;    
        pieceColors[5] = Color.MAGENTA;   
        pieceColors[6] = Color.RED;       
    }
}
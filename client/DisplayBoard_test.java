import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import javax.swing.*;

public class DisplayBoard_test extends JFrame {

    // Simple GUI components
    private JTextArea boardArea;
    private JTextField highScoreField;
    private JTextField statusField;
    private JPanel boardPanel;
    private JLabel[][] boardCells;
    
    // Network components
    private Socket sock;
    private Scanner netin;
    private PrintWriter netout;

    //Constructor. Sets layout
    public DisplayBoard_test(Socket sock, Scanner netin, PrintWriter netout) {
        this.sock = sock;
        this.netout = netout;
        this.netin = netin;

        setupGUI();
    }

    private void setupGUI() {

        //Create the window as a border-layout.
        setTitle("Tetris");
        setLayout(new BorderLayout());

        //Create the high-score field
        JPanel topPanel = new JPanel();
        highScoreField = new JTextField("High Score: ", 20);
        highScoreField.setEditable(false);
        topPanel.add(highScoreField, BorderLayout.NORTH);
        add(topPanel, BorderLayout.NORTH);

        //Create board panel with cells
        boardPanel = new JPanel(new GridLayout(20,10));
        boardCells = new JLabel[20][10];

        //Create the 20 cells and add them to the board panel.
        for (int row=0; row<20;row++) {
            for (int col=0; col<20; row++) {
                boardCells[row][col] = new JLabel(" ");
                boardCells[row][col].setHorizontalAlignment(SwingConstants.CENTER);
                boardCells[row][col].setBackground(Color.BLACK);
                boardCells[row][col].setForeground(Color.WHITE);
                boardCells[row][col].setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
                boardPanel.add(boardCells[row][col]);
            }
        }

        add(boardPanel, BorderLayout.CENTER);

        //Create status panel. Shows winner etc.
        JPanel bottomPanel = new JPanel();
        statusField = new JTextField("Status: ", 30);
        statusField.setEditable(false);
        bottomPanel.add(statusField, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        // Set size etc.
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

    }

    //Starts the GUI and connects to the server.
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
            SwingUtilities.invokeLater(() -> showWelcome()); 

            } catch (Exception e){
                System.err.println("Error in server-connection: " + e.getMessage());
                SwingUtilities.invokeLater(() -> 
                    statusField.setText("Connection error: " + e.getMessage()));
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
        title.setFont(new Font("Arial", Font.BOLD, 16));
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

                //Listen to answers from server continuously
                while (true) {
                    if (netin.hasNextLine()) {
                        String serverMessage = netin.nextLine();
                        System.out.println("DEBUG: Received message: " + serverMessage);
                        processMessage(serverMessage);
                    } else {
                        System.out.println("DEBUG: Server disconnected");
                        SwingUtilities.invokeLater(() -> 
                            statusField.setText("Server disconnected"));
                        break;
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
        } else if (serverMessage.startsWith("SCORE")) {
            System.out.println("DEBUG: Score message detected: " + serverMessage);
            SwingUtilities.invokeLater(() -> 
                highScoreField.setText("SCORE: " + serverMessage.substring(5).trim()));
        //To set the status-field. 
        } else if (serverMessage.startsWith("GAME OVER")) {
            System.out.println("DEBUG: Game over message detected");
            SwingUtilities.invokeLater(() -> 
                statusField.setText(serverMessage));
        } else {
            System.out.println("DEBUG: Unknown message format");
            SwingUtilities.invokeLater(() -> 
                boardArea.setText("ERROR: Unknown message\n" + serverMessage));
        }
    }

    private void updateBoard(String serverMessage) {
        String board = serverMessage.substring(8);

        if (board.length() >= 200) {
            for (int row = 0; row < 20; row++) {
                for (int col = 0; col < 10; col++) {
                    int index = row * 10 + col;
                    char cell = board.charAt(index);
                    if (cell == 'x') {
                        boardCells[row][col].setText("■");
                    } else {
                        boardCells[row][col].setText(" ");
                    }
                }
            }
        }
}
}
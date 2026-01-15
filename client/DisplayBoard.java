import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import javax.swing.*;

public class DisplayBoard extends JFrame implements ActionListener {

    //Declare GUI components and the printwriter for sending move to server.
    public JButton[] buttons;
    public JTextField textfield1;
    public JTextField textfield2;
    public JTextField board_display;
    public PrintWriter netout;

    //Constructor. Sets layout
    public DisplayBoard(String textline1, String textline2, String board) {


            
        

        // Create textboxes to show game status
        textfield1 = new JTextField();
        textfield2 = new JTextField();
        board_display = new JTextField();

        
    }

    // Event handler method. Called when a button is clicked
    public void actionPerformed(ActionEvent e) {
        String input;

        // Find out which button was pressed, and send the number (1-9) for that button to the server.
        for (int i = 0; i<9; i++) {
            if (e.getSource() == buttons[i]) {
                this.netout.print(String.valueOf(i+1) + "\r\n");
                this.netout.flush();
            }
        }

    }
    
    //Main method. Create window.
    public static void main (String[] args) {

        //Create and open window
        TicTacToeGUI window = new TicTacToeGUI();
        window.setTitle("Tic Tac Toe");
        window.setSize(600, 600);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);

        //Declaration of sockets, writer/reader and gameactive boolean.
        Socket sock = null;
        Scanner netin = null;
        Boolean gameactive = null;
        Scanner scanner = null;

        //Setup connection to server
        try {
            scanner = new Scanner(System.in);
            sock = new Socket("34302.cyberteknologi.dk", 1060);
            netin = new Scanner(sock.getInputStream());
            window.netout = new PrintWriter(sock.getOutputStream());
                
        } catch (Exception e) {
            System.err.println("Error in server-connection: " + e.getMessage());
            System.exit(0);
        }

        try {
            //Read the first line. If its starts with welcome, set that line to be visible on the GUI, and set game to be active.
            String textline = netin.nextLine();
            if (textline.startsWith("WELCOME")) {
                window.textfield1.setText(textline);
                gameactive = true;
            } 

        //Loop that runs whenever the game is still active
            while (gameactive=true) {
                String textline2 = netin.nextLine(); //Reads second line
                String textline3 = netin.nextLine(); //Reads third line

                //If the second line does NOT say ILLEGAL MOVE, it contains the board.
                if (!textline2.startsWith("ILLEGAL")) {
                    window.textfield2.setText(textline3);
                    // Set the board
                    for (int i=9; i<18; i++) {
                        char boardChar = textline2.charAt(i);
                        if (boardChar != '.') {
                            window.buttons[i-9].setText(String.valueOf(boardChar));
                        }
                    }
                    // If there is a winner, set the game to inactive.
                    if (textline3.endsWith("WINS")) {
                    gameactive = false;
                    break;
                }

                } else {
                    window.textfield2.setText(textline2); // Set the textbox to "ILLEGAL MOVE".
                }
            }

            sock.close(); //Closes the connection
            
        } catch (Exception e) {
            System.out.println("Error" + e.getMessage());
            System.exit(0);
        }
    }
}
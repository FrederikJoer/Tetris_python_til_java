// ClientMain.java - Main class that starts everything
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientMain {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        try {
            System.out.println("Starting Tetris Client...");
            System.out.println("Connecting to server at localhost:1500");
            
            // Connect to server
            Socket sock = new Socket("localhost", 1500);
            PrintWriter netout = new PrintWriter(sock.getOutputStream(), true);
            Scanner netin = new Scanner(sock.getInputStream());
            
            // Create the GUI window
            DisplayBoard gui = new DisplayBoard(sock, netin, netout);
            
            // Start the GUI
            gui.start();
            
        } catch (Exception e) {
            System.err.println("Error when launching client: " + e.getMessage());
            System.exit(1);
        }
    }
}


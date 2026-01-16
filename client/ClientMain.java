// ClientMain.java - Main class that starts everything
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientMain {
    public static void main(String[] args) {

        //Declare client resources
        Socket sock = null;
        Scanner netin = null;
        PrintWriter netout = null;
        Scanner scanner = null;

        try {
            // Setup connection and initialize input/output streams
            sock = new Socket("10.209.227.247", 1500);
            scanner = new Scanner(System.in);
            netin = new Scanner(sock.getInputStream());
            netout = new PrintWriter(sock.getOutputStream());  
      
            //Recieve first lines from server
            String textline = netin.nextLine();
            String textline2 = netin.nextLine();
            System.out.println(textline);
            System.out.println(textline2);
          
            // Client enters name, and it gets sent to the server
            String text = scanner.nextLine();
            netout.print(text+ "\r\n");
            netout.flush();

            // Read the next line from the server.
            textline = netin.nextLine();
            System.out.println(textline);

            //Client sends S if player is ready to start.
            String text2 = scanner.nextLine();
            netout.print(text2+ "\r\n");
            netout.flush();


            // Read the next line from the server.
            textline = netin.nextLine();
            System.out.println(textline);

            //Listen to server, 
            while (textline.contains("ERROR")) {

                //Read error from server
                textline = netin.nextLine();
                System.out.println(textline);

                text2 = scanner.nextLine();
                netout.print(text2+ "\r\n");
                netout.flush();

                //Read answer from server
                textline = netin.nextLine();
                System.out.println(textline);
            }

            int moveCount = 0;
            while (moveCount < 10) {
                // Display move number
                System.out.println("Move " + (moveCount + 1) + " of 10");
                System.out.print("Enter your move: ");
                
                // Get user input
                String move = scanner.nextLine();
                
                // Send move to server
                netout.print(move + "\r\n");
                netout.flush();
                
                // Read server response (could be updated board, score, etc.)
                if (netin.hasNextLine()) {
                    textline = netin.nextLine();
                    System.out.println("Server response: " + textline);
                }
                moveCount++;
            }
            
        } catch (Exception e) {
            System.err.println("Error when launching client: " + e.getMessage());
            System.exit(1);
        }
    }
}
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class SimpleClientTest {

    private Socket sock;
    private Scanner netin;
    private PrintWriter netout;

    private boolean nameSent = false;
    private boolean startSent = false;

    public void connect() {
        try {
            sock = new Socket("localhost", 1500);
            netin = new Scanner(sock.getInputStream());
            netout = new PrintWriter(sock.getOutputStream()); // ingen autoflush

            System.out.println("Forbundet til server");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void listen() {
        while (true) {
            try {
                String line = netin.nextLine();
                System.out.println("SERVER: " + line);

                // Ikke else-if: hvis server en dag sender flere ting i én linje,
                // kan vi stadig reagere på alt.
                if (line.contains("CHOOSE A NAME") && !nameSent) {
                    send("test");
                    nameSent = true;
                    System.out.println("CLIENT -> test");
                }

                if (line.contains("WRITE START TO START") && !startSent) {
                    send("START");
                    startSent = true;
                    System.out.println("CLIENT -> START");
                }

                if (line.startsWith("BOARD IS:")) {
                    printBoard(line);
                }

            } catch (Exception e) {
                System.out.println("Forbindelse lukket: " + e.getClass().getName() + " - " + e.getMessage());
                // e.printStackTrace(); // slå til hvis du vil se hele stacktrace
                break;
            }
        }
    }

    private void send(String msg) {
        netout.println(msg);
        netout.flush();
    }

    private void printBoard(String msg) {
        String boardData = msg.substring(9).trim(); // efter "BOARD IS:"

        int WIDTH = 10;
        int HEIGHT = 20;

        if (boardData.length() != WIDTH * HEIGHT) {
            System.out.println("Ugyldig board-længde: " + boardData.length());
            return;
        }

        System.out.println("=== BOARD ===");
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                int index = y * WIDTH + x;
                System.out.print(boardData.charAt(index));
            }
            System.out.println();
        }
        System.out.println("=============");
    }

    public static void main(String[] args) {
        SimpleClientTest client = new SimpleClientTest();
        client.connect();
        client.listen();
    }
}

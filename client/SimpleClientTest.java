import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;

public class SimpleClientTest {

    private Socket sock;
    private Scanner netin;
    private PrintWriter netout;

    private boolean nameSent = false;
    private boolean startSent = false;
    private boolean gameStarted = false;

    private int moveCount = 0;        // hvor mange bevægelser tilbage
    private String currentMove = "";  // LEFT / RIGHT / ROTATE

    private Random random = new Random();

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

                // 1) Navn
                if (line.contains("CHOOSE A NAME") && !nameSent) {
                    send("test");
                    nameSent = true;
                    System.out.println("CLIENT -> test");
                }

                // 2) Start
                if (line.contains("WRITE START TO START") && !startSent) {
                    send("START");
                    startSent = true;
                    System.out.println("CLIENT -> START");
                }

                // 3) Board = spillet kører
                if (line.startsWith("BOARD IS:")) {
                    gameStarted = true;
                    printBoard(line);

                    maybeSendRandomMove();
                }

            } catch (Exception e) {
                System.out.println("Forbindelse lukket: "
                        + e.getClass().getName() + " - " + e.getMessage());
                break;
            }
        }
    }

    /**
     * Sender LEFT / RIGHT / ROTATE i serier á 1–5 gange
     * Kaldes kun når spillet er startet
     */
    private void maybeSendRandomMove() {
        if (!gameStarted) return;

        // Hvis vi ikke er i gang med en serie → start en ny
        if (moveCount == 0) {
            moveCount = random.nextInt(5) + 1; // 1–5

            int choice = random.nextInt(10); // 0..9
            // 40% LEFT, 40% RIGHT, 20% ROTATE
            if (choice < 4) {
                currentMove = "LEFT";
            } else if (choice < 8) {
                currentMove = "RIGHT";
            } else {
                currentMove = "ROTATE";
            }

            System.out.println("CLIENT planlægger: "
                    + moveCount + "x " + currentMove);
        }

        // Send én bevægelse
        send(currentMove);
        System.out.println("CLIENT -> " + currentMove);

        moveCount--;

        // Lille pause så serveren kan følge med
        try {
            Thread.sleep(150);
        } catch (InterruptedException ignored) {}
    }

    private void send(String msg) {
        netout.println(msg);
        netout.flush();
    }

    private void printBoard(String msg) {
        String boardData = msg.substring(9).trim();

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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class userLog {
    public static void main(String name, int score) {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter("playersLog.txt", true));

            writer.println("Player ID: " + name + ". Score: " + score);

            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
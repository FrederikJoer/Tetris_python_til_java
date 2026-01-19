import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

public class userLog {

    public static void main(String name, int score) {

        boolean found = false;

        try {
            Scanner sc = new Scanner(new File("playersLog.txt"));
            PrintWriter out = new PrintWriter(new FileWriter("temp.txt"));

            while (sc.hasNextLine()) {
                String line = sc.nextLine();

                // Matcher præcist på Player ID
                if (line.startsWith("Player ID: " + name + ". Score: ")) {
                    found = true;

                    // Udtræk gammel score
                    int oldScore = Integer.parseInt(
                        line.substring(line.lastIndexOf(" ") + 1)
                    );

                    // Overwrite kun hvis ny score er højere
                    if (score > oldScore) {
                        out.println("Player ID: " + name + ". Score: " + score);
                    } else {
                        out.println(line);
                    }
                } else {
                    out.println(line);
                }
            }

            sc.close();

            // Hvis spilleren ikke fandtes → tilføj ny linje
            if (!found) {
                out.println("Player ID: " + name + ". Score: " + score);
            }

            out.close();

            // Erstat gammel fil med ny
            File oldFile = new File("playersLog.txt");
            File newFile = new File("temp.txt");

            oldFile.delete();
            newFile.renameTo(oldFile);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


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

            userLog.top10ToFile();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void top10ToFile() {

        String[] topNames = new String[10];
        int[] topScores = new int[10];

        // init med placeholders
        for (int i = 0; i < 10; i++) {
            topNames[i] = ".............................";
            topScores[i] = -1;
        }

        try {
            // Læs playersLog.txt
            Scanner sc = new Scanner(new File("playersLog.txt"));

            while (sc.hasNextLine()) {
                String line = sc.nextLine();

                // Forventer: Player ID: <name>. Score: <score>
                if (line.startsWith("Player ID: ") && line.contains(". Score: ")) {

                    int nameStart = "Player ID: ".length();
                    int nameEnd = line.indexOf(". Score: ");
                    String name = line.substring(nameStart, nameEnd);

                    int score = Integer.parseInt(
                        line.substring(line.lastIndexOf(" ") + 1)
                    );

                    // indsæt i top 10 hvis score er høj nok
                    for (int i = 0; i < 10; i++) {
                        if (score > topScores[i]) {

                            // skub resten ned
                            for (int j = 9; j > i; j--) {
                                topScores[j] = topScores[j - 1];
                                topNames[j] = topNames[j - 1];
                            }

                            topScores[i] = score;
                            topNames[i] = name;
                            break;
                        }
                    }
                }
            }

            sc.close();

            // Skriv top10.txt
            PrintWriter out = new PrintWriter(new FileWriter("top10.txt", false));

            for (int i = 0; i < 10; i++) {
                if (topScores[i] == -1) {
                    out.println((i + 1) + ". .............................");
                } else {
                    out.println((i + 1) + ". " + topNames[i] + " - " + topScores[i]);
                }
            }

            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String fetchHighScore(String playername) {
        String highScore = "0";

        try {
            Scanner sc = new Scanner(new File("playersLog.txt")); // FIX: filnavn matcher resten af koden

            while (sc.hasNextLine()) {
                String line = sc.nextLine();

                if (line.startsWith("Player ID: " + playername + ". Score: ")) {
                    highScore = line.substring(line.lastIndexOf(" ") + 1);
                    break;
                }
            }

            sc.close();
        } catch (Exception e) {
        }

        System.out.println(highScore);
        return highScore;
    }




}
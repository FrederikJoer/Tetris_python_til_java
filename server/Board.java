public class Board {
    private String[] board = new String[200];

    public String[] makeBoard() {
        for (int i = 0; i < 200; i++) {
            board[i] = ".";
        }
        return board;
    }

    public boolean collision(String[] board, int gx, int gy) { // tjekker om en position kolliderer
        // FIX: bounds + optaget felt samlet her (flyttet fra GameSession)
        if (gx < 0 || gx >= 10) return true;
        if (gy < 0 || gy >= 20) return true;

        int index = gy * 10 + gx; // 10 = WIDTH (bredde)
        if (!board[index].equals(".")) return true;

        return false;
    }

    public boolean isEmpty(int x, int y) { // tjekker om en plads er ledig eller låst
        return false;
    }

    public char[][] copyLocked() { // Kopiere board efter en kolision som kan bruges til næste activePiece
        return null;
    }

    public void clear() { // bruges til at nulstille board til restart game
    }
}

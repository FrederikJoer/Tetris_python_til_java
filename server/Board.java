public class Board {

    private String[] board = new String[200];

    public String[] makeBoard() {
        for (int i = 0; i < 200; i++) {
            board[i] = ".";
        }
        return board;
    }

    public boolean collisionBottom(String[] board, int gx, int gy) {
        if (gy >= 20) {
            return true;
        }
        // ÆNDRET: tjek for 1..7 i stedet for "#"
        if (!board[gy * 10 + gx].equals(".") && !board[gy * 10 + gx].equals("X")) {
            return true;
        }
        return false;
    }

    public boolean collisionWall(String[] board, int gx, int gy) {
        if (gx < 0 || gx >= 10) {
            return true;
        }
        // ÆNDRET: tjek for 1..7 i stedet for "#"
        if (!board[gy * 10 + gx].equals(".") && !board[gy * 10 + gx].equals("X")) {
            return true;
        }
        else return false;
    }

    public String[] lockBoard(String[] board, int activePieceID) {
        String[] lockedBoard = board.clone();
        for (int i = 0; i < board.length; i++) {
            if (board[i].equals("X")) {
                lockedBoard[i] = String.valueOf(activePieceID);
            }
        }
        return lockedBoard;
    }

    public boolean checkGameOver(String[] board) {
        for (int x = 0; x < 10; x++) {
            // ÆNDRET: tjek for 1..7 i stedet for "#"
            if (!board[x].equals(".") && !board[x].equals("X")) {
                return true;
            }
        }
        return false;
    }

    public void clear() { //Skal bruges til at nulstille board til restart game
    }
}

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
        if (!board[gy * 10 + gx].equals(".") && !board[gy * 10 + gx].equals("X") && !board[gy * 10 + gx].equals("#")) {
            return true;
        }
        return false;
    }

    public boolean collisionWall(String[] board, int gx, int gy) {
        if (gx < 0 || gx >= 10) {
            return true;
        }
        if (!board[gy * 10 + gx].equals(".") && !board[gy * 10 + gx].equals("X") && !board[gy * 10 + gx].equals("#")) {
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
            if (!board[x].equals(".") && !board[x].equals("X") && !board[x].equals("#")) {
                return true;
            }
        }
        return false;
    }

}

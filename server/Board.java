public class Board {

    private String[] board = new String[200];

    public String[] makeBoard() {
        for (int i = 0; i < 200; i++) {
            board[i] = ".";
        }
        return board;
    }

    public boolean collisionBottom(String[] board, int gx, int gy) {
        if (gy < 0 || gy >= 20) {
            return true;
        }
        if (board[gy * 10 + gx].equals("#")) {
            return true;
        }
        return false;
    }

    public boolean collisionWall(String[] board, int gx, int gy) {
        if (gx < 0 ||gx >= 10) {
            return true;
        } else return false;
    }


    public String[] lockBoard(String[] board) {
        String[] lockedBoard = board.clone();
        for (int i = 0; i < board.length; i++) {
            if (board[i].equals("X")){
                lockedBoard[i] = "#";
            }
        }
        return lockedBoard;
    }

    public void clear() { //Skal bruges til at nulstille board til restart game
    }
}

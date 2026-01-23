public class Board {

    private String[] board = new String[200];

    public String[] makeBoard() {
        for (int i = 0; i < 200; i++) {
            board[i] = ".";
        }
        return board;
    }

    //Tjekker collision i y retningen både for indexfejl og om der ern brik
    public boolean collisionBottom(String[] board, int gx, int gy) {
        if (gy >= 20) {
            return true;
        }
        if (!board[gy * 10 + gx].equals(".") && !board[gy * 10 + gx].equals("X") && !board[gy * 10 + gx].equals("#")) {
            return true;
        }
        return false;
    }

    //Tjekekr colision ved væggen ved at tjekke index fejl og om der en brik
    public boolean collisionWall(String[] board, int gx, int gy) {
        if (gx < 0 || gx >= 10) {
            return true;
        }
        if (!board[gy * 10 + gx].equals(".") && !board[gy * 10 + gx].equals("X") && !board[gy * 10 + gx].equals("#")) {
            return true;
        }
        else return false;
    }

    //Locker boardet og gemmer det i en variabel
    public String[] lockBoard(String[] board, int activePieceID) {
        String[] lockedBoard = board.clone();
        for (int i = 0; i < board.length; i++) {
            if (board[i].equals("X")) {
                lockedBoard[i] = String.valueOf(activePieceID);
            }
        }
        return lockedBoard;
    }

    //Tjekekr for gameover. Tjekker om index blver mindre end x
    public boolean checkGameOver(String[] board) {
        for (int x = 0; x < 10; x++) {
            if (!board[x].equals(".") && !board[x].equals("X") && !board[x].equals("#")) {
                return true;
            }
        }
        return false;
    }

}

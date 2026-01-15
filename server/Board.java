public class Board{
    private String[] board = new String[200];

    public String[] makeBoard() {
        for (int i = 0; i < 200; i++) {
            board[i] = ".";
        }
    
        return board;
    }


    public String[] updateBoardMovement(String board, String input) {
        return null;
    }

    public String[] updateBoardGravity(String board) {
        return null;
    }


    public boolean collisionWall(String board) {
        return false;
    }

    public boolean collisionPiece(String board) {
        return false;
    }
}
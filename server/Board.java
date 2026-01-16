public class Board{
    private String[] board = new String[200];

    public String[] makeBoard() {
        for (int i = 0; i < 200; i++) {
            board[i] = ".";
        }
    
        return board;
    }

    public boolean collisionBottom(String[] board, int globalX, int globalY) { //tjekker om en position er inden for globalBoard
        
        
        
        return false;
    }

    public boolean isEmpty(int x, int y) { // tjekker om en plads er ledig eller låst
        return false;
    }


    public char[][] copyLocked() { //Kopiere board efter en kolision som kan bruges til næste activePiece
        return null;
    }

    public void clear() { //bruges til at nulstille board til restart game

    }
}
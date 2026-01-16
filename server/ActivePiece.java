public class ActivePiece {

    private int x;
    private int y;
    private int rotationIndex; // 0..3

    public void moveLeft()  { 
        x--; 
    }  // x--

    public void moveRight() { 
        x++; 
    }  // x++

    public void moveDown()  { 
        y++; 
    }  // y++

    public void rotationCW()  { 

    }
    public void rotationCCW() {

    }

    public int getX() { 
        return x; 
    }

    public int getY() { 
        return y; 
    }

    public int getRotationIndex() {
        return rotationIndex; 
    }


}

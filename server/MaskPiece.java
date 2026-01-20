import java.util.Random;

public class MaskPiece {

    public int randomNumber() {
        Random random = new Random();
        int tal = random.nextInt(7) + 1;
        return tal;
    }

    public int[][] chooseNextPiece() {
        int randomNumber = randomNumber();
        if (randomNumber == 1) {
            return longMask(0);
        }
        if (randomNumber == 2) {
            return tMask(0);
        }
        if (randomNumber == 3) {
            return zigzagL(0);
        }
        if (randomNumber == 4) {
            return zigzagR(0);
        }
        if (randomNumber == 5) {
            return lLeft(0);
        }
        if (randomNumber == 6) {
            return lRight(0);
        }
        if (randomNumber == 7) {
            return square(0);
        }
        return null;
    }


    public int[][] getMask(int pieceId, int rotationIndex) {
        if (pieceId == 1) {
            return longMask(rotationIndex);
        }
        if (pieceId == 2) {
            return tMask(rotationIndex);
        }
        if (pieceId == 3) {
            return zigzagL(rotationIndex);
        }
        if (pieceId == 4) {
            return zigzagR(rotationIndex);
        }
        if (pieceId == 5) {
            return lLeft(rotationIndex);
        }
        if (pieceId == 6) {
            return lRight(rotationIndex);
        }
        if (pieceId == 7) {
            return square(rotationIndex);
        }
        return null;
    }

    public int[][] longMask(int rotationIndex) {
        if (rotationIndex == 0) {
            return new int[][] { {0,0}, {1,0}, {2,0}, {3,0} }; // {x,y}
            // 0 1 2 3
            // X X X X  0
            // . . . .  1
            // . . . .  2
            // . . . .  3
        } 
        else if (rotationIndex == 1) {
            return new int[][] { {2,0}, {2,1}, {2,2}, {2,3} }; 
            // 0 1 2 3
            // . . X .  0
            // . . X .  1
            // . . X .  2
            // . . X .  3
        } 
        else if (rotationIndex == 2) {
            return new int[][] { {0,0}, {1,0}, {2,0}, {3,0} }; 
            // 0 1 2 3
            // . . . .  0
            // X X X X  1
            // . . . .  2
            // . . . .  3
        } 
        else if (rotationIndex == 3) {
            return new int[][] { {1,0}, {1,1}, {1,2}, {1,3} }; 
            // 0 1 2 3
            // . X . .  0
            // . X . .  1
            // . X . .  2
            // . X . .  3
        }

        return null;
    }

    public int[][] tMask(int rotationIndex) {
        if (rotationIndex == 0) {
            return new int[][] { {1,0}, {0,1}, {1,1}, {2,1} }; 
            // 0 1 2 3
            // . X . .  0
            // X X X .  1
            // . . . .  2
            // . . . .  3
        } 
        else if (rotationIndex == 1) {
            return new int[][] { {1,0}, {1,1}, {2,1}, {1,2} }; 
            // 0 1 2 3
            // . X . .  0
            // . X X .  1
            // . X . .  2
            // . . . .  3
        } 
        else if (rotationIndex == 2) {
            return new int[][] { {0,1}, {1,1}, {2,1}, {1,2} }; 
            // 0 1 2 3
            // . . . .  0
            // X X X .  1
            // . X . .  2
            // . . . .  3
        } 
        else if (rotationIndex == 3) {
            return new int[][] { {1,0}, {0,1}, {1,1}, {1,2} }; 
            // 0 1 2 3
            // . X . .  0
            // X X . .  1
            // . X . .  2
            // . . . .  3
        }

        return null;
    }

    public int[][] zigzagL(int rotationIndex) {
        if (rotationIndex == 0) {
            return new int[][] { {0,1}, {1,1}, {1,0}, {2,0} }; 
            // 0 1 2 3
            // . X X .  0
            // X X . .  1
            // . . . .  2
            // . . . .  3
        } 
        else if (rotationIndex == 1) {
            return new int[][] { {1,0}, {1,1}, {2,1}, {2,2} }; 
            // 0 1 2 3
            // . X . .  0
            // . X X .  1
            // . . X .  2
            // . . . .  3
        } 
        else if (rotationIndex == 2) {
            return new int[][] { {0,2}, {1,2}, {1,1}, {2,1} }; 
            // 0 1 2 3
            // . . . .  0
            // . X X .  1
            // X X . .  2
            // . . . .  3
        } 
        else if (rotationIndex == 3) {
            return new int[][] { {0,0}, {0,1}, {1,1}, {1,2} };
            // 0 1 2 3
            // X . . .  0
            // X X . .  1
            // . X . .  2
            // . . . .  3
        }

        return null;
    }

    public int[][] zigzagR(int rotationIndex) {
        if (rotationIndex == 0) {
            return new int[][] { {0,0}, {1,0}, {1,1}, {2,1} }; 
            // 0 1 2 3
            // X X . .  0
            // . X X .  1
            // . . . .  2
            // . . . .  3
        } 
        else if (rotationIndex == 1) {
            return new int[][] { {1,1}, {1,2}, {2,0}, {2,1} }; 
            // 0 1 2 3
            // . . X .  0
            // . X X .  1
            // . X . .  2
            // . . . .  3
        } 
        else if (rotationIndex == 2) {
            return new int[][] { {0,1}, {1,1}, {1,2}, {2,2} }; 
            // 0 1 2 3
            // . . . .  0
            // X X . .  1
            // . X X .  2
            // . . . .  3
        } 
        else if (rotationIndex == 3) {
            return new int[][] { {1,0}, {0,1}, {1,1}, {0,2} }; 
            // 0 1 2 3
            // . X . .  0
            // X X . .  1
            // X . . .  2
            // . . . .  3
        }

        return null;
    }

    public int[][] lLeft(int rotationIndex) {
        if (rotationIndex == 0) {
            return new int[][] { {0,0}, {0,1}, {1,1}, {2,1} }; 
            // 0 1 2 3
            // X . . .  0
            // X X X .  1
            // . . . .  2
            // . . . .  3
        } 
        else if (rotationIndex == 1) {
            return new int[][] { {1,0}, {1,1}, {1,2}, {2,0} }; 
            // 0 1 2 3
            // . X X .  0
            // . X . .  1
            // . X . .  2
            // . . . .  3
        } 
        else if (rotationIndex == 2) {
            return new int[][] { {0,1}, {1,1}, {2,1}, {2,2} };
            // 0 1 2 3
            // . . . .  0
            // X X X .  1
            // . . X .  2
            // . . . .  3
        } 
        else if (rotationIndex == 3) {
            return new int[][] { {1,0}, {1,1}, {1,2}, {0,2} }; 
            // 0 1 2 3
            // . X . .  0
            // . X . .  1
            // X X . .  2
            // . . . .  3
        }

        return null;
    }

    public int[][] lRight(int rotationIndex) {
        if (rotationIndex == 0) {
            return new int[][] { {2,0}, {0,1}, {1,1}, {2,1} }; 
            // 0 1 2 3
            // . . X .  0
            // X X X .  1
            // . . . .  2
            // . . . .  3
        } 
        else if (rotationIndex == 1) {
            return new int[][] { {1,0}, {1,1}, {1,2}, {2,2} }; 
            // 0 1 2 3
            // . X . .  0
            // . X . .  1
            // . X X .  2
            // . . . .  3
        } 
        else if (rotationIndex == 2) {
            return new int[][] { {0,1}, {1,1}, {2,1}, {0,2} };
            // 0 1 2 3
            // . . . .  0
            // X X X .  1
            // X . . .  2
            // . . . .  3
        } 
        else if (rotationIndex == 3) {
            return new int[][] { {0,0}, {1,0}, {1,1}, {1,2} };
            // 0 1 2 3
            // X X . .  0
            // . X . .  1
            // . X . .  2
            // . . . .  3
        }

        return null;
    }

    public int[][] square(int rotationIndex) {
        if (rotationIndex == 0 || rotationIndex == 1 || rotationIndex == 2 || rotationIndex == 3) {
            return new int[][] { {1,0}, {2,0}, {1,1}, {2,1} };
            // 0 1 2 3
            // . X X .  0
            // . X X .  1
            // . . . .  2
            // . . . .  3
        }

        return null;
    }

}


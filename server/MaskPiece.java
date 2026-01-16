public class MaskPiece {

    public int[][] longMask(int rotationIndex) {
        if (rotationIndex == 0) {
            return new int[][] { {0,0}, {1,0}, {2,0}, {3,0} };
            // 0 1 2 3
            // . . . . 0
            // [][][][]1
            // . . . . 2
            // . . . . 3
        } else if (rotationIndex == 1) {
            // . . [] .
            // . . [] .
            // . . [] .
            // . . [] .
        }

        return null;
    }

}
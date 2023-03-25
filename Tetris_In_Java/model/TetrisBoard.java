// TetrisBoard.java
package model;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;

/** Represents a Board class for Tetris.  
 * Based on the Tetris assignment in the Nifty Assignments Database, authored by Nick Parlante
 */
public class TetrisBoard implements Serializable{
    private int width; //board height and width
    private int height;
    protected boolean[][] tetrisGrid; //board grid
    boolean committed; //indicates if the board is in a 'committed' state, meaning can't undo!

    //In your implementation, you'll want to keep counts of filled grid positions in each column.
    //A completely filled column means the game is over!
    private int colCounts[];
    //You will also want to keep counts by row.
    //A completely filled row can be cleared from the board (and points are awarded)!
    private int rowCounts[];

    //In addition, you'll need to allocate some space to back up your grid data.
    //This will be important when you implement "undo".
    private boolean[][] backupGrid; //to back up your grid
    private int backupColCounts[]; //to back up your row counts
    private int backupRowCounts[]; //to back up your column counts

    //error types (to be returned by the place function)
    public static final int ADD_OK = 0;
    public static final int ADD_ROW_FILLED = 1;
    public static final int ADD_OUT_BOUNDS = 2;
    public static final int ADD_BAD = 3;

    /**
     * Constructor for an empty board of the given width and height measured in blocks.
     *
     * @param aWidth    width
     * @param aHeight    height
     */
    public TetrisBoard(int aWidth, int aHeight) {
        width = aWidth;
        height = aHeight;
        tetrisGrid = new boolean[width][height];

        colCounts = new int[width];
        rowCounts = new int[height];

        //init backup storage, for undo
        backupGrid = new boolean[width][height];
        backupColCounts = new int[width];
        backupRowCounts = new int[height];
    }

    /**
     * Helper to fill new game grid with empty values
     */
    public void newGame() {
        for (int x = 0; x < tetrisGrid.length; x++) {
            for (int y = 0; y < tetrisGrid[x].length; y++) {
                tetrisGrid[x][y] = false;
                }
            }
        Arrays.fill(colCounts, 0);
        Arrays.fill(rowCounts, 0);
        committed = true;
    }

    /**
     * Getter for board width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Getter for board height
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the max column height present in the board.
     * For an empty board this is 0.
     *
     * @return the y position of the last filled square in the tallest column
     */
    public int getMaxHeight() {
        return Arrays.stream(colCounts).max().getAsInt();
    }

    /**
     * Returns the height of the given column -- i.e. the y value of the highest block + 1.
     * The height is 0 if the column contains no blocks.
     *
     * @param x grid column, x
     *
     * @return the height of the given column, x
     */
    public int getColumnHeight(int x) {
        return colCounts[x];
    }

    /**
     * Returns the number of filled blocks in the given row.
     *
     * @param y grid row, y
     *
     * @return the number of filled blocks in row y
     */
    public int getRowWidth(int y) {
        return rowCounts[y];
    }

    /**
     * Returns true if the given block is filled in the board. Blocks outside of the
     * valid width/height area always return true (as we can't place anything there).
     *
     * @param x grid position, x
     * @param y grid position, y
     *
     * @return true if the given block at x,y is filled, else false
     */
    public boolean getGrid(int x, int y) {
        if (x >= width || x < 0 || y >= height || y < 0 || tetrisGrid[x][y])
            return true;
        return false;
    }

    /**
     * Given a piece and an x, returns the y value where the piece will come to rest
     * if it were dropped straight down at that x.q
     *
     * Use getLowestYVals and the col heights (getColumnHeight) to compute this quickly!
     *
     * @param piece piece to place
     * @param x column of grid
     *
     * @return the y value where the piece will come to rest
     */
    public int placementHeight(TetrisPiece piece, int x) {
        int max = 0;
        for (TetrisPoint p: piece.getBody()){
            if (getColumnHeight(x) - p.y > max){
                max = getColumnHeight(x) - p.y;
            }
        }
        return max;
    }

    /**
     * Attempts to add the body of a piece to the board. Copies the piece blocks into the board grid.
     * Returns ADD_OK for a regular placement, or ADD_ROW_FILLED
     * for a regular placement that causes at least one row to be filled. 
     * 
     * Error cases:
     * A placement may fail in two ways. First, if part of the piece may fall out
     * of bounds of the board, ADD_OUT_BOUNDS is returned.
     * Or the placement may collide with existing blocks in the grid
     * in which case ADD_BAD is returned.
     * In both error cases, the board may be left in an invalid
     * state. The client can use undo(), to recover the valid, pre-place state.
     * 
     * @param piece piece to place
     * @param x placement position, x
     * @param y placement position, y
     *
     * @return static int that defines result of placement
     */
    public int placePiece(TetrisPiece piece, int x, int y) {
        backupGrid();
        committed = false;
        for (TetrisPoint p : piece.getBody()){
            //check out of bounds
            try {
                if (getGrid(p.x + x, p.y + y)) {
                    //check colliding with existing blocks
                    if (tetrisGrid[p.x + x][p.y + y]){
                        return ADD_BAD;
                    } else if (x >= width || x < 0 || y >= height || y < 0 ) {
                        return ADD_OUT_BOUNDS;
                    }
                }
            }
            catch (IndexOutOfBoundsException error) {
                return ADD_OUT_BOUNDS;
            }

        }
        //place the pieces onto the board
        for (TetrisPoint p : piece.getBody()){
            tetrisGrid[p.x + x][p.y + y] = true;
        }
        makeHeightAndWidthArrays();

        //check row filled
        if (Arrays.stream(rowCounts).anyMatch(i -> i == getWidth())){
            return ADD_ROW_FILLED;
        }
        else {
            return ADD_OK;
        }
    }

    /**
     * Deletes rows that are filled all the way across, moving
     * things above down. Returns the number of rows cleared.
     * 
     * @return number of rows cleared (useful for scoring)
     */
    public int clearRows() {
        int counter = 0;
        int clearedHeight = 0;
        boolean check = true;
        for (int i = 0; i < getHeight(); i++) {
            if (rowCounts[i] == getWidth()) {
                if (check){
                    clearedHeight = i;
                    check = false;
                }
                for (int j = 0; j < getWidth(); j++){
                    tetrisGrid[j][i] = false;
                }
                counter += 1;
            }
        }
        if (counter > 0){
            clearRowsHelper(counter, clearedHeight);
            makeHeightAndWidthArrays();
        }
        return counter;
    }

    /**
     * Helper function for clear row, brings everything down x = counter rows
     */
    private void clearRowsHelper(int counter, int clearedHeight) {
        boolean emptyCheck = true;
        for (int currCol = clearedHeight; currCol < this.getHeight() - 1; currCol++) {
            for (int currRow = 0; currRow < this.getWidth() - 1; currRow++) {
                try {
                    tetrisGrid[currRow][currCol] = tetrisGrid[currRow][currCol + counter];
                } catch (ArrayIndexOutOfBoundsException error) {
                    tetrisGrid[currRow][currCol] = false;
                }
            }
        }
    }

    /**
     * Reverts the board to its state before up to one call to placePiece() and one to clearRows();
     * If the conditions for undo() are not met, such as calling undo() twice in a row, then the second undo() does nothing.
     * See the overview docs.
     */
    public void undo() {
        if (committed == true) return;  //a committed board cannot be undone!

        if (backupGrid == null) throw new RuntimeException("No source for backup!");  //a board with no backup source cannot be undone!

        //make a copy!!
        for (int i = 0; i < backupGrid.length; i++) {
            System.arraycopy(backupGrid[i], 0, tetrisGrid[i], 0, backupGrid[i].length);
        }

        //copy row and column tallies as well.
        System.arraycopy(backupRowCounts, 0, rowCounts, 0, backupRowCounts.length);
        System.arraycopy(backupColCounts, 0, colCounts, 0, backupColCounts.length);

        committed = true; //no going backwards now!
    }

    /**
     * Copy the backup grid into the grid that defines the board (to support undo)
     */
    private void backupGrid() {
        //make a copy!!
        for (int i = 0; i < tetrisGrid.length; i++) {
            System.arraycopy(tetrisGrid[i], 0, backupGrid[i], 0, tetrisGrid[i].length);
        }
        //copy row and column tallies as well.
        System.arraycopy(rowCounts, 0, backupRowCounts, 0, rowCounts.length);
        System.arraycopy(colCounts, 0, backupColCounts, 0, colCounts.length);
    }

    /**
     * Puts the board in the 'committed' state.
     */
    public void commit() {
        committed = true;
    }

    /**
     * Fills heightsOfCols[] and widthOfRows[].  Useful helper to support clearing rows and placing pieces.
     */
    private void makeHeightAndWidthArrays() {

        Arrays.fill(colCounts, 0);
        Arrays.fill(rowCounts, 0);

        for (int x = 0; x < tetrisGrid.length; x++) {
            for (int y = 0; y < tetrisGrid[x].length; y++) {
                if (tetrisGrid[x][y]) { //means is not an empty cell
                    colCounts[x] = y + 1; //these tallies can be useful when clearing rows or placing pieces
                    rowCounts[y]++;
                }
            }
        }
    }

    /**
     * Print the board
     * 
     * @return a string representation of the board (useful for debugging)
     */
    public String toString() {
        StringBuilder buff = new StringBuilder();
        for (int y = height-1; y>=0; y--) {
            buff.append('|');
            for (int x=0; x<width; x++) {
                if (getGrid(x,y)) buff.append('+');
                else buff.append(' ');
            }
            buff.append("|\n");
        }
        for (int x=0; x<width+2; x++) buff.append('-');
        return(buff.toString());
    }


}



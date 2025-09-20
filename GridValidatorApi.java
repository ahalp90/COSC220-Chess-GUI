package minigames.client.checkmates;

import java.util.*;

/**
 * Utility class for grid-based game operations.
 * Import to your project with <import minigames.client.checkmates.GridValidatorApi;>
 * Extracted from chess move validation patterns to help with common grid operations.
 * All methods are static so you can just call them directly without creating an object.
 *
 * CASTING NOTES:
 * - You DON'T need to cast when passing your arrays/objects INTO these methods
 *   (Java automatically upcasts your types to Object)
 * - You DO need to cast when getting results OUT of copyGrid() if you want your specific type back:
 *     Object[][] generic = copyGrid(yourTileBoard);  // No cast needed going in
 *     Tile piece = (Tile) generic[0][0];              // Cast needed coming out
 *
 * Note: Not set-up to work with jagged arrays.
 */
public class GridValidatorApi {

    /**
     * Private constructor to prevent instantiation of this utility class.
     * All methods are static and should be accessed directly via the class name.
     * (Also, it gets the class to 100% code coverage by overriding the implicit constructor)
     */
    private GridValidatorApi() {
        // This class is not meant to be instantiated.
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * All 8 directions around a square (includes diagonals).
     * Order: up-left, up, up-right, left, right, down-left, down, down-right
     */
    public static final int[][] EIGHT_DIRECTIONS = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1},           {0, 1},
            {1, -1},  {1, 0},  {1, 1}
    };

    /**
     * Just the 4 cardinal directions (no diagonals).
     * Order: up, right, down, left
     */
    public static final int[][] FOUR_DIRECTIONS = {
            {-1, 0},   // up
            {0, 1},    // right
            {1, 0},    // down
            {0, -1}    // left
    };

    /**
     * Checks if a position is within grid bounds.
     *
     * @param row the row to check
     * @param col the column to check
     * @param gridRows total rows in your grid
     * @param gridCols total columns in your grid
     * @return true if valid position, false if outside bounds
     *
     * Example: Before moving a piece, check if the destination is valid:
     *   if (isValidPosition(targetRow, targetCol, 8, 8)) {
     *       // Make the move
     *   }
     */
    public static boolean isValidPosition(int row, int col, int gridRows, int gridCols) {
        return row >= 0 && row < gridRows && col >= 0 && col < gridCols;
    }

    /**
     * Gets all positions in a straight line from a starting point.
     * Goes in one direction until it hits the edge of the board.
     * Useful for checking all squares in a direction regardless of what's there.
     *
     * @param row starting row
     * @param col starting column
     * @param direction which way to go [rowChange, colChange] like [-1, 0] for up
     * @param maxDistance how many squares to check
     * @param gridRows total rows in grid
     * @param gridCols total columns in grid
     * @return list of positions in that direction
     *
     * Example: Get all squares north of a position:
     *   int[] northDirection = {-1, 0};  // up one row, same column
     *   List<int[]> northSquares = getStraightLine(row, col, northDirection, 7, 8, 8);
     */
    public static List<int[]> getStraightLine(int row, int col, int[] direction,
                                              int maxDistance, int gridRows, int gridCols) {
        List<int[]> positions = new ArrayList<>();
        int rowChange = direction[0];
        int colChange = direction[1];

        for (int distance = 1; distance <= maxDistance; distance++) {
            int newRow = row + distance * rowChange;
            int newCol = col + distance * colChange;

            if (!isValidPosition(newRow, newCol, gridRows, gridCols)) {
                break; // Stop seeking if you've hit something that would make any further movement invalid.
            }

            positions.add(new int[]{newRow, newCol});
        }

        return positions;
    }

    /**
     * Gets positions in a straight line until blocked by something on the board.
     * Stops when it hits an occupied square (can include or exclude that square).
     * This is how chess pieces move - they stop when blocked.
     *
     * @param row starting row
     * @param col starting column
     * @param direction which way to go [rowChange, colChange]
     * @param maxDistance how many squares to check
     * @param board the actual game board to check for blocking pieces
     * @param stopCondition when to stop sliding (return true to stop at that square)
     * @param includeStopSquare whether to include the square where we stopped
     * @return list of positions in that direction
     * @throws IllegalArgumentException if board is null
     * Example: Get squares a rook can actually move to:
     *   ObjectCheck hasAnyPiece = new ObjectCheck() {
     *       @Override
     *       public boolean matches(Object obj) {
     *           return obj != null;  // Stop at any piece
     *       }
     *   };
     *   List<int[]> rookMoves = getStraightLineUntilBlocked(row, col, direction, 7, board, hasAnyPiece, true);
     *   // This includes the square with an enemy piece but stops there
     */
    public static List<int[]> getStraightLineUntilBlocked(int row, int col, int[] direction,
                                                          int maxDistance, Object[][] board,
                                                          ObjectCheck stopCondition, boolean includeStopSquare) {
        if (board == null || board.length == 0) {
            throw new IllegalArgumentException("Cannot check straight line on a null board.");
        }
        List<int[]> positions = new ArrayList<>();
        int rowChange = direction[0];
        int colChange = direction[1];

        for (int distance = 1; distance <= maxDistance; distance++) {
            int newRow = row + distance * rowChange;
            int newCol = col + distance * colChange;

            if (!isValidPosition(newRow, newCol, board.length, board[0].length)) {
                break; // Stop seeking if you've hit something that would make any further movement invalid.
            }

            Object atPosition = board[newRow][newCol];
            if (stopCondition.matches(atPosition)) {
                if (includeStopSquare) {
                    positions.add(new int[]{newRow, newCol});
                }
                break;  // Stop here
            }

            positions.add(new int[]{newRow, newCol});
        }

        return positions;
    }

    /**
     * Creates a copy of a 2D array so you can test moves without messing up the original.
     * Makes a new array structure but keeps references to the same objects inside.
     *
     * REQUIRES: A fully initialized 2D array (no null array, no null rows).
     *
     * NOTE: Returns Object[][] for compatibility with any game type. You may need to cast:
     *   YourType[][] testBoard = (YourType[][]) copyGrid(gameBoard);
     * Or just work with it as Object[][] and cast individual elements as needed.
     *
     * @param original the grid to copy (must be fully initialized)
     * @return new grid with same stuff in it (as Object[][])
     * @throws IllegalArgumentException if original is null or has null rows
     *
     * Example: Test if a move would be valid:
     *   Object[][] testBoard = copyGrid(gameBoard);
     *   testBoard[0][0] = null;  // Make a test move; original board unchanged
     */
    public static Object[][] copyGrid(Object[][] original) {
        if (original == null) {
            throw new IllegalArgumentException("Cannot copy null grid");
        }

        Object[][] copy = new Object[original.length][];
        for (int i = 0; i < original.length; i++) {
            if (original[i] == null) {
                throw new IllegalArgumentException("Grid has null row at index " + i);
            }
            copy[i] = new Object[original[i].length];
            System.arraycopy(original[i], 0, copy[i], 0, original[i].length);
        }
        return copy;
    }

    /**
     * Interface for checking conditions based on row/col position.
     * Use this when you want to check something about the position itself.
     *
     * How to use: Define it once, then reuse it everywhere:
     *
     *   // Define a check for edge squares
     *   PositionCheck isEdge = new PositionCheck() {
     *       @Override
     *       public boolean matches(int row, int col) {
     *           return row == 0 || row == 7 || col == 0 || col == 7;
     *       }
     *   };
     *
     *   // Now use it wherever you need
     *   List<int[]> edgeSquares = findAllMatching(8, 8, isEdge);
     */
    public interface PositionCheck {
        /**
         * Check if this position meets your condition.
         *
         * @param row the row to check
         * @param col the column to check
         * @return true if it matches what you're looking for
         */
        boolean matches(int row, int col);
    }

    /**
     * Interface for checking conditions based on what's stored at a position.
     * Use this when you want to check the actual object in your grid.
     *
     * How to use: Define it once, then reuse it everywhere:
     *
     *   // Define a check for empty squares
     *   ObjectCheck isEmpty = new ObjectCheck() {
     *       @Override
     *       public boolean matches(Object obj) {
     *           return obj == null;  // No piece here
     *       }
     *   };
     *
     *   // Now use it wherever you need
     *   List<int[]> emptySquares = findAllMatching(board, isEmpty);
     *
     *   // Or use it to check when sliding stops
     *   List<int[]> rookPath = getStraightLine(row, col, direction, 7, board, isEmpty, false);
     *   // This gets squares until hitting a piece (stops before the piece)
     */
    public interface ObjectCheck {
        /**
         * Check if this object meets your condition.
         *
         * @param obj the object to check (might be null)
         * @return true if it matches what you're looking for
         */
        boolean matches(Object obj);
    }

    /**
     * Finds all positions that meet your condition.
     * Goes through every square and checks if it matches what you want.
     *
     * @param gridRows number of rows
     * @param gridCols number of columns
     * @param check your condition to test each position
     * @return list of [row, col] positions that matched
     *
     * Example: Find all positions in the top row:
     *   PositionCheck topRow = new PositionCheck() {
     *       public boolean matches(int row, int col) {
     *           return row == 0;
     *       }
     *   };
     *   List<int[]> topPositions = findAllMatching(8, 8, topRow);
     */
    public static List<int[]> findAllMatching(int gridRows, int gridCols, PositionCheck check) {
        List<int[]> matches = new ArrayList<>();

        for (int row = 0; row < gridRows; row++) {
            for (int col = 0; col < gridCols; col++) {
                if (check.matches(row, col)) {
                    matches.add(new int[]{row, col});
                }
            }
        }

        return matches;
    }

    /**
     * Finds all positions where the object at that position meets your condition.
     * Like finding all white pieces or all empty squares.
     *
     * @param grid your 2D game board
     * @param check your condition to test each object
     * @return list of [row, col] positions with matching objects
     *
     * Example: Find all positions with white pieces:
     *   ObjectCheck isWhite = new ObjectCheck() {
     *       public boolean matches(Object obj) {
     *           if (obj == null) return false;
     *           ChessPiece piece = (ChessPiece) obj;
     *           return piece.getColour() == 'w';
     *       }
     *   };
     *   List<int[]> whitePieces = findAllMatching(board, isWhite);
     */
    public static List<int[]> findAllMatching(Object[][] grid, ObjectCheck check) {
        List<int[]> matches = new ArrayList<>();

        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[row].length; col++) {
                if (check.matches(grid[row][col])) {
                    matches.add(new int[]{row, col});
                }
            }
        }

        return matches;
    }

    /**
     * Gets the squares around a position.
     *
     * @param row center position row
     * @param col center position column
     * @param gridRows total rows in grid
     * @param gridCols total columns in grid
     * @param includeDiagonals true for all 8 squares, false for just 4 cardinal directions
     * @return list of adjacent [row, col] positions
     *
     * Example: Check all squares a king could move to:
     *   List<int[]> aroundKing = getAdjacentSquares(kingRow, kingCol, 8, 8, true);
     *   for (int[] square : aroundKing) {
     *       // Check if square is safe
     *   }
     */
    public static List<int[]> getAdjacentSquares(int row, int col, int gridRows, int gridCols,
                                                 boolean includeDiagonals) {
        List<int[]> adjacent = new ArrayList<>();
        int[][] directions = includeDiagonals ? EIGHT_DIRECTIONS : FOUR_DIRECTIONS;

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (isValidPosition(newRow, newCol, gridRows, gridCols)) {
                adjacent.add(new int[]{newRow, newCol});
            }
        }

        return adjacent;
    }
}
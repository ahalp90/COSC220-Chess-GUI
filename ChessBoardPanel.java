package minigames.client.checkmates;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChessBoardPanel extends JPanel implements Themeable {
    private final GameBoard gameBoard;
    
    private boolean enableMouseEvents = false;
    private PlayerColour clientPlayerColour;
    private boolean isWhiteAtBottom = true; // Default orientation with bottom left square A1.
    private int[] originSquare = null; // int{row, col}, translated from global px to board squares.
    private final List<int[]> legalMoveSquares = new ArrayList<>();

    private boolean showAvailableMoves = true; // Allow GUI to turn on/off available moves highlight.

    // Colours for squares. Not final, as they need to be modifiable by Themeable + GuiThemes.
    private Color lightSquareColour = new Color(255, 253, 224); // Washed out creamy white.
    private Color darkSquareColour = new Color(53, 52, 51); // Washed out black.
    private Color highlightColour = new Color(255, 100, 100, 191); // Washed-out bright red with transparency.
    private Color selectedColour = new Color (219,204,189,100);

    // Store default border to reversion after flashing red on illegal move from check.
    private final Border defaultBorder;

    // Chesspieces from https://commons.wikimedia.org/wiki/Category:PNG_chess_pieces/Standard_transparent
    private final ImageIcon wPawn = new ImageIcon(getClass().getResource("/checkmates-assets/w-pawn.png"));
    private final ImageIcon bPawn = new ImageIcon(getClass().getResource("/checkmates-assets/b-pawn.png"));
    private final ImageIcon wRook = new ImageIcon(getClass().getResource("/checkmates-assets/w-rook.png"));
    private final ImageIcon bRook = new ImageIcon(getClass().getResource("/checkmates-assets/b-rook.png"));
    private final ImageIcon wKnight = new ImageIcon(getClass().getResource("/checkmates-assets/w-knight.png"));
    private final ImageIcon bKnight = new ImageIcon(getClass().getResource("/checkmates-assets/b-knight.png"));
    private final ImageIcon wBishop = new ImageIcon(getClass().getResource("/checkmates-assets/w-bishop.png"));
    private final ImageIcon bBishop = new ImageIcon(getClass().getResource("/checkmates-assets/b-bishop.png"));
    private final ImageIcon wQueen = new ImageIcon(getClass().getResource("/checkmates-assets/w-queen.png"));
    private final ImageIcon bQueen = new ImageIcon(getClass().getResource("/checkmates-assets/b-queen.png"));
    private final ImageIcon wKing = new ImageIcon(getClass().getResource("/checkmates-assets/w-king.png"));
    private final ImageIcon bKing = new ImageIcon(getClass().getResource("/checkmates-assets/b-king.png"));

    /**
     * Initiate a ChessBoardPanel with a mouse handler/listeners local to its boundaries.
     * Listens for clicks and motion.
     * Mouse event handling is done by a helper method which is passed the MouseEventType and its coordinates.
     */
    public ChessBoardPanel(GameBoard gameBoard){
        this.gameBoard = gameBoard;
        // Store default border for reversion after red illegal check move-border.
        this.defaultBorder = this.getBorder();
 
        MouseAdapter mouseHandler = new MouseAdapter() {
            /**
             * Mouseclicks call the handleMouseEvents() method and pass it the mouse's Point repacked as an int[]{y,x}.
             * int[] format to align with the gameBoard's coordinate system and avoid introducing an extra datatype.
             * @param e the event to be processed
             */
            @Override
            public void mouseClicked(MouseEvent e) {
                // Hahn: Needed to control pregame board events.
                if (!enableMouseEvents) {
                    return;
                }
                handleMouseEvents(new int[]{e.getY(), e.getX()});
            }
        };
        this.addMouseListener(mouseHandler);

        // Sign up to the ColorScheme interface to receive colour updates for the board and highlights.
        GuiThemes.register(this);
    }


    /**
     * Provides a reasonable default size for the initial layout calculation.
     * <b>Important:</b> This is the anchor for the entire container's initial proportions.
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(400, 400);
    }

    /**
     * Paints the board. Sets rendering style with AA, and calls helpers to paint squares and relevant highlights.
     * squares, highlights and pieces.
     * @param g the <code>Graphics</code> object to protect
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); //boilerplate for paintComponent
        Graphics2D g2d = (Graphics2D) g;
        // Smooth out the images with AA.
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Derive square width/height from responsive board size; set here so it's updated here
        // before painting if window resized.
        double squareWidth = (double) getWidth() / 8;
        double squareHeight = (double) getHeight() / 8;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                drawSquare(g2d, row, col, squareWidth, squareHeight);
            }
        }

        drawSelectedSquareHighlight(g2d, squareWidth, squareHeight); // call after squares to not be overdrawn.
        drawPieces(g2d, squareWidth, squareHeight); // call after squares and highlights to not be overdrawn
        drawLegalMoveHighlights(g2d, squareWidth, squareHeight); //call after squares and pieces or it won't show.
    }


    /**
     *
     * @param g2d graphics renderer
     * @param row int of the current board row (0-7)
     * @param col int of the current board column (0-7)
     * @param squareWidth double primitive of the width of the square derived from the board height.
     * @param squareHeight double primitive of the height of hte square derived from the board height.
     */
    private void drawSquare(Graphics2D g2d, int row, int col, double squareWidth, double squareHeight) {
        Rectangle2D.Double squareRect = new Rectangle2D.Double(
                col * squareWidth,
                row * squareHeight,
                squareWidth,
                squareHeight
        );

        //All light square are an even index pos; you can now answer 1 LeetCode question!
        // https://algo.monster/liteproblems/1812 (well, after you adjust for us starting from 0 rather than 1).
        if ((row+col) % 2 == 0){
            g2d.setColor(lightSquareColour);
        } else {
            g2d.setColor(darkSquareColour);
        }
        g2d.fill(squareRect);
    }


    /**
     * Draws the pieces onto the board; calls gameBoard to check the piece positions and
     * translates these from array language to the board's visual orientation.
     * @param g2d graphics renderer
     * @param squareWidth double primitive of the width of the square derived from the board width
     * @param squareHeight double primitive of the height of the square derived from the board height.
     */
    private void drawPieces(Graphics2D g2d, double squareWidth, double squareHeight) {
        //Iteration numbers represent the visual row and need subsequent correlation to array state,
        //as the board might be flipped to place black at the bottom.
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                int boardArrayRow = isWhiteAtBottom ? row : 7 - row;
                int boardArrayCol = isWhiteAtBottom ? col : 7 - col;

                // Get pieces
                ChessPiece piece = this.gameBoard.getPieceAtPosition(new int[]{boardArrayRow, boardArrayCol});

                if (piece != null){
                    ImageIcon icon = getIconForPiece(""+piece.getColour()+piece.getType());
                    // drawImage requires casting to int. No need for the same precision here as for
                    // drawing the squares and catching mouseclicks within them, as a 0.5px offset
                    // won't be noticeable in this context.
                    g2d.drawImage(
                            icon.getImage(),
                            (int)(col * squareWidth),
                            (int)(row * squareHeight),
                            (int)squareWidth,
                            (int)squareHeight,
                            null

                    );
                }
            }
        }
    }

    /**
     * Highlights the square selected if it holds a piece.
     * @param g2d graphics renderer.
     * @param squareWidth double primitive of the width of the square derived from the board width
     * @param squareHeight double primitive of the height of the square derived from the board height.
     */
    private void drawSelectedSquareHighlight (Graphics2D g2d, double squareWidth, double squareHeight) {
        if (originSquare == null) return; // No need to drawHighlights if no piece selected.

        // Highlight the *selected square*.
        g2d.setColor(selectedColour);

        int[] guiRowCol = getGuiRowColFromBoardRowCol(originSquare);

        Rectangle2D.Double squareRect =  new Rectangle2D.Double(
                guiRowCol[1] * squareWidth,
                guiRowCol[0] * squareHeight,
                squareWidth,
                squareHeight
        );
        g2d.fill(squareRect);
    }

    /**
     * Highlights legal movement squares; overlays them with a small oval.
     * @param g2d graphics renderer
     * @param squareWidth double primitive of the width of the square derived from the board width
     * @param squareHeight double primitive of the height of the square derived from the board height.
     */
    private void drawLegalMoveHighlights (Graphics2D g2d, double squareWidth, double squareHeight) {
        if (!showAvailableMoves) return; // Don't highlight available moves if the user doesn't want, as instructed by GameGui.

        g2d.setColor(highlightColour);
        for (int[] move : legalMoveSquares) {
            int[] guiRowCol = getGuiRowColFromBoardRowCol(move);

            double ovalWidth = squareWidth / 4;
            double ovalHeight = squareHeight / 4;

            double ovalX = (guiRowCol[1] * squareWidth) + (squareWidth / 2) - (ovalWidth / 2);
            double ovalY = (guiRowCol[0] * squareHeight) + (squareHeight / 2) - (ovalHeight / 2);

            g2d.fillOval(
                    (int)ovalX,
                    (int)ovalY,
                    (int)ovalWidth,
                    (int)ovalHeight
            );
        }
    }

    /**
     * Flashes a red border around the panel for 750ms.
     * Intended for use in conjunction with GameBoard MoveOutcome INVALID_IN_CHECK;
     * the player tried to make a move from within check or that would put them in check.
     */
    public void redBorderOnIllegalMoveFromCheck() {
        this.setBorder(BorderFactory.createLineBorder(Color.RED, 20));
        this.repaint(); // Repaint to show red border
        // Revert border to default after timer finishes up.
        Timer borderResetTimer = new Timer(750, e -> {
            this.setBorder(defaultBorder);
            this.repaint(); // Repaint default invisible border after timer finishes
        });
        borderResetTimer.setRepeats(false);
        borderResetTimer.start();
    }

    /**
     * Orchestrates actions on the Panel in response to mouseClicked.
     * Clicking a piece's square will highlight that square and show the piece's available moves.
     * <p>Subsequently, clicking:
     * <li>(1) another piece of the same colour will transfer the focus to that piece
     * <li>(2) a valid movement location will trigger GameBoard to attempt the move,
     * <li>(3) an invalid movement location
     * <p>will remove the focus on the previously selected piece and any relevant square highlights.
     * <p>Note: package private to allow automated testing.</p>
     *
     * @param clickCoords an int[] of the {y,x} of the pixel location clicked.
     */
    void handleMouseEvents(int[] clickCoords){
        if (gameBoard.isGameOver()) return; // Don't allow interaction with pieces/squares if gameover.

        int[] clickedSquare = pixelToSquareConverter(clickCoords[0],  clickCoords[1]);

        //Handle error discovered by helper.
        if (clickedSquare == null) {
            System.err.println("Mouse events could not be handled as the board JPanel has no square size "
                    +"or the click was outside the panel.");
            return;
        }

        ChessPiece piece = gameBoard.getPieceAtPosition(clickedSquare);

        // Allow the preview for any piece on the board 
        if (originSquare == null) {
            if (piece != null) {
                // Will now show preview of any piece, not just pieces of correct 
                // turn colour.
                selectPieceAt(clickedSquare);
            }
            return;
        }

        if (originSquare != null) {
            if (listContainsArrayValues(legalMoveSquares, clickedSquare)) {
                ChessPiece selectedPiece = gameBoard.getPieceAtPosition(originSquare);

                // You can only make the move if its your turn and piece  
                if (selectedPiece != null
                        && selectedPiece.getColour() == gameBoard.getTurnColour()
                        && this.clientPlayerColour.toChar() == gameBoard.getTurnColour()) {

                    String fromPos = FormatTranslation.convertPosToString(originSquare);
                    String toPos = FormatTranslation.convertPosToString(clickedSquare);

                    gameBoard.makeMove(fromPos, toPos);
                        }

                deselectSquares(); 
                return;
            }

            // Only select pieces of the correct colour for that turn; null means no piece at square..
            if (piece != null && piece.getColour() == gameBoard.getTurnColour()) {
                selectPieceAt(clickedSquare);
            } else {
                //Attempt the move anyway so that the red border will flash on an illegal move from/into check.
                String fromPos = FormatTranslation.convertPosToString(originSquare);
                String toPos = FormatTranslation.convertPosToString(clickedSquare);
                gameBoard.makeMove(fromPos, toPos);
                //***Handles ALL invalid clicks by deselection***
                deselectSquares();
            }
            return;
        }
    }

    /**
     * Helper method to set the currently selected piece, set a moving piece's point
     * of departure, and calculate its legal moves.
     * @param clickedSquare int[] of the board coords {row, col} of the piece to select.
     */
    private void selectPieceAt(int[] clickedSquare) {
        // Set the piece's coord of departure in anticipation that the next click queues a move.
        originSquare = Arrays.copyOf(clickedSquare, clickedSquare.length);
        // Clear this.List of legal moves
        legalMoveSquares.clear();

        // Get the converted position
        String position = FormatTranslation.convertPosToString(clickedSquare);
        // Get the piece clicked 
        ChessPiece piece = gameBoard.getPieceAtPosition(clickedSquare);

        // Checking for null... always im paranoid
        if (piece != null) {
            // Hahn: This is the same as your old code ari, just inside the if 
            //       to check if it should go to movePiece because it is the turn 
            //       of the person clicking on the piece.
            if (piece.getColour() == gameBoard.getTurnColour()) {
                gameBoard.movePiece(position);
                legalMoveSquares.addAll(gameBoard.getAvailableMovesAtTurn());
            }
            // Otherwise the piece that was clicked is not the right colour to move 
            // therefore we need to check it in a separate function that is the 
            // same as "movePiece" but ignore turnColour as a piece that is the 
            // wrong turn colour has 0 legal moves and generate nothing on the 
            // chessboard.
            else {
                legalMoveSquares.addAll(gameBoard.getLegalMovesForPieceIgnoringTurnColour(position));
            }
        }
        repaint(); // Draw updated board state.
    }

    private void deselectSquares() {
        originSquare = null;
        legalMoveSquares.clear();

        repaint(); // Draw updated board state.
    }

    /**
     * Converts visual GUI grid coords into GameBoard array coordinates,
     * including flipping to account for white/black at bottom visually.
     * @param guiGridCoords int[] {row,col}
     * @return int[] {boardArrayRow, boardArrayCol}
     */
    private int[] getBoardRowColFromGuiRowCol(int[] guiGridCoords) {
        int guiRow = guiGridCoords[0];
        int guiCol = guiGridCoords[1];

        int boardArrayRow = isWhiteAtBottom ? guiRow : 7 - guiRow;
        int boardArrayCol = isWhiteAtBottom ? guiCol : 7 - guiCol;

        return new int[]{boardArrayRow, boardArrayCol};
    }

    /**
     * Converts GameBoard array coordinates into visual GUI grid coords,
     * including flipping to account for white/black at bottom visually.
     * @param boardCoords int[] {row,col}
     * @return int[]{guiRow, guiCol}
     * Identical method to getBoardRowColFromGuiRowCol, but its separate name reduces documentation and confusion.
     */
    private int[] getGuiRowColFromBoardRowCol(int[] boardCoords) {
        int boardRow = boardCoords[0];
        int boardCol = boardCoords[1];

        int guiRow = isWhiteAtBottom ? boardRow : 7 - boardRow;
        int guiCol = isWhiteAtBottom ? boardCol : 7 - boardCol;

        return new int[]{guiRow, guiCol};
    }


    /**
     * Converts a given (y,x) mouseclick pixel coordinate to a chessboard square coordinate.
     * <p>Calls a helper to ensure the board square accounts for whether whiteIsAtBottom or not.
     * @param y int of the mouse's y position, used to determine board row
     * @param x int of the mouse's x position, used to determine board column
     * @return int[] of the gui board's row and column
     */
    private int[] pixelToSquareConverter(int y, int x) {
        double currentSquareWidth = (double) getWidth() / 8;
        double currentSquareHeight = (double) getHeight() / 8;

        // Safety check in case it's used in a context where the panel hasn't yet been sized.
        if (currentSquareWidth <= 0 || currentSquareHeight <= 0) {
            return null;
        }

        //Convert pixel coordinates to visual GUI grid coordinates (0-7)
        int guiRow = (int) (y / currentSquareHeight);
        int guiCol = (int) (x / currentSquareWidth);

        //Ensure output coords are on the 8x8 board.
        if (guiCol < 0 || guiCol > 7 || guiRow < 0 || guiRow > 7) {
            return null;
        }
        //Call helper to do isWhiteAtBottom check and translate if necessary.
        return getBoardRowColFromGuiRowCol(new int[]{guiRow, guiCol});
    }

    /**
     * Helper method returns appropriate image for the piece.
     * Public as it's called by ChessBoardContainer.
     * @param piece String of the piece's colour+type (eg "wp" or "bp").
     * @return ImageIcon of chess pieces initialised in class field.
     */
    public ImageIcon getIconForPiece(String piece){
        if (piece == null) return null; // If the calling method doesn't prevent this, check here.
        // Could be replaced by a dictionary lookup, but that felt a bit slower for the same lines of code (even if it's neater).
        return switch (piece) {
            case "wp" -> wPawn;
            case "wr" -> wRook;
            case "wn" -> wKnight;
            case "wb" -> wBishop;
            case "wq" -> wQueen;
            case "wk" -> wKing;
            case "bp" -> bPawn;
            case "br" -> bRook;
            case "bn" -> bKnight;
            case "bb" -> bBishop;
            case "bq" -> bQueen;
            case "bk" -> bKing;
            default -> null;
        };
    }

    /**
     * Redraw the board so that the pieces are drawn correctly relative to the player's perspective.
     */
    public void flipBoard(){
        this.isWhiteAtBottom = !this.isWhiteAtBottom; //invert flag state
        // Avoid any weird bugs carrying over from flipping while selected.
        this.originSquare = null;
        this.legalMoveSquares.clear();
        this.repaint();
    }

    /**
     * Applies the colours from a new theme to the board's visual elements.
     * <p>Overrides default options; the game will still function without this.
     * <p>Called by GuiThemes manager whenever the theme is switched.
     * <p>Updates the colours for the board squares, as well as highlights for selected pieces and legal moves.
     * <p>Triggers repaint.
     * @param colors the ColorScheme provided by the new theme.
     */
    @Override
    public void applyTheme(GuiThemes.ColorScheme colors) {
        this.lightSquareColour = colors.boardLight();
        this.darkSquareColour = colors.boardDark();
        this.highlightColour = colors.moveHighlight();
        this.selectedColour = colors.selectionHighlight();
        repaint();
    }

    /**
     * Toggles whether available moves helper highlighting should be shown or not.
     */
    public void setShowAvailableMoves(){
        this.showAvailableMoves = !this.showAvailableMoves;
    }

    /**
     * Helper to check that a given List of int arrays contains the coordinates of a given int array.
     * Expected to be used with List of legal moves and int[] {y,x} piece coordinates
     * @param collection a List of int[]
     * @param coord an int[] of
     * @return boolean
     */
    private boolean listContainsArrayValues(List<int[]> collection, int[] coord){
        for  (int[] array : collection) {
            if (Arrays.equals(array, coord)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets and unsets who can edit the GUI (ie make moves) depending on whose turn it is, as relayed from server.
     * @param enable boolean true if mouse events should be enabled for this client.
     */
    public void setChessBoardMouseEvents(boolean enable) {
        this.enableMouseEvents = enable;
    }

    /**
     * Sets the colour of the current player.
     * <p>Called from CheckmatesClient to help control which pieces are movable by the current player.
     * @param colour PlayerColour Enum value.
     */
    public void setClientPlayerColour(PlayerColour colour) {
        this.clientPlayerColour = colour;
    }

}

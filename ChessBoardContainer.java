package minigames.client.checkmates;

import java.util.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Container that assembles all the core visual components of the chessboard UI.
 * <p>Key responsibilities include:
 * <li>Coordinating responsive resizing,
 * <li>Attaching the captured pieces panels,
 * <li>Attaching the board's axis labels (a-h, 1-8),
 * <li>Propagating 'flip' commands down to the ChessBoardPanel.
 * <p>Implements the Themeable interface to handle colour updates for its brackground and axis labels.
 */
public class ChessBoardContainer extends JPanel implements GameStateListener, Themeable {

    /**
     * INNER RECORD. Simple storage of gbc layout property values to minimise magic numbers.
     * @param gridx int of gbc argument
     * @param gridy int of gbc argument
     * @param gridwidth int of gbc argument
     * @param gridheight int of gbc argument
     * @param weightx double of gbc argument
     * @param weighty double of gbc argument
     */

    public record GbcLayoutProperties(
            int gridx,
            int gridy,
            int gridwidth,
            int gridheight,
            double weightx,
            double weighty
    ){}

    private final GameBoard gameBoard; // reference to gameBoard required for pieces captured display.
    private final ChessBoardPanel chessBoardPanel; //The actual chessboard.

    // Track board orientation here for labels and separately within ChessBoardPanel for the actual gameplay.
    private boolean isWhiteAtBottom = true;

    // UI LAYOUT OBJECTS
    //Map of GridBagLayout Properties for assignment to layout manager and any required read-outs to
    //other components. Immutable at assignment; doesn't permit nulls.
    private final Map<String, GbcLayoutProperties> gbcLayoutMap;
    // Holds the axis labels (a-h, 1-8)
    private final JPanel rowLabelPanel;
    private final JPanel colLabelPanel;
    // Panels to show the captured pieces above and below the board.
    private final CapturedPiecePanel topCapturedPanel;
    private final CapturedPiecePanel bottomCapturedPanel;

    // LAYOUT CONSTANT VALUES
    private int CAPTURED_PANEL_PREFERRED_HEIGHT; //Not final static because it's defined at construction.
    private static final double BOARD_WEIGHT = 1.0;          // The board gets the bulk of the space
    private static final double CAPTURED_PANEL_WEIGHT = 0.1; // Captured panels get a small, proportional slice


    /**
     * Constructor sets layout, initialises ChessBoardPanel gameBoard
     * (where the gui representation and interaction with gameplay actually occurs),
     * initialises custom captured piece Panels, and prepares JPanels for receipt of file/rank axis Strings.
     * @param gameBoard GameBoard--the chess engine's internal gamestate,
     *                  which holds a reference to a ChessPiece[][]
     */

    public ChessBoardContainer(GameBoard gameBoard) {
        this.gameBoard = gameBoard; //Backend chess processing core object
        this.gameBoard.addGameStateListener(this); //interface to receive instructions from GameBoard

        //Load the layout plans from the Map
        this.gbcLayoutMap = createInitialLayoutMap();
        //Set the layout for the following lines to apply to a GridBagLayout for y-weighted stacking.
        this.setLayout(new GridBagLayout());

        //object responsible for representing the actual chess and receiving user input to relay to GameBoard.
        this.chessBoardPanel = new ChessBoardPanel(this.gameBoard);
        // Get initial proportions for ChessBoardPanel and the top and bottom captured pieces panels.
        int initialBoardHeight = chessBoardPanel.getPreferredSize().height;
        // Dynamically derived constant to inform the captured panel heights.
        this.CAPTURED_PANEL_PREFERRED_HEIGHT = (int) (initialBoardHeight * (CAPTURED_PANEL_WEIGHT / BOARD_WEIGHT));

        // Instantiate captured piece panels using custom panel and icon painter object.
        this.topCapturedPanel = new CapturedPiecePanel(this.CAPTURED_PANEL_PREFERRED_HEIGHT, initialBoardHeight);
        this.bottomCapturedPanel = new CapturedPiecePanel(this.CAPTURED_PANEL_PREFERRED_HEIGHT, initialBoardHeight);

        //Create the row and column label panels that will host their 8 axis labels (a-h, 1-8).
        this.rowLabelPanel = new JPanel(new GridLayout(8, 1));
        this.colLabelPanel = new JPanel(new GridLayout(1, 8));
        rowLabelPanel.setOpaque(false);
        colLabelPanel.setOpaque(false);
        for (int i = 0; i < 8; i++) {
            rowLabelPanel.add(new JLabel(" ", SwingConstants.CENTER));
            colLabelPanel.add(new JLabel(" ", SwingConstants.CENTER));
        }


        // ASSEMBLE THE UI
        // Create an intermediate boardAndLabelsPanel using BorderLayout. This supports simple
        // responsive growth and proportion of the core chessBoardPanel and axis labels around it.
        JPanel boardAndLabelsPanel = new JPanel(new BorderLayout());
        boardAndLabelsPanel.setOpaque(false);
        boardAndLabelsPanel.add(this.chessBoardPanel, BorderLayout.CENTER);
        boardAndLabelsPanel.add(this.rowLabelPanel, BorderLayout.WEST);
        boardAndLabelsPanel.add(this.colLabelPanel, BorderLayout.SOUTH);

        // Give the board a strong minimum size to protect it from being squashed at initial population.
        boardAndLabelsPanel.setMinimumSize(new Dimension(200, 200));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH; //expand to fit max available dimensions

        //Specify this.add(object, constraints object) at every call; otherwise gbc acts silly and
        //only takes the final lot of constraints.
        applyProperties(gbc, gbcLayoutMap.get("topCapturedPanel"));
        this.add(topCapturedPanel, gbc);

        applyProperties(gbc, gbcLayoutMap.get("boardAndLabelsPanel"));
        this.add(boardAndLabelsPanel, gbc);

        applyProperties(gbc, gbcLayoutMap.get("bottomCapturedPanel"));
        this.add(bottomCapturedPanel, gbc);

        //This MUST be called at construction or else the axis labels will not be shown until the board is explicitly flipped.
        updateAxisLabels();

        GuiThemes.register(this); // Sign up to the Themeable GuiThemes listener
    }


    /**
     * Repaints the chessBoardPanel and updates the captured pieces labels on updates to GameBoard.
     * Interface method for GameStateListener
     * @param updatedGameBoard this object's known GameBoard
     */
    @Override
    public void onGameStateUpdate(GameBoard updatedGameBoard) {
        chessBoardPanel.repaint();
        updateCapturedPieces();
    }

    /**
     * Sets the text of the file and rank labels. Inverts order according to whether the
     * board is showing white or black on the bottom.
     */
    private void updateAxisLabels() {
        String[] cols = {"A", "B", "C", "D", "E", "F", "G", "H"}; //files
        String[] rows = {"1", "2", "3", "4", "5", "6", "7", "8"}; //ranks

        Component[] rowLabelComponents = rowLabelPanel.getComponents();
        Component[] colLabelComponents = colLabelPanel.getComponents();

        for (int i = 0; i < 8; i++) {
            //Update  row and colLabels
            JLabel rowLabel = (JLabel) rowLabelComponents[i];
            rowLabel.setText(isWhiteAtBottom ? rows[7 - i] : rows[i]);

            JLabel colLabel = (JLabel) colLabelComponents[i];
            colLabel.setText(isWhiteAtBottom ? cols[i] : cols[7 - i]);
        }
    }

    /**
     * Updates the ChessBoardContainer's Lists of captured pieces. Passes these to the CapturedPiecePanel objects,
     * and calls their setIcons to repaint and update the relevant top and bottom captured pieces panels.
     * Invokes later to maintain thread safety.
     */
    public void updateCapturedPieces() {
        // Invoke later because no dedicated Swing event handler attached to ensure thread safety.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Determine which main container is for which player
                CapturedPiecePanel panelCapturedByBlack = isWhiteAtBottom ? topCapturedPanel : bottomCapturedPanel;
                CapturedPiecePanel panelCapturedByWhite = isWhiteAtBottom ? bottomCapturedPanel : topCapturedPanel;

                //***Get the lists of captured pieces characters from GameBoard
                // List of WHITE piece characters (eg. 'P', 'Q') that have been captured by BLACK.
                List<Character> whiteColouredPiecesCaptured = gameBoard.getWhiteColouredPiecesCaptured();
                // List of BLACK piece characters (eg. 'p', 'q') that have been captured by WHITE.
                List<Character> blackColouredPiecesCaptured = gameBoard.getBlackColouredPiecesCaptured();

                //***Build the corresponding lists of ImageIcons
                // Create List of Icons for the panel that shows what the BLACK player has captured.
                List<ImageIcon> iconsForBlacksPanel = new ArrayList<>();
                for (char piece : whiteColouredPiecesCaptured) {
                    iconsForBlacksPanel.add(chessBoardPanel.getIconForPiece("w" + piece));
                }

                // Create List of Icons for the panel that shows what the WHITE player has captured.
                List<ImageIcon> iconsForWhitesPanel = new ArrayList<>();
                for (char piece : blackColouredPiecesCaptured) {
                    iconsForWhitesPanel.add(chessBoardPanel.getIconForPiece("b" + piece));
                }

                //***Pass the correct list of ICONS to the correct destination PANEL
                panelCapturedByBlack.setIcons(iconsForBlacksPanel);
                panelCapturedByWhite.setIcons(iconsForWhitesPanel);
                // NB. The repaint() is handled inside the receving object's setIcons method.
            }
        });

    }

    /**
     * Coordinates flipped board view (white/black player view at the bottom).
     * Flipping here directly affects the axis labels.
     * <p>But this also propagates to flip the board held in this object's field ChessBoardPanel,
     * affecting gameplay and representation of the full board therein.
     */
    public void flipBoard() {
        this.isWhiteAtBottom = !this.isWhiteAtBottom;
        updateAxisLabels();
        updateCapturedPieces();
        chessBoardPanel.flipBoard();
    }

    /**
     * <b>WARNING:</b> Do not remove this override. You will break the responsive layout.
     * <p>Overrides default setBounds to force a square aspect ratio for this container.
     * <p>This is critical because the parent layout provides rectangular bounds.
     * This explicit call gives a stable SQUARE boundary for the internal GridBagLayout to work properly.
     * @param x the new <i>x</i>-coordinate of this component
     * @param y the new <i>y</i>-coordinate of this component
     * @param width the new {@code width} of this component
     * @param height the new {@code height} of this
     *          component
     */
    @Override
    public void setBounds(int x, int y, int width, int height) {
        // Force square dimensions using the smaller of width/height
        int size = Math.min(width, height);

        int newX = x + (width - size) / 2;
        int newY = y + (height - size) / 2;
        super.setBounds(newX, newY, size, size);
    }

    /**
     * Creates and configures the layout for the main visual components of this layer of the gui.
     * <p>GridBagLayout handles the finicky proportional distribution, ensuring the minor components--
     * axis label panels and captured pieces panels--are correctly positioned and proportioned (weighted).
     * <p>Defines a simple stack of rows with varying weighty assignments.
     * The main board area is given the most weight, ensuring it scales correctly.
     *
     * @return An immutable Map of component names to their GridBagConstraints properties.
     */
    private static Map<String, GbcLayoutProperties> createInitialLayoutMap() {
        Map<String, GbcLayoutProperties> map = new HashMap<>();
        // Row 0: topCapturedPanel
        // Row 1: boardAndLabelsPanel (the main content)
        // Row 2: bottomCapturedPanel

        // All components are in gridx=0 and have weightx=1.0 to fill the full width.
        final int gridx = 0;
        final int gridwidth = 1;
        final double weightx = 1.0;

        // Top captured panel (Row 0): Gets a small, proportional slice of the vertical space.
        map.put("topCapturedPanel", new GbcLayoutProperties(
                gridx,
                0,
                gridwidth,
                1,
                weightx,
                CAPTURED_PANEL_WEIGHT
        ));

        // The main board area (Row 1): Gets the largest, primary slice of the vertical space.
        map.put("boardAndLabelsPanel", new GbcLayoutProperties(
                gridx,
                1,
                gridwidth,
                1,
                weightx,
                BOARD_WEIGHT
        ));

        // Bottom captured panel (Row 2): Gets another small, proportional slice.
        map.put("bottomCapturedPanel", new GbcLayoutProperties(
                gridx,
                2,
                gridwidth,
                1,
                weightx,
                CAPTURED_PANEL_WEIGHT
        ));

        return Map.copyOf(map);
    }

    /**
     * Private helper to apply the properties from a GbcLayoutProperties record to a GridBagCosntraints object.
     * Keeps the constructor clean, and supports maintainability and lookup of layout rules as defined in the Map.
     *
     * @param gbc the GridBagConstraints object to configure.
     * @param properties the GbcLayoutProperties record of the layout properties.
     */
    private void applyProperties(GridBagConstraints gbc, GbcLayoutProperties properties) {
        if (properties == null) {
            throw new NullPointerException("Could not find layout properties. Check for a typo in a map key.");
        }

        //Transfer the values from the immutable record to the GridBag constraints objects.
        gbc.gridx = properties.gridx();
        gbc.gridy = properties.gridy();
        gbc.gridwidth = properties.gridwidth();
        gbc.gridheight = properties.gridheight();
        gbc.weightx = properties.weightx();
        gbc.weighty = properties.weighty();
    }

    /**
     * Applies the colours from a new theme to the components managed by this container.
     * <p>Called by the GuiThemes manager whenever the theme is switched.
     * <p>Responsible for updating the container's own background colour,
     * and the foreground colour of the axis labels (a-h, 1-8).
     * Does not apply theming to the ChessBoardPanel itself.
     * @param colors the ColorScheme provided by the new theme.
     */
    @Override
    public void applyTheme(GuiThemes.ColorScheme colors) {
        this.setBackground(colors.background());

        // Iterate through the labels in the row and column panels to update their text colour.
        for (Component comp : rowLabelPanel.getComponents()) {
            if (comp instanceof JLabel) {
                comp.setForeground(colors.text());
            }
        }
        for (Component comp : colLabelPanel.getComponents()) {
            if (comp instanceof JLabel) {
                comp.setForeground(colors.text());
            }
        }

        //Redundant because setForeground does this, but left as an explicit reminder and in case of
        //code changes; cheap operation even if it's redundant.
        repaint();
    }

    //***       PUBLIC GETTERS      ***

    /**
     * Gets the ChessBoardPanel held by this container.
     * @return ChessBoardPanel
     */
    public ChessBoardPanel getChessBoardPanel() {return this.chessBoardPanel;}

    /**
     * Gets a read-only view of the whole GBC layout Map.
     * <p>May be useful for components that need to know all layout properties
     * @return unmodifiable Map view of all stored layout properties.
     */
    public Map<String, GbcLayoutProperties> getLayoutMap() {return Collections.unmodifiableMap(gbcLayoutMap);}

    /**
     * Gets the specific layout properties record for a specific named component.
     * <p>Public access for GbcLayoutProperties record layout data.
     * @param componentKey String equivalent of the key for the component (e.g. 'boardAndLabelsPanel').
     * @return relevant GbcLayoutProperties record, or null if none exists for the given key.
     */
    public GbcLayoutProperties getLayoutProperties(String componentKey) {return this.gbcLayoutMap.get(componentKey);}
}

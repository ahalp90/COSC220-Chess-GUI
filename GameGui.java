package minigames.client.checkmates;

import minigames.client.ratingSystem.RatingsGUI;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;

/**
 * Main container class responsible for chess GUI representation.
 * <p>Instantiates GUI components--ChessBoardContainer (directly) and ChessBoardPanel(indirectly--by ChessBoardContainer).
 * <p>Coordinates all non chess-related components of the UI for a chess game. Eg:
 * <li>Right side panel, including move history and chat</li>
 * <li>Menu bar</li>
 * <p>Implements several interfaces to coordinate with the rest of the system:
 * <li>GameStateListener reacts to updates from the game's backend</li>
 * <li>PromotionHandler manages the pawn promotion dialog popup for threading reasons</li>
 * <li><i>Note, GameGui is registered with PromotionHandler by CheckmatesClient</li></i>
 * <li>Themeable allows appearance to be dynamically changed.</li>
 */
public class GameGui implements GameStateListener, PromotionHandler, Themeable {
    private final GameBoard gameBoard;
    private final CheckmatesClient client;
    // Main frame to hold everything
    private final JPanel guiPanel;
    //Holds the gameboard interaction panel.
    private final ChessBoardContainer chessBoardContainer;

    // ScrollPanes that hold the move history and chat objects.
    private final JScrollPane moveHistoryScrollPane;
    private final JScrollPane chatScrollPane;

    // Dynamic text objects
    private final JTextArea chatText;
    private final JTextField chatInputField;
    private final JTextArea moveHistoryText;

    // Time display as updated at server sync
    private final JPanel timerPanel;
    private final JLabel whiteTimeLabel;
    private final JLabel blackTimeLabel;

    private boolean gameOverDialogueShown = false; // Gameover triggers a JDialog popup

    private final JButton[] WHITE_PAWN_PROMOTION_BUTTONS = createPawnPromotionButtons('w');
    private final JButton[] BLACK_PAWN_PROMOTION_BUTTONS = createPawnPromotionButtons('b');

    /**
     * Public constructor for the GameGui.
     * <p>Sets up all relevant Swing Components at this level and instantiates ChessBoardContainer.
     * <p>Chains to the 'real' (package-private) constructor to facilitate testing.
     * @param gameBoard GameBoard backend chess logic gamestate and processing
     * @param playerIdentity PlayerColour Enum value of the player (black or white)
     * @param client CheckmatesClient overall client manager
     */
    public GameGui(GameBoard gameBoard, PlayerColour playerIdentity, CheckmatesClient client) {
        this(gameBoard, playerIdentity, client, new ChessBoardContainer(gameBoard)); //instantiate ChessBoardContainer.
    }

    /**
     * The package-private constructor that does the real heavy lifting in construction.
     * This separation allows a mock ChessBoardContainer to be passed in, allowing isolated unit tests.
     * @param gameBoard GameBoard backend chess logic gamestate and processing
     * @param playerIdentity PlayerColour Enum value of the player (black or white)
     * @param client CheckmatesClient overall client manager
     * @param container the ChessBoardContainer passed in from the public constructor
     */
    GameGui(GameBoard gameBoard, PlayerColour playerIdentity, CheckmatesClient client,
            ChessBoardContainer chessBoardContainer) {

        this.client = client;
        this.chessBoardContainer = chessBoardContainer;
        this.gameBoard = gameBoard;

        // Initialise Swing Components
        this.guiPanel = new JPanel();

        //The right side panel objects
        this.moveHistoryScrollPane = new JScrollPane(); //initialise before assigning its values in createRightSidePanel()
        this.moveHistoryText = new JTextArea(25,12); // Preferred min size of several words across and a handful of lines down.
        this.moveHistoryText.setLineWrap(true);          
        this.moveHistoryText.setWrapStyleWord(true);
        this.chatScrollPane = new JScrollPane();
        this.chatText = new JTextArea(25,12);
        this.chatInputField = new JTextField();
        //timer objects
        this.whiteTimeLabel = new JLabel("White: 05:00", SwingConstants.CENTER);
        this.blackTimeLabel = new JLabel("Black: 05:00", SwingConstants.CENTER);
        this.timerPanel = new JPanel(); //Initialise this before assigning its values in createTimerPanel, as it's a final field.

        //Create main panels
        JMenuBar menuBar = createMenuBar();
        JPanel timerPanel = createTimerPanel(); //Configures the field object.
        JPanel rightSidePanel = createRightSidePanel(); // Configures the field object.

        guiPanel.setLayout(new BorderLayout(5, 5));

        // Set the board to the correct orientation for the player.
        if (playerIdentity == PlayerColour.BLACK) {
            chessBoardContainer.flipBoard();
        }

        // Add components to the Frame.
        guiPanel.add(menuBar, BorderLayout.NORTH);
        guiPanel.add(timerPanel, BorderLayout.SOUTH);
        guiPanel.add(rightSidePanel, BorderLayout.EAST);
        guiPanel.add(this.chessBoardContainer, BorderLayout.CENTER); //Main component stretches to fill available area.
        guiPanel.setVisible(true);

        this.gameBoard.addGameStateListener(this); // Sign up for listener that relays GameBoard backend updates.
        GuiThemes.register(this); // Sign up to the GuiThemes ColorScheme interface.
    }

    /**
     * The timer panel collects the white and black time labels, which are held in the class'
     * field and dynamically updated.
     *
     * @return JPanel holding the timers
     */
    private JPanel createTimerPanel(){
        // Box layout for easy responsive layout.
        timerPanel.setLayout(new BoxLayout(timerPanel, BoxLayout.X_AXIS));
        timerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createBevelBorder(
                        BevelBorder.RAISED,
                        new Color(240,240,240),
                        new Color (180,180,180)),
                BorderFactory.createEmptyBorder(2,5,2,5)
        ));

        // Make font bold and 120%. deriveFont avoids hardcoding font name or size.
        // Black and white labels are assumed to share identical font properties for now.
        Font prominentFont = this.whiteTimeLabel.getFont().deriveFont(
                Font.BOLD,
                this.whiteTimeLabel.getFont().getSize() *1.2f);

        //Apply the new beefed up font.
        this.whiteTimeLabel.setFont(prominentFont);
        this.blackTimeLabel.setFont(prominentFont);

        //Add components to full timerPanel
        timerPanel.add(Box.createRigidArea(new Dimension(10,0))); //Add a bit of left padding before white the label
        timerPanel.add(this.whiteTimeLabel);

        timerPanel.add(Box.createHorizontalGlue()); //responsive spacer between the two timers.

        timerPanel.add(this.blackTimeLabel);
        timerPanel.add(Box.createRigidArea(new Dimension(10,0))); // Add a bit of right padding after the black label.

        return timerPanel;
    }

    /**
     * Creates the right side panel which holds the move history display and chat area.
     * @return JPanel
     */
    private JPanel createRightSidePanel() {
        JPanel rightSidePanel = new JPanel();
        // GridBagLayout allows us to easily change the ratios and organisation down the track.
        rightSidePanel.setLayout(new GridBagLayout());
        rightSidePanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
       // rightSidePanel.setPreferredSize(new Dimension(200, 600)); // Should take up about 1/4 screen width.

        // Use GBC to proportion Panel's child components (move history and chat).
        GridBagConstraints gbc = new GridBagConstraints();

        // Constraints for Move History
        gbc.gridx = 0; gbc.gridy = 0; // Top component of container.
        gbc.weightx = 1.0; gbc.weighty = 0.4; // Fills horizontally, but only takes 40% vertically.
        gbc.fill = GridBagConstraints.BOTH;

        // Move History TextArea/ScrollPane
        this.moveHistoryText.setEditable(false);
        //Set scroll, contents and border.
        this.moveHistoryScrollPane.setViewportView(this.moveHistoryText);
        this.moveHistoryScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        this.moveHistoryScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        this.moveHistoryScrollPane.setBorder(BorderFactory.createTitledBorder("Move History"));

        rightSidePanel.add(moveHistoryScrollPane, gbc); // Add to Panel

        //Chat area sits below move history and takes 60% of Panel
        gbc.gridy = 1; gbc.weighty = 0.6;
        gbc.insets = new Insets(10,0,0,0); //10x margin between move history and chat.

        // chatPanel holds chatText history, input field and ScrollPane
        JPanel chatPanel = new JPanel(new BorderLayout()); // Responsively autoresize so chatText takes most of the Panel.
        // Title and border go on the surrounding ScrollPane
        // Set scroll and contents for chatText
        chatScrollPane.setViewportView(this.chatText);
        chatScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        // Add a small 5px space to separate the chatScrollPane from the chat input field, but do so
        // preserving any default border the object came with.
        chatScrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Chat"),
                BorderFactory.createEmptyBorder(0, 0, 5, 0)
        ));

        //chatText properties
        this.chatText.setEditable(false);
        this.chatText.setLineWrap(true);
        this.chatText.setWrapStyleWord(true);
        //Autoscroll chat text to most recent line on update.
        DefaultCaret caret = (DefaultCaret) chatText.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        // Add components to chatPanel
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);
        chatPanel.add(this.chatInputField, BorderLayout.SOUTH);

        rightSidePanel.add(chatPanel, gbc); //Add chat to rightSidePanel

         // Hahn: Testing to see if I can retrieve chat messages in the client
        chatInputField.addActionListener(e -> {
            client.sendNetworkCommand("chatMessage", chatInputField.getText() + "\n");
            chatInputField.setText("");
        });

        return rightSidePanel;
    }

    /**
     * Makes a menu bar that hosts the general interface interaction buttons.
     * Includes 'flip board', 'show available moves', 'show my pieces under attack', 'resign' and 'quit'.
     * @return JMenuBar
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Button for theme selection.
        menuBar.add(createThemeMenu());
        menuBar.add(Box.createRigidArea(new Dimension(5,0)));

        // Implements GameRatings team Game rating system
        JMenuItem rateGame = new JMenuItem("Rate our Game Here");
        rateGame.setBorder(BorderFactory.createRaisedBevelBorder());
        rateGame.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> new RatingsGUI("Checkmates"));
        });
        menuBar.add(rateGame);
        menuBar.add(Box.createRigidArea(new Dimension(5,0)));

        // Implements flipBoard button
        JMenuItem flipBoardItem = new JMenuItem("Flip Board");
        flipBoardItem.setBorder(BorderFactory.createRaisedBevelBorder());
        flipBoardItem.addActionListener(e -> this.chessBoardContainer.flipBoard());
        menuBar.add(flipBoardItem);
        menuBar.add(Box.createRigidArea(new Dimension(5,0)));

        JMenuItem showAvailMovesOnOffItem = new JMenuItem("Show available moves on/off");
        showAvailMovesOnOffItem.setBorder(BorderFactory.createRaisedBevelBorder());
        showAvailMovesOnOffItem.addActionListener(e -> {
            this.chessBoardContainer.getChessBoardPanel().setShowAvailableMoves();
            this.chessBoardContainer.getChessBoardPanel().repaint();
        });

        // Implements resign button
        JMenuItem resignItem = new JMenuItem("Resign");
        resignItem.setBorder(BorderFactory.createRaisedBevelBorder());
        menuBar.add(resignItem);
        menuBar.add(Box.createRigidArea(new Dimension(5,0)));
        // If the game is not over a player can resign if they want to.
        resignItem.addActionListener(e -> {
            if (!gameBoard.isGameOver()) {
                // System.out.println("GUI THE GAME IS OVER?: " + gameBoard.isGameOver());
                System.out.println(client.playerColour.toString() + " HAS RESIGNED");
                client.sendNetworkCommand("playerResigned", client.playerColour.toString());
            }
        });

        // Implements quit button
        JMenuItem quitToLobbyItem = new JMenuItem("Quit");
        quitToLobbyItem.setBorder(BorderFactory.createRaisedBevelBorder());
        menuBar.add(quitToLobbyItem);
        menuBar.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        quitToLobbyItem.addActionListener(e -> {
                client.closeGame();
        });

        return menuBar;
    }

    /**
     * Creates an array of JButtons for pawn promotion, one for each piece type.
     * <p>Static factory because the buttons themselves are generic and only depend
     * on the piece colour, not on any instance of the GameGui.
     * @param colour char primitive for the piece colour ('w' for white, 'b' for black)
     * @return JButton array with four JButtons, one each for the appropriately coloured Queen, Bishop, Rook and Knight.
     * @throws IllegalStateException if the chess piece icons couldn't be found; game can't sensibly work without these.
     */
    private static JButton[] createPawnPromotionButtons(char colour) throws IllegalStateException {
        String[] pieceNames = {"queen", "rook", "bishop", "knight"}; //use to populate filepath
        JButton[] buttons = new JButton[pieceNames.length];

        for (int i = 0; i < pieceNames.length; i++) {
            //removed leading slash to allow loading using threadsafe ClassLoader; necessitated for testing
            String threadSafeResourcePath = String.format("checkmates-assets/"+colour+"-"+pieceNames[i]+".png");
//            ImageIcon icon  = new ImageIcon(GameGui.class.getResource(path));
            // Load icons, or throw unchecked exception if not found--no point handling, as the game is unusable like this.
            java.net.URL imageUrl = Thread.currentThread().getContextClassLoader().getResource(threadSafeResourcePath);
            if (imageUrl == null) {
                throw new IllegalStateException("Missing chess piece icons at: " + threadSafeResourcePath);
            }
            ImageIcon icon = new ImageIcon(imageUrl);

            String pieceName = pieceNames[i].substring(0,1).toUpperCase() + pieceNames[i].substring(1);
            JButton button = new JButton(pieceName,icon);

            // Pop the name of the piece down below; could be useful for testing.
            button.setVerticalTextPosition(SwingConstants.BOTTOM);
            button.setHorizontalTextPosition(SwingConstants.CENTER);

            buttons[i] = button;
        }
        return buttons;
    }

    /**
     * Call method for PromotionHandler; supports calling showPromotionDialogue from GameBoard but keeping
     * it on the GUI's EDT.
     * @param turnColour 'w' or 'b' of the current turn.
     * @return showPawnPromotionDialogue(), which shows the pawn promotion dialogue and ultimately the char
     * of the player's promotion choice ('q', 'r', 'b', 'n')
     */
    @Override
    public char getPromotionChoice(char turnColour) {
        return showPawnPromotionDialogue();
    }

    /**
     * Present user with a modal dialog to choose a promotion piece.
     * Swing does NOT natively support option panes responding to custom button objects.
     * Custom pane and dialog with dedicated listeners implemented to correctly close the window and return a value.
     * @return char representing the user's pawn promotion choice ('q', 'r', 'b', 'n')
     */
    private char showPawnPromotionDialogue() {
        // Array to map the integer choices to a piece character.
        char[] pieceChars = {'q', 'r', 'b', 'n'};
        // Set promotion piece image options based on promoting player's colour.
        // Note, must be of type 'Object' to be accepted by JOptionPane.
        Object[] options = gameBoard.getTurnColour()=='w' ? WHITE_PAWN_PROMOTION_BUTTONS : BLACK_PAWN_PROMOTION_BUTTONS;


        // Manually create the JOptionPane. Provides layout management for the dialog.
        // Pane stores the value returned by the button-click within the dialog.
        final JOptionPane promotionPane = new JOptionPane(
                "Promote pawn to:",
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION,
                null,
                options,
                options[0]
        );

        // Manually create the JDialog window that will contain the JOptionPane.
        final JDialog dialog = new JDialog();
        dialog.setModal(true); // Blocks the GUI EDT until selection is finished.
        dialog.setTitle("Pawn Promotion");
        dialog.setContentPane(promotionPane);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // Add action listeners to the custom buttons. JOptionPane doesn't do this for custom components.
        for (int i = 0; i < options.length; i++) {
            // Cast options back to JButton. Convoluted step required by JOptionPane requiring Objects passed in.
            JButton button = (JButton) options[i];
            button.addActionListener(e -> {
                promotionPane.setValue(button);
                // Manually closes the dialog on button-click.
                dialog.setVisible(false);
            });
        }

        dialog.pack(); // Autosize to fit dialog's components.
        dialog.setLocationRelativeTo(guiPanel); // Center in GUI panel.
        dialog.setVisible(true); // Blocks execution at this line until a value is returned.

        Object selectedValue = promotionPane.getValue(); // Retrieve value stored in pane by dialog
        dialog.dispose(); // Safely releases the screen resources used by the custom dialog.

        // Get the index value of the button clicked; default choice is triggered by closing the
        // pane without selection.
        int choice = JOptionPane.CLOSED_OPTION;
        if (selectedValue instanceof JButton) {
            for (int i = 0; i<options.length; i++) {
                if (options[i].equals(selectedValue)) {
                    choice = i;
                    break;
                }
            }
        }

        // Default to Queen if the dialog is closed
        // Otherwise, translate the label choice to its equivalent array position in pieceChars;
        // note, the array characters must be in appropriate order relative to the JLabels
        // instantiated in the field.
        // Pass out a char equivalent for coherence with existing game logic.
        return (choice == JOptionPane.CLOSED_OPTION) ? 'q' : pieceChars[choice];
    }

    /**
     * Responds to updates from the GameBoard state
     * <p>Called by GameBoard whenever the player is INVALID_IN_CHECK or it's gameover.
     * @param gameBoard the GameBoard.
     */
    @Override
    public void onGameStateUpdate(GameBoard gameBoard) {
        SwingUtilities.invokeLater(() -> {
                    // THIS IS HELPFUL DEBUG CODE; leaving commented out deliberately.
                    // System.out.println("LISTENER RUNNING. Current MoveOutcome is: " +gameBoard.getMoveOutcome());
                    if (gameBoard.getMoveOutcome() == MoveOutcome.INVALID_IN_CHECK) {
                        // redBorder method contains own repaint calls due to timer-oriented logic.
                        chessBoardContainer.getChessBoardPanel().redBorderOnIllegalMoveFromCheck();
                    }
                    if (gameBoard.isGameOver() && !gameOverDialogueShown) {
                        this.gameOverDialogueShown = true;
                    }
                }
        );
    }

    /**
     * Appends a chat message from the opponent into the chat text area.
     * The caret is moved to the end so the latest message is visible.
     *
     * @param message The opponent's chat message.
     */
    public void appendChatMessage(String message){
        chatText.append(message);
        chatText.setCaretPosition(chatText.getDocument().getLength());
    }

    /**
     * Appends a move to the move history text area.
     * In singleplayer mode, moves are relabeled to show they came from the AI.
     * The caret is moved to the end so the latest move is visible.
     *
     * @param move The move in standard notation, e.g. "Player: e2e4".
     */
    public void appendMoveHistory(String move){
        if (client.getSingleplayer()) {
            if (gameBoard.getMoveHistory().size() % 2 == 0) {
                move = "PHD AI: " + move.split(":")[1]; //AI moves have the AI name prepended.
            }
        }
            
        moveHistoryText.append(move);
        moveHistoryText.setCaretPosition(moveHistoryText.getDocument().getLength());
    }

    /**
     * Updates both players' clocks to match the latest server state.
     *
     * @param clocks A comma-separated string with white and black times.
     *               Format: "whiteTime,blackTime".
     */
     public void updateClocks(String clocks) {
        String[] bothClocks = clocks.split(",");
        setWhiteTimeLabel(bothClocks[0]);
        setBlackTimeLabel(bothClocks[1]);
    }

    /**
     * Called when a piece is taken on the chessboard, updates the takenpieces lists in the gameboard object.
     * <p>This method is called by CheckmatesClient when it receives an updateTakenPieces command</p>
     * @param piece String representing the piece, uppercase for white and lowercase for black (e.g. "Q").
     */
    public void updateTakenPieces(String piece) {
        if (client.playerColour.toChar() == 'w') {
            gameBoard.setWhiteColouredPiecesCaptured(piece.charAt(0));
        }
        else {
            System.out.println("PIECE UPDATED FOR BLACK " + piece);
            gameBoard.setBlackColouredPiecesCaptured(piece.charAt(0));
        }
        getChessBoardContainer().updateCapturedPieces();
    }

    /**
     * Applies the colours from a new theme to all relevant UI components in this panel.
     * Overrides any default themes for relevant objects.
     * <p> Called by the GuiThemes manager whenever the theme is switched.
     * <p>Updates the background, text and border colours of the timer panel, chat and move history areas.
     * @param colors the ColorScheme provided by the new theme.
     */
    @Override
    public void applyTheme(GuiThemes.ColorScheme colors) {
        // Main panel
        guiPanel.setBackground(colors.background());

        // Timer panel and labels
        timerPanel.setBackground(colors.surface());
        whiteTimeLabel.setForeground(colors.text());
        blackTimeLabel.setForeground(colors.text());

        // Update the titled borders for Move History and Chat
        moveHistoryScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createMatteBorder(
                        2,2,2,2,
                        colors.border()),
                "Move History")
        );

        chatScrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createMatteBorder(
                                2,2,2,2,
                                colors.border()),
                        "Chat"),
                BorderFactory.createEmptyBorder(0, 0, 5, 0)  // Keep the original padding
        ));

        // Text areas
        moveHistoryText.setBackground(colors.surface());
        moveHistoryText.setForeground(colors.textMuted()); // Slightly different text colour from chat
        chatText.setBackground(colors.surface());
        chatText.setForeground(colors.text());
        chatInputField.setBackground(colors.surface());
        chatInputField.setForeground(colors.text());
        chatInputField.setCaretColor(colors.text());

        guiPanel.repaint();
    }

    /**
     * Creates the 'Theme' dropdown menu for the main menu bar.
     * <p> Dynamically populates the menu with all available themes defined in the GuiThemes Enum.
     * <p>Only allows selecting one theme at a time.
     * @return JMenu containing the theme selection radio buttons.
     */
    private JMenu createThemeMenu() {
        JMenu themeMenu = new JMenu("Theme");
        // ButtonGroup sets all buttons other than the one selected to 'off'; only one theme can be selected at a time.
        ButtonGroup themeGroup = new ButtonGroup();

        for (GuiThemes theme : GuiThemes.values()) {
            JRadioButtonMenuItem themeItem = new JRadioButtonMenuItem(theme.getDisplayName());
            themeItem.setSelected(theme == GuiThemes.getCurrentTheme());
            themeGroup.add(themeItem);

            themeItem.addActionListener(e -> GuiThemes.switchTheme(theme));

            themeMenu.add(themeItem);
        }
        return themeMenu;
    }

    /**
     * Builds and returns the main game panel containing the chessboard.
     *
     * @return A JPanel containing the board and UI elements.
     */
    public JPanel buildGamePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(guiPanel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Gets a direct reference to the chat history text area for ferry messages from server to GUI.
     * @return the chat JTextArea.
     */
    public JTextArea getChatTextArea() {
        return chatText;
    }

    /**
     * Gets a direct reference to the move history text area to update its content based on GameBoard updates.
     * @return the moveHistory JTextArea
     */
    public JTextArea getMoveHistoryArea() {
        return moveHistoryText;
    }

    /**
     * Gets a direct reference to the interactive chessboard panel nested in the ChessBoardContainer
     * for the client to query.
     * @return the ChessBoardPanel
     */
    public ChessBoardPanel getChessBoardPanel() {
        return chessBoardContainer.getChessBoardPanel();
    }

    /**
     * Gets a direct reference to the interactive ChessBoardContainer for the client to query
     * @return ChessBoardContainer
     */
    public ChessBoardContainer getChessBoardContainer() {
        return this.chessBoardContainer;
    }

    /**
     * Updates the displayed time for the white player.
     *
     * @param time The remaining time in string format (e.g. "05:23").
     */
    public void setWhiteTimeLabel(String time) {
        whiteTimeLabel.setText("White: " + time);
    }

    /**
     * Updates the displayed time for the black player.
     *
     * @param time The remaining time in string format (e.g. "07:15").
     */
    public void setBlackTimeLabel(String time) {
        blackTimeLabel.setText("Black: " + time);
    }

    /**
     * Gets the main panel that holds all Gui components. Used for testing to inspect the components.
     * Package private to reduce exposure--only internal access needed.
     * @return the main JPanel for the game.
     */
    JPanel getGuiPanel() {return guiPanel;}
}


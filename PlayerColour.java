package minigames.client.checkmates;

/**
 * Player colours to support multiplayer initialisation.
 */
public enum PlayerColour {
    WHITE, BLACK;

    /**
     * Provides a char of PlayerColour for comparison with GameBoard turnColour.
     * @return char of the value: 'w' for WHITE, 'b' for BLACK
     */
    public char toChar() {return this == WHITE ? 'w' : 'b';}
}


package minigames.client.checkmates;

/**
 * Listener interface to coordinate GUI updates based on board state changes.
 * Keeps game engine classes, particularly GUI classes, appropriately separated.
 * Necessary for events like updating pieces taken label in GUI.
 */
public interface GameStateListener {
    public void onGameStateUpdate(GameBoard gameBoard);
}
